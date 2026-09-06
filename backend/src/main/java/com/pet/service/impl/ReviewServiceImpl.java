package com.pet.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.api.PageQuery;
import com.pet.common.api.PageResult;
import com.pet.common.api.ResultCode;
import com.pet.common.enums.OrderStatus;
import com.pet.common.exception.BusinessException;
import com.pet.dto.ReviewSaveDTO;
import com.pet.entity.Order;
import com.pet.entity.Review;
import com.pet.entity.ServiceCategory;
import com.pet.entity.User;
import com.pet.mapper.OrderMapper;
import com.pet.mapper.ReviewMapper;
import com.pet.mapper.UserMapper;
import com.pet.security.UserContext;
import com.pet.service.ReviewService;
import com.pet.service.ServiceCategoryService;
import com.pet.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ServiceCategoryService serviceCategoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewVO submit(ReviewSaveDTO dto) {
        Long fromUserId = UserContext.userId();
        Order order = requireReviewable(dto.getOrderId(), fromUserId);
        // uk_order_from 才是最终防线，但先查一次才能回「您已评价过」而不是通用的「数据已存在」；
        // 真撞上并发重复提交时唯一键会拦下第二条，走全局的 DuplicateKeyException 处理
        if (exists(order.getId(), fromUserId)) {
            throw new BusinessException(ResultCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setFromUserId(fromUserId);
        review.setToUserId(counterpart(order, fromUserId));
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        review.setAnonymous(Boolean.TRUE.equals(dto.getAnonymous()) ? 1 : 0);
        save(review);
        return toVOs(List.of(review), fromUserId).get(0);
    }

    @Override
    public List<ReviewVO> listByOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        Long viewerId = UserContext.userId();
        // 与 OrderServiceImpl#getDetail 同一套可见性：当事人双方 + 管理员
        boolean visible = order.getUserId().equals(viewerId)
                || viewerId.equals(order.getSitterId())
                || UserContext.isAdmin();
        if (!visible) {
            throw new BusinessException(ResultCode.ORDER_ACCESS_DENIED);
        }
        return toVOs(list(Wrappers.<Review>lambdaQuery()
                .eq(Review::getOrderId, orderId)
                .orderByAsc(Review::getId)), viewerId);
    }

    @Override
    public PageResult<ReviewVO> pageReceived(PageQuery query) {
        Long userId = UserContext.userId();
        Page<Review> page = baseMapper.selectPage(query.toPage(), Wrappers.<Review>lambdaQuery()
                .eq(Review::getToUserId, userId)
                .orderByDesc(Review::getId));
        // 不用 PageResult.of(page, mapper)：拼订单号、类别名与昵称都要整页 id 批量查，逐行转换会 N+1
        List<ReviewVO> records = toVOs(page.getRecords(), userId);
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 这一单能不能被这个人评。
     * <p>
     * 管理员不在其列：他不是当事人，替任何一方打分都会污染信誉数据。
     */
    private Order requireReviewable(Long orderId, Long viewerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(viewerId) && !viewerId.equals(order.getSitterId())) {
            throw new BusinessException(ResultCode.ORDER_ACCESS_DENIED);
        }
        // 只允许评已完成的单：服务还没结束就打分，星级既不反映履约质量，
        // 也能被拿来在服务中途要挟对方（「不给好评就……」）
        if (order.getStatus() == null || order.getStatus() != OrderStatus.COMPLETED.getCode()) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL.getCode(), "订单完成后才能评价");
        }
        return order;
    }

    /** 双向评价的对手方：我是下单用户就评接单员，反之评下单用户。 */
    private Long counterpart(Order order, Long viewerId) {
        return order.getUserId().equals(viewerId) ? order.getSitterId() : order.getUserId();
    }

    private boolean exists(Long orderId, Long fromUserId) {
        // 直接走 baseMapper.selectCount 而不是 ServiceImpl#count，理由同 WalletServiceImpl#requireWallet：
        // 转发链条末端落到哪个重载由框架版本决定，单测里没有确定的拦截点
        return baseMapper.selectCount(Wrappers.<Review>lambdaQuery()
                .eq(Review::getOrderId, orderId)
                .eq(Review::getFromUserId, fromUserId)) > 0;
    }

    private List<ReviewVO> toVOs(List<Review> reviews, Long viewerId) {
        if (reviews.isEmpty()) {
            return List.of();
        }
        Map<Long, Order> orders = loadOrders(reviews);
        Map<Long, String> nicknames = loadNicknames(reviews);
        Map<Long, String> categoryNames = loadCategoryNames(orders.values());
        return reviews.stream()
                .map(r -> toVO(r, viewerId, orders.get(r.getOrderId()), nicknames, categoryNames))
                .toList();
    }

    private ReviewVO toVO(Review r, Long viewerId, Order order,
                          Map<Long, String> nicknames, Map<Long, String> categoryNames) {
        ReviewVO vo = new ReviewVO();
        vo.setId(r.getId());
        vo.setOrderId(r.getOrderId());
        vo.setToUserId(r.getToUserId());
        vo.setRating(r.getRating());
        vo.setContent(r.getContent());
        vo.setCreateTime(r.getCreateTime());
        boolean mine = r.getFromUserId().equals(viewerId);
        boolean anonymous = r.getAnonymous() != null && r.getAnonymous() == 1;
        vo.setMine(mine);
        vo.setAnonymous(anonymous);
        // 自己看自己写的匿名评价照常署名，他本来就知道是谁；别人的匿名评价连 id 一起藏
        if (mine || !anonymous) {
            vo.setFromUserId(r.getFromUserId());
            vo.setFromNickname(nicknames.get(r.getFromUserId()));
        }
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
            vo.setCategoryName(categoryNames.get(order.getCategoryId()));
        }
        return vo;
    }

    private Map<Long, Order> loadOrders(List<Review> reviews) {
        Set<Long> ids = reviews.stream().map(Review::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        // 空集合必须短路：MyBatis-Plus 的 in() 收到空集合会生成 IN () 导致 SQL 语法错误
        if (ids.isEmpty()) {
            return Map.of();
        }
        return orderMapper.selectList(Wrappers.<Order>lambdaQuery().in(Order::getId, ids)).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
    }

    /** 批量取评价人昵称；昵称为空时退回用户名，免得列表里出现一片空白。 */
    private Map<Long, String> loadNicknames(List<Review> reviews) {
        Set<Long> ids = reviews.stream().map(Review::getFromUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(Wrappers.<User>lambdaQuery().in(User::getId, ids)).stream()
                .collect(Collectors.toMap(User::getId, u -> StrUtil.blankToDefault(u.getNickname(), u.getUsername())));
    }

    private Map<Long, String> loadCategoryNames(Collection<Order> orders) {
        Set<Long> ids = orders.stream().map(Order::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return serviceCategoryService.listByIds(ids).stream()
                .collect(Collectors.toMap(ServiceCategory::getId, ServiceCategory::getName));
    }
}
