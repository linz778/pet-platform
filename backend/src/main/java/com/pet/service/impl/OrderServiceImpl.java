package com.pet.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.api.PageResult;
import com.pet.common.api.ResultCode;
import com.pet.common.enums.OrderStatus;
import com.pet.common.enums.PayStatus;
import com.pet.common.exception.BusinessException;
import com.pet.common.geo.OrderGeoIndex;
import com.pet.dto.OrderCancelDTO;
import com.pet.dto.OrderCreateDTO;
import com.pet.dto.OrderQuery;
import com.pet.entity.Order;
import com.pet.entity.Pet;
import com.pet.entity.ServiceCategory;
import com.pet.mapper.OrderMapper;
import com.pet.mapper.PetMapper;
import com.pet.security.UserContext;
import com.pet.service.OrderService;
import com.pet.service.ServiceCategoryService;
import com.pet.service.PetService;
import com.pet.service.WalletService;
import com.pet.vo.OrderDetailVO;
import com.pet.vo.OrderListVO;
import com.pet.vo.PricePreviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PetService petService;
    private final PetMapper petMapper;
    private final ServiceCategoryService serviceCategoryService;
    private final WalletService walletService;
    private final OrderGeoIndex geoIndex;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO create(OrderCreateDTO dto) {
        Long userId = UserContext.userId();
        // 校验宠物归属：不校验就能拿别人的 petId 下单，让陌生接单员上门到别人家
        petService.requireMine(dto.getPetId());
        validateServiceTime(dto.getServiceStart(), dto.getServiceEnd());
        // 计价一律走 previewPrice，全项目只有这一份算法，保证「预览多少就付多少」；
        // 顺带挡掉对已下架或不存在的类别下单
        PricePreviewVO price = serviceCategoryService.previewPrice(dto.getCategoryId(), dto.getServiceStart());

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPetId(dto.getPetId());
        order.setCategoryId(dto.getCategoryId());
        order.setServiceAddress(dto.getServiceAddress());
        order.setAddressLat(dto.getAddressLat());
        order.setAddressLng(dto.getAddressLng());
        order.setServiceStart(dto.getServiceStart());
        order.setServiceEnd(dto.getServiceEnd());
        order.setAmount(price.getAmount());
        order.setCommission(price.getCommission());
        order.setSitterIncome(price.getSitterIncome());
        order.setStatus(OrderStatus.UNPAID.getCode());
        order.setPayStatus(PayStatus.UNPAID.getCode());
        order.setRemark(dto.getRemark());
        save(order);
        return getDetail(order.getId());
    }

    @Override
    public PageResult<OrderListVO> pageMine(OrderQuery query) {
        Page<Order> page = page(query.toPage(), Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, UserContext.userId())
                .eq(query.getStatus() != null, Order::getStatus, query.getStatus())
                .orderByDesc(Order::getId));
        // 不用 PageResult.of(page, mapper)：拼宠物名与类别名需要整页的 id 集合去批量查，逐行转换会 N+1
        List<OrderListVO> records = toListVOs(page.getRecords());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public OrderDetailVO getDetail(Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        Long viewerId = UserContext.userId();
        boolean isOwner = order.getUserId().equals(viewerId);
        boolean isSitter = viewerId.equals(order.getSitterId());
        boolean isAdmin = UserContext.isAdmin();
        if (!isOwner && !isSitter && !isAdmin) {
            throw new BusinessException(ResultCode.ORDER_ACCESS_DENIED);
        }

        List<Order> one = List.of(order);
        Map<Long, ServiceCategory> categories = loadCategories(one);
        Map<Long, Pet> pets = loadPets(one);

        OrderDetailVO vo = new OrderDetailVO();
        fillCommon(vo, order, categories, pets);
        vo.setAddressLat(order.getAddressLat());
        vo.setAddressLng(order.getAddressLng());
        vo.setRemark(order.getRemark());
        vo.setPayTime(order.getPayTime());
        vo.setTakenTime(order.getTakenTime());
        vo.setCheckinTime(order.getCheckinTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setAcceptTime(order.getAcceptTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());

        Pet pet = pets.get(order.getPetId());
        if (pet != null) {
            vo.setPetSpecies(pet.getSpecies());
            vo.setPetBreed(pet.getBreed());
            vo.setPetAvatar(pet.getAvatar());
        }

        // 平台分成只对管理员与该单接单员赋值，对下单用户保持 null —— non_null 下这个键会彻底消失。
        // 这不是漏赋值，别「顺手补全」，理由见 OrderDetailVO#commission 的注释。
        if (isAdmin || isSitter) {
            vo.setCommission(order.getCommission());
            vo.setSitterIncome(order.getSitterIncome());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId) {
        Long userId = UserContext.userId();
        Order order = requireOwned(orderId, userId);

        // markPaid 带 status = 0 AND pay_status = 0 条件，只有影响行数为 1 才允许动钱包。
        // 写成「先查再改」的话，连点两次支付会给同一笔订单冻结两次。
        if (baseMapper.markPaid(orderId) == 0) {
            boolean alreadyPaid = order.getPayStatus() != null && order.getPayStatus() != PayStatus.UNPAID.getCode();
            throw new BusinessException(alreadyPaid ? ResultCode.ORDER_ALREADY_PAID : ResultCode.ORDER_STATUS_ILLEGAL);
        }
        // 余额不足时这里抛异常，上面的 markPaid 会随事务一起回滚，订单退回待支付
        walletService.payOrder(orderId, userId, order.getAmount());

        // GEO 索引写在事务提交之前，若提交失败会留下一条脏数据。可接受：检索侧会拿候选 id
        // 回 MySQL 用 status = 1 二次过滤，脏 id 顶多让某次查询白跑一趟，不会漏单也不会错单。
        geoIndex.add(orderId, order.getAddressLng(), order.getAddressLat());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, OrderCancelDTO dto) {
        Long userId = UserContext.userId();
        Order order = requireOwned(orderId, userId);
        String reason = StrUtil.blankToDefault(dto == null ? null : dto.getReason(), "用户主动取消");

        // 条件更新限定 status IN (0, 1)：已接单之后的取消要走仲裁流程，本期不实现
        if (baseMapper.markCancelled(orderId, reason) == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
        }
        if (order.getPayStatus() != null && order.getPayStatus() == PayStatus.PAID.getCode()) {
            // markRefunded 带 pay_status = 1 条件，是退款的第二道幂等防线：
            // 即使两个取消请求同时越过了 markCancelled，也只有一个能把钱退回去
            if (baseMapper.markRefunded(orderId) == 1) {
                walletService.refundOrder(orderId, userId, order.getAmount());
            }
        }
        geoIndex.remove(orderId);
    }

    /** 订单必须是当前登录用户下的单，否则连「订单不存在」都不必伪装——直接回无权操作。 */
    private Order requireOwned(Long orderId, Long userId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_ACCESS_DENIED);
        }
        return order;
    }

    private void validateServiceTime(LocalDateTime start, LocalDateTime end) {
        if (!start.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.SERVICE_TIME_ILLEGAL);
        }
        if (end != null && !end.isAfter(start)) {
            throw new BusinessException(ResultCode.SERVICE_TIME_ILLEGAL.getCode(), "预约结束时间必须晚于开始时间");
        }
    }

    /** PO + 秒级时间戳 + 6 位随机数，共 22 字符；uk_order_no 兜底，撞号概率可忽略。 */
    private String generateOrderNo() {
        return "PO" + LocalDateTime.now().format(ORDER_NO_TIME) + RandomUtil.randomNumbers(6);
    }

    private List<OrderListVO> toListVOs(List<Order> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, ServiceCategory> categories = loadCategories(orders);
        Map<Long, Pet> pets = loadPets(orders);
        return orders.stream().map(o -> {
            OrderListVO vo = new OrderListVO();
            fillCommon(vo, o, categories, pets);
            return vo;
        }).toList();
    }

    /** 列表与详情共用的公共字段填充，详情 VO 继承列表 VO 所以能直接传进来。 */
    private void fillCommon(OrderListVO vo, Order o, Map<Long, ServiceCategory> categories, Map<Long, Pet> pets) {
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setCategoryId(o.getCategoryId());
        vo.setPetId(o.getPetId());
        vo.setServiceAddress(o.getServiceAddress());
        vo.setServiceStart(o.getServiceStart());
        vo.setServiceEnd(o.getServiceEnd());
        vo.setAmount(o.getAmount());
        vo.setStatus(o.getStatus());
        vo.setStatusText(OrderStatus.descOf(o.getStatus()));
        vo.setPayStatus(o.getPayStatus());
        vo.setPayStatusText(PayStatus.descOf(o.getPayStatus()));
        vo.setCreateTime(o.getCreateTime());

        ServiceCategory category = categories.get(o.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
            vo.setUnit(category.getUnit());
        }
        Pet pet = pets.get(o.getPetId());
        if (pet != null) {
            vo.setPetName(pet.getName());
            vo.setPetDeleted(pet.getDeleted() != null && pet.getDeleted() == 1);
        }
    }

    private Map<Long, ServiceCategory> loadCategories(List<Order> orders) {
        Set<Long> ids = orders.stream().map(Order::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        // 空集合必须短路：MyBatis-Plus 的 in() 收到空集合会生成 IN () 导致 SQL 语法错误
        if (ids.isEmpty()) {
            return Map.of();
        }
        return serviceCategoryService.listByIds(ids).stream()
                .collect(Collectors.toMap(ServiceCategory::getId, Function.identity()));
    }

    private Map<Long, Pet> loadPets(List<Order> orders) {
        Set<Long> ids = orders.stream().map(Order::getPetId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return petMapper.selectSnapshots(ids).stream()
                .collect(Collectors.toMap(Pet::getId, Function.identity()));
    }
}
