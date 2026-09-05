package com.pet.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.common.api.ResultCode;
import com.pet.common.enums.EvidenceType;
import com.pet.common.enums.OrderStatus;
import com.pet.common.exception.BusinessException;
import com.pet.common.util.GeoUtil;
import com.pet.config.GeoProperties;
import com.pet.dto.CheckInDTO;
import com.pet.dto.EvidenceSaveDTO;
import com.pet.dto.TrackPointDTO;
import com.pet.dto.TrackSaveDTO;
import com.pet.entity.Order;
import com.pet.entity.OrderEvidence;
import com.pet.mapper.OrderEvidenceMapper;
import com.pet.mapper.OrderMapper;
import com.pet.security.UserContext;
import com.pet.service.FulfillmentService;
import com.pet.service.ServiceCategoryService;
import com.pet.vo.OrderEvidenceVO;
import com.pet.vo.TrackPointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FulfillmentServiceImpl extends ServiceImpl<OrderEvidenceMapper, OrderEvidence>
        implements FulfillmentService {

    private final OrderMapper orderMapper;
    private final ServiceCategoryService serviceCategoryService;
    private final GeoProperties geoProperties;
    /** 复用 Spring 配好的 ObjectMapper：track_json 里的时间格式才会和接口出参一致 */
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkIn(Long orderId, CheckInDTO dto) {
        Order order = requireMyOrder(orderId);
        // GeoUtil 是纬度在前，与 Redis GEO / 高德 Marker 的经度在前相反，写反不会报错只会永远超限
        double distance = GeoUtil.distanceMeters(
                dto.getLat().doubleValue(), dto.getLng().doubleValue(),
                order.getAddressLat().doubleValue(), order.getAddressLng().doubleValue());
        int radius = geoProperties.getCheckInRadius();
        if (distance > radius) {
            throw new BusinessException(ResultCode.GEO_CHECK_IN_FAILED.getCode(),
                    String.format("定位打卡失败：当前距服务地址约 %.0f 米，允许范围 %d 米", distance, radius));
        }

        // 条件更新（status = 2 → 3）是幂等防线：连点两次打卡，第二次影响行数为 0。
        // 存证与状态推进必须同生共死，所以整个方法在事务里。
        if (orderMapper.markCheckedIn(orderId) == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
        }

        OrderEvidence evidence = new OrderEvidence();
        evidence.setOrderId(orderId);
        evidence.setSitterId(order.getSitterId());
        evidence.setType(EvidenceType.CHECK_IN.getCode());
        evidence.setLat(dto.getLat());
        evidence.setLng(dto.getLng());
        evidence.setRemark(String.format("距服务地址约 %.0f 米", distance));
        save(evidence);
    }

    @Override
    public OrderEvidenceVO saveChecklistEvidence(Long orderId, EvidenceSaveDTO dto) {
        Order order = requireMyOrder(orderId);
        requireInService(order);
        List<String> checklist = serviceCategoryService.getDetail(order.getCategoryId()).getChecklist();
        // 清单项由服务端比对：让接单员自由填一个「已完成」，验收时用户看到的清单就没有意义了
        if (!checklist.contains(dto.getCheckItem())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "「" + dto.getCheckItem() + "」不在该服务的作业清单里");
        }

        // 直接走 baseMapper.selectOne 而不是 ServiceImpl#getOne，理由同 WalletServiceImpl#getMine：
        // getOne 层层转发后落到哪个 selectOne 重载由框架版本决定，单测里没有确定的拦截点
        OrderEvidence existing = baseMapper.selectOne(Wrappers.<OrderEvidence>lambdaQuery()
                .eq(OrderEvidence::getOrderId, orderId)
                .eq(OrderEvidence::getType, EvidenceType.CHECKLIST.getCode())
                .eq(OrderEvidence::getCheckItem, dto.getCheckItem())
                .last("LIMIT 1"));
        if (existing == null) {
            OrderEvidence evidence = new OrderEvidence();
            evidence.setOrderId(orderId);
            evidence.setSitterId(order.getSitterId());
            evidence.setType(EvidenceType.CHECKLIST.getCode());
            evidence.setCheckItem(dto.getCheckItem());
            evidence.setImageUrl(dto.getImageUrl());
            evidence.setLat(dto.getLat());
            evidence.setLng(dto.getLng());
            evidence.setRemark(dto.getRemark());
            save(evidence);
            return toVO(evidence);
        }

        // 重拍同一项就覆盖。必须显式 set 每一列：默认字段策略会跳过 null，
        // 这次不带定位就会把上一次的坐标留在库里；wrapper 更新也不走 MetaObjectHandler，
        // update_time 得自己给。
        lambdaUpdate()
                .eq(OrderEvidence::getId, existing.getId())
                .set(OrderEvidence::getImageUrl, dto.getImageUrl())
                .set(OrderEvidence::getLat, dto.getLat())
                .set(OrderEvidence::getLng, dto.getLng())
                .set(OrderEvidence::getRemark, dto.getRemark())
                .set(OrderEvidence::getUpdateTime, LocalDateTime.now())
                .update();
        return toVO(getById(existing.getId()));
    }

    @Override
    public OrderEvidenceVO saveTrack(Long orderId, TrackSaveDTO dto) {
        Order order = requireMyOrder(orderId);
        requireInService(order);

        OrderEvidence evidence = new OrderEvidence();
        evidence.setOrderId(orderId);
        evidence.setSitterId(order.getSitterId());
        evidence.setType(EvidenceType.TRACK.getCode());
        evidence.setTrackJson(writeTrack(dto.getPoints()));
        evidence.setRemark(dto.getRemark());
        save(evidence);
        return toVO(evidence);
    }

    @Override
    public void finish(Long orderId) {
        Order order = requireMyOrder(orderId);
        requireInService(order);
        List<String> missing = missingItems(order);
        if (!missing.isEmpty()) {
            // 只说「请先提交存证」接单员不知道该补哪几项，把缺的项直接列出来
            throw new BusinessException(ResultCode.EVIDENCE_REQUIRED.getCode(),
                    "还有 " + missing.size() + " 项未拍照存证：" + String.join("、", missing));
        }
        if (orderMapper.markFinished(orderId) == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
        }
    }

    @Override
    public List<OrderEvidenceVO> listEvidence(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        Long viewerId = UserContext.userId();
        // 与 OrderServiceImpl#getDetail 同一套可见性：当事人双方 + 管理员。
        // 三行判断，不值得为它把 FulfillmentService 挂到 OrderService 上
        boolean visible = order.getUserId().equals(viewerId)
                || viewerId.equals(order.getSitterId())
                || UserContext.isAdmin();
        if (!visible) {
            throw new BusinessException(ResultCode.ORDER_ACCESS_DENIED);
        }
        return list(Wrappers.<OrderEvidence>lambdaQuery()
                .eq(OrderEvidence::getOrderId, orderId)
                .orderByAsc(OrderEvidence::getId))
                .stream().map(this::toVO).toList();
    }

    /** 只有该单的接单员能推进履约；sitterId 为 null（还没人抢）时 equals 自然是 false，同样回 2005。 */
    private Order requireMyOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!UserContext.userId().equals(order.getSitterId())) {
            throw new BusinessException(ResultCode.ORDER_ACCESS_DENIED);
        }
        return order;
    }

    private void requireInService(Order order) {
        if (order.getStatus() == null || order.getStatus() != OrderStatus.IN_SERVICE.getCode()) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
        }
    }

    /** 清单里还没有存证照片的项，按模板顺序返回。 */
    private List<String> missingItems(Order order) {
        Set<String> done = list(Wrappers.<OrderEvidence>lambdaQuery()
                .eq(OrderEvidence::getOrderId, order.getId())
                .eq(OrderEvidence::getType, EvidenceType.CHECKLIST.getCode()))
                .stream().map(OrderEvidence::getCheckItem).filter(Objects::nonNull).collect(Collectors.toSet());
        return serviceCategoryService.getDetail(order.getCategoryId()).getChecklist().stream()
                .filter(item -> !done.contains(item))
                .toList();
    }

    private String writeTrack(List<TrackPointDTO> points) {
        LocalDateTime now = LocalDateTime.now();
        List<TrackPointVO> normalized = points.stream().map(p -> {
            TrackPointVO vo = new TrackPointVO();
            vo.setLat(p.getLat());
            vo.setLng(p.getLng());
            // 浏览器 watchPosition 有些实现不给时间戳，缺了就补服务端当前时间，
            // 否则前端画轨迹时这个点是 null，整条线断掉
            vo.setTime(p.getTime() == null ? now : p.getTime());
            return vo;
        }).toList();
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException e) {
            // 只有三个基础字段，正常不会失败；真失败了宁可整次上传报错，
            // 也不要往 track_json 里写半截 JSON 让后续读取一直炸
            log.warn("序列化轨迹失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "轨迹数据格式不正确");
        }
    }

    private List<TrackPointVO> readTrack(OrderEvidence evidence) {
        try {
            return objectMapper.readValue(evidence.getTrackJson(), new TypeReference<List<TrackPointVO>>() {
            });
        } catch (JsonProcessingException e) {
            // track_json 是库里的自由文本列，读到脏数据时只丢这一条轨迹，
            // 不能让整个存证列表 500——用户还得靠剩下的照片验收
            log.warn("存证 {} 的轨迹 JSON 解析失败，已跳过: {}", evidence.getId(), e.getMessage());
            return List.of();
        }
    }

    private OrderEvidenceVO toVO(OrderEvidence e) {
        OrderEvidenceVO vo = new OrderEvidenceVO();
        vo.setId(e.getId());
        vo.setOrderId(e.getOrderId());
        vo.setType(e.getType());
        vo.setTypeText(EvidenceType.descOf(e.getType()));
        vo.setCheckItem(e.getCheckItem());
        vo.setImageUrl(e.getImageUrl());
        vo.setLat(e.getLat());
        vo.setLng(e.getLng());
        vo.setRemark(e.getRemark());
        vo.setCreateTime(e.getCreateTime());
        if (StrUtil.isNotBlank(e.getTrackJson())) {
            vo.setTrackPoints(readTrack(e));
        }
        return vo;
    }
}
