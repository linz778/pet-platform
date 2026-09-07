package com.pet.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.api.PageResult;
import com.pet.common.api.ResultCode;
import com.pet.common.enums.ArbitrationStatus;
import com.pet.common.enums.OrderStatus;
import com.pet.common.enums.PayStatus;
import com.pet.common.exception.BusinessException;
import com.pet.common.util.CommaListUtil;
import com.pet.dto.ArbitrationCreateDTO;
import com.pet.dto.ArbitrationDecisionDTO;
import com.pet.dto.ArbitrationQuery;
import com.pet.entity.Arbitration;
import com.pet.entity.Order;
import com.pet.entity.ServiceCategory;
import com.pet.entity.User;
import com.pet.mapper.ArbitrationMapper;
import com.pet.mapper.OrderMapper;
import com.pet.mapper.UserMapper;
import com.pet.security.UserContext;
import com.pet.service.ArbitrationService;
import com.pet.service.ServiceCategoryService;
import com.pet.service.WalletService;
import com.pet.vo.ArbitrationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArbitrationServiceImpl extends ServiceImpl<ArbitrationMapper, Arbitration>
        implements ArbitrationService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ServiceCategoryService serviceCategoryService;
    private final WalletService walletService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long orderId, ArbitrationCreateDTO dto) {
        Long userId = UserContext.userId();
        Order order = requireOrder(orderId);
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException(ResultCode.ORDER_ACCESS_DENIED);
        }
        if (!Integer.valueOf(OrderStatus.PENDING_ACCEPT.getCode()).equals(order.getStatus())
                || !Integer.valueOf(PayStatus.PAID.getCode()).equals(order.getPayStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL.getCode(), "仅待验收订单可以提交服务申诉");
        }
        if (count(Wrappers.<Arbitration>lambdaQuery().eq(Arbitration::getOrderId, orderId)) > 0) {
            throw new BusinessException(ResultCode.ARBITRATION_ALREADY_EXISTS);
        }

        // 先用条件更新锁住订单状态：并发提交时只有一个请求能从 4 推进到 7。
        if (orderMapper.markArbitrating(orderId, userId) == 0) {
            throw new BusinessException(ResultCode.ARBITRATION_ALREADY_EXISTS);
        }
        Arbitration arbitration = new Arbitration();
        arbitration.setOrderId(orderId);
        arbitration.setComplainantId(userId);
        arbitration.setReason(StrUtil.trim(dto.getReason()));
        String evidence = CommaListUtil.join(dto.getEvidenceUrls());
        if (evidence != null && evidence.length() > 1000) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "证据照片地址总长度超出限制");
        }
        arbitration.setEvidence(evidence);
        arbitration.setStatus(ArbitrationStatus.PENDING.getCode());
        arbitration.setRefundAmount(BigDecimal.ZERO);
        save(arbitration);
    }

    @Override
    public ArbitrationVO getByOrder(Long orderId) {
        Order order = requireOrder(orderId);
        Long viewerId = UserContext.userId();
        if (!UserContext.isAdmin() && !viewerId.equals(order.getUserId()) && !viewerId.equals(order.getSitterId())) {
            throw new BusinessException(ResultCode.ORDER_ACCESS_DENIED);
        }
        Arbitration arbitration = getOne(Wrappers.<Arbitration>lambdaQuery()
                .eq(Arbitration::getOrderId, orderId).orderByDesc(Arbitration::getId).last("LIMIT 1"));
        if (arbitration == null) {
            return null;
        }
        return toVOs(List.of(arbitration), Map.of(order.getId(), order)).getFirst();
    }

    @Override
    public PageResult<ArbitrationVO> pageAdmin(ArbitrationQuery query) {
        Page<Arbitration> page = page(query.toPage(), Wrappers.<Arbitration>lambdaQuery()
                .eq(query.getStatus() != null, Arbitration::getStatus, query.getStatus())
                .orderByAsc(Arbitration::getStatus)
                .orderByDesc(Arbitration::getId));
        List<Order> orders = loadOrders(page.getRecords());
        Map<Long, Order> orderMap = orders.stream().collect(Collectors.toMap(Order::getId, Function.identity()));
        return new PageResult<>(toVOs(page.getRecords(), orderMap), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decide(Long arbitrationId, ArbitrationDecisionDTO dto) {
        Arbitration arbitration = getById(arbitrationId);
        if (arbitration == null) {
            throw new BusinessException(ResultCode.ARBITRATION_NOT_FOUND);
        }
        if (!Integer.valueOf(ArbitrationStatus.PENDING.getCode()).equals(arbitration.getStatus())) {
            throw new BusinessException(ResultCode.ARBITRATION_ALREADY_DECIDED);
        }
        Order order = requireOrder(arbitration.getOrderId());
        if (!Integer.valueOf(OrderStatus.ARBITRATING.getCode()).equals(order.getStatus())
                || !Integer.valueOf(PayStatus.PAID.getCode()).equals(order.getPayStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
        }

        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        BigDecimal refundAmount = approved ? order.getAmount() : BigDecimal.ZERO;
        String result = StrUtil.trim(dto.getResult());
        if (baseMapper.markDecided(arbitrationId, UserContext.userId(), result, refundAmount) == 0) {
            throw new BusinessException(ResultCode.ARBITRATION_ALREADY_DECIDED);
        }

        if (approved) {
            if (orderMapper.markArbitrationRefunded(order.getId(), "平台仲裁通过：" + result) == 0) {
                throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
            }
            // 订单与申诉两次条件更新成功后才动钱；任一步失败，事务会把三者全部回滚。
            walletService.refundOrder(order.getId(), order.getUserId(), order.getAmount(),
                    "平台仲裁通过，担保资金退回余额");
        } else if (orderMapper.markArbitrationRejected(order.getId()) == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
        }
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private List<Order> loadOrders(List<Arbitration> arbitrations) {
        Set<Long> ids = arbitrations.stream().map(Arbitration::getOrderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return ids.isEmpty() ? List.of() : orderMapper.selectBatchIds(ids);
    }

    private List<ArbitrationVO> toVOs(List<Arbitration> arbitrations, Map<Long, Order> orderMap) {
        Set<Long> userIds = arbitrations.stream()
                .flatMap(a -> {
                    Order order = orderMap.get(a.getOrderId());
                    return java.util.stream.Stream.of(a.getComplainantId(), order == null ? null : order.getSitterId());
                })
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> names = userIds.isEmpty() ? Map.of() : userMapper
                .selectList(Wrappers.<User>lambdaQuery().in(User::getId, userIds)).stream()
                .collect(Collectors.toMap(User::getId,
                        u -> StrUtil.blankToDefault(u.getNickname(), u.getUsername())));

        Set<Long> categoryIds = orderMap.values().stream().map(Order::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> categories = categoryIds.isEmpty() ? Map.of() : serviceCategoryService
                .listByIds(categoryIds).stream().collect(Collectors.toMap(ServiceCategory::getId, ServiceCategory::getName));

        return arbitrations.stream().map(a -> {
            ArbitrationVO vo = new ArbitrationVO();
            vo.setId(a.getId());
            vo.setOrderId(a.getOrderId());
            vo.setComplainantName(names.get(a.getComplainantId()));
            vo.setReason(a.getReason());
            vo.setEvidenceUrls(CommaListUtil.split(a.getEvidence()));
            vo.setStatus(a.getStatus());
            vo.setStatusText(ArbitrationStatus.descOf(a.getStatus()));
            vo.setResult(a.getResult());
            vo.setRefundAmount(a.getRefundAmount());
            vo.setCreateTime(a.getCreateTime());
            vo.setUpdateTime(a.getUpdateTime());
            if (Integer.valueOf(ArbitrationStatus.DECIDED.getCode()).equals(a.getStatus())) {
                vo.setApproved(a.getRefundAmount() != null && a.getRefundAmount().compareTo(BigDecimal.ZERO) > 0);
            }
            Order order = orderMap.get(a.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setOrderAmount(order.getAmount());
                vo.setSitterName(names.get(order.getSitterId()));
                vo.setCategoryName(categories.get(order.getCategoryId()));
            }
            return vo;
        }).toList();
    }
}
