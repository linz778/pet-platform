package com.pet.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import com.pet.service.ServiceCategoryService;
import com.pet.vo.ReviewVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁死双向评价的两件容易出错的事：被评价人由谁决定、匿名到底藏住了什么。
 * <p>
 * 评价方向若采信前端传参，任何人都能给任意用户刷差评；匿名若只藏昵称而留下 userId，
 * 前端拿 id 调一次用户接口就还原了身份——两者都是「代码看着对、上线才出事」的类型。
 * 纯 Mockito，不启 Spring 上下文，不依赖本机 MySQL / Redis。
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final long ORDER_ID = 77L;
    private static final long OWNER_ID = 2L;
    private static final long SITTER_ID = 3L;
    private static final long ADMIN_ID = 1L;
    private static final long STRANGER_ID = 99L;
    private static final long CATEGORY_ID = 1L;
    private static final String ORDER_NO = "PO20260905120000123456";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 5, 12, 30, 0);

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ServiceCategoryService serviceCategoryService;

    private ReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(orderMapper, userMapper, serviceCategoryService);
        // baseMapper 是 ServiceImpl 的父类字段，@InjectMocks 注不进去，只能反射塞
        ReflectionTestUtils.setField(service, "baseMapper", reviewMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private void loginAs(long userId, String role) {
        UserContext.set(new LoginUser(userId, "u" + userId, role));
    }

    /** 造一笔已完成（验收结算过）的订单，并挂到 selectById 上 */
    private Order completedOrder() {
        Order order = newOrder();
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        return order;
    }

    /** 只造对象不打桩：pageReceived 不读单条订单，多打一个桩会被严格模式判成无用桩 */
    private Order newOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setOrderNo(ORDER_NO);
        order.setUserId(OWNER_ID);
        order.setSitterId(SITTER_ID);
        order.setCategoryId(CATEGORY_ID);
        order.setStatus(OrderStatus.COMPLETED.getCode());
        return order;
    }

    private Review review(long id, long fromUserId, long toUserId, int rating, String content, int anonymous) {
        Review review = new Review();
        review.setId(id);
        review.setOrderId(ORDER_ID);
        review.setFromUserId(fromUserId);
        review.setToUserId(toUserId);
        review.setRating(rating);
        review.setContent(content);
        review.setAnonymous(anonymous);
        review.setCreateTime(CREATED_AT);
        return review;
    }

    /**
     * 拼 VO 要批量回查订单号、评价人昵称与类别名。
     * 接单员刻意留空昵称：顺带覆盖「昵称为空退回用户名」，否则列表里会出现一片空白。
     */
    private void stubVoLookups(Order order) {
        when(orderMapper.selectList(any())).thenReturn(List.of(order));
        User owner = new User();
        owner.setId(OWNER_ID);
        owner.setUsername("user");
        owner.setNickname("宠物主人");
        User sitter = new User();
        sitter.setId(SITTER_ID);
        sitter.setUsername("sitter");
        when(userMapper.selectList(any())).thenReturn(List.of(owner, sitter));
        ServiceCategory category = new ServiceCategory();
        category.setId(CATEGORY_ID);
        category.setName("上门喂养");
        when(serviceCategoryService.listByIds(any())).thenReturn(List.of(category));
    }

    private ReviewSaveDTO dto(int rating, String content, Boolean anonymous) {
        ReviewSaveDTO dto = new ReviewSaveDTO();
        dto.setOrderId(ORDER_ID);
        dto.setRating(rating);
        dto.setContent(content);
        dto.setAnonymous(anonymous);
        return dto;
    }

    /**
     * insert 在真实环境里会回填自增 id，并由 MetaObjectHandler 就地填上 create_time；
     * mock 里得手动补这两件事，否则返回的 VO 既没有 id 也没有时间可断言。
     */
    private void stubInsertFillsGeneratedFields(long id) {
        when(reviewMapper.insert(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0, Review.class);
            saved.setId(id);
            saved.setCreateTime(CREATED_AT);
            return 1;
        });
    }

    // ───────────────────────── 评价方向 ─────────────────────────

    @Test
    @DisplayName("下单用户评接单员：被评价人由订单归属反推，落库带上星级与订单号")
    void ownerReviewIsDirectedToSitter() {
        Order order = completedOrder();
        stubVoLookups(order);
        stubInsertFillsGeneratedFields(9L);
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        loginAs(OWNER_ID, "USER");

        ReviewVO vo = service.submit(dto(5, "喂得很仔细，照片齐全", null));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(ORDER_ID);
        assertThat(captor.getValue().getFromUserId()).isEqualTo(OWNER_ID);
        assertThat(captor.getValue().getToUserId()).isEqualTo(SITTER_ID);
        assertThat(captor.getValue().getRating()).isEqualTo(5);
        assertThat(captor.getValue().getContent()).isEqualTo("喂得很仔细，照片齐全");
        // anonymous 不传按实名处理，落库不能是 null（列是 NOT NULL）
        assertThat(captor.getValue().getAnonymous()).isZero();

        assertThat(vo.getId()).isEqualTo(9L);
        assertThat(vo.getMine()).isTrue();
        assertThat(vo.getAnonymous()).isFalse();
        assertThat(vo.getFromUserId()).isEqualTo(OWNER_ID);
        assertThat(vo.getFromNickname()).isEqualTo("宠物主人");
        assertThat(vo.getToUserId()).isEqualTo(SITTER_ID);
        assertThat(vo.getOrderNo()).isEqualTo(ORDER_NO);
        assertThat(vo.getCategoryName()).isEqualTo("上门喂养");
        assertThat(vo.getCreateTime()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("接单员评下单用户：方向反过来，评的是下单用户而不是自己")
    void sitterReviewIsDirectedToOwner() {
        Order order = completedOrder();
        stubVoLookups(order);
        stubInsertFillsGeneratedFields(10L);
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        loginAs(SITTER_ID, "SITTER");

        ReviewVO vo = service.submit(dto(4, "门禁密码给得很清楚", false));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(captor.capture());
        assertThat(captor.getValue().getFromUserId()).isEqualTo(SITTER_ID);
        assertThat(captor.getValue().getToUserId()).isEqualTo(OWNER_ID);
        assertThat(vo.getMine()).isTrue();
        // 昵称留空时退回用户名，否则接单员这条评价在列表里是空白署名
        assertThat(vo.getFromNickname()).isEqualTo("sitter");
    }

    // ───────────────────────── 准入与幂等 ─────────────────────────

    /**
     * 服务还没结束就能打分的话，星级既不反映履约质量，也给了双方一个中途要挟的把手
     * （「不给好评就不喂了」）。评价必须发生在验收结算之后。
     */
    @Test
    @DisplayName("订单未完成不能评价：待验收也只回 2003 并说明原因")
    void unfinishedOrderCannotBeReviewed() {
        Order order = completedOrder();
        order.setStatus(OrderStatus.PENDING_ACCEPT.getCode());
        loginAs(OWNER_ID, "USER");

        assertThatThrownBy(() -> service.submit(dto(1, "服务还没完就给差评", null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_STATUS_ILLEGAL.getCode());
                    assertThat(e.getMessage()).isEqualTo("订单完成后才能评价");
                });

        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    @DisplayName("与这一单无关的人不能评价，回 2005 而不是伪装成订单不存在")
    void strangerCannotReviewOthersOrder() {
        completedOrder();
        loginAs(STRANGER_ID, "USER");

        assertThatThrownBy(() -> service.submit(dto(1, "刷个差评", null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_ACCESS_DENIED.getCode()));

        verify(reviewMapper, never()).insert(any(Review.class));
    }

    /**
     * 管理员能看双方评价（处理纠纷要用），但不是当事人，替任何一方打分都会污染信誉数据。
     */
    @Test
    @DisplayName("管理员不是当事人，不能替任何一方评价")
    void adminCannotReview() {
        completedOrder();
        loginAs(ADMIN_ID, "ADMIN");

        assertThatThrownBy(() -> service.submit(dto(5, "平台代评", null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_ACCESS_DENIED.getCode()));

        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    @DisplayName("订单不存在返回 2001")
    void missingOrderIsNotFound() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(null);
        loginAs(OWNER_ID, "USER");

        assertThatThrownBy(() -> service.submit(dto(5, "评一单不存在的订单", null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_NOT_FOUND.getCode()));

        verify(reviewMapper, never()).insert(any(Review.class));
    }

    /** uk_order_from 是最终防线，但先查一次才能回「您已评价过」而不是通用的「数据已存在」。 */
    @Test
    @DisplayName("同一人对同一单重复评价返回 2009，且不再插一条")
    void duplicateReviewIsRejected() {
        completedOrder();
        when(reviewMapper.selectCount(any())).thenReturn(1L);
        loginAs(OWNER_ID, "USER");

        assertThatThrownBy(() -> service.submit(dto(1, "改个差评", null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.REVIEW_ALREADY_EXISTS.getCode()));

        verify(reviewMapper, never()).insert(any(Review.class));
    }

    /** 对方评过我不影响我评对方：唯一键是 (order_id, from_user_id)，不是 order_id。 */
    @Test
    @DisplayName("对方已经评过，我这一侧照样能评")
    void bothDirectionsReviewIndependently() {
        Order order = completedOrder();
        stubVoLookups(order);
        stubInsertFillsGeneratedFields(11L);
        // 唯一键查的是「我评过没有」，条件里带上了 from_user_id，所以对方的评价不会让我误判成重复
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        loginAs(SITTER_ID, "SITTER");

        service.submit(dto(5, "很好的雇主", null));

        verify(reviewMapper).insert(any(Review.class));
    }

    // ───────────────────────── 匿名与可见性 ─────────────────────────

    @Test
    @DisplayName("匿名评价落库为 1，但提交人自己看自己那条依然署实名")
    void anonymousSubmitKeepsSignatureForAuthor() {
        Order order = completedOrder();
        stubVoLookups(order);
        stubInsertFillsGeneratedFields(12L);
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        loginAs(OWNER_ID, "USER");

        ReviewVO vo = service.submit(dto(2, "猫砂没铲干净", true));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(captor.capture());
        assertThat(captor.getValue().getAnonymous()).isEqualTo(1);
        assertThat(vo.getAnonymous()).isTrue();
        assertThat(vo.getMine()).isTrue();
        assertThat(vo.getFromUserId()).isEqualTo(OWNER_ID);
        assertThat(vo.getFromNickname()).isEqualTo("宠物主人");
    }

    /**
     * 匿名只藏昵称是不够的：留下 fromUserId，前端拿 id 调一次用户接口就还原了身份。
     * 两个字段必须一起消失（Jackson non_null 下键都不出现）。
     */
    @Test
    @DisplayName("看对方的匿名评价：评价人 id 与昵称都不下发，自己那条照常署名")
    void anonymousReviewHidesAuthorFromCounterpart() {
        Order order = completedOrder();
        // 用户匿名评了接单员，接单员实名评了用户
        when(reviewMapper.selectList(any())).thenReturn(List.of(
                review(20L, OWNER_ID, SITTER_ID, 2, "猫砂没铲干净", 1),
                review(21L, SITTER_ID, OWNER_ID, 5, "很好的雇主", 0)));
        stubVoLookups(order);
        loginAs(SITTER_ID, "SITTER");

        List<ReviewVO> vos = service.listByOrder(ORDER_ID);

        assertThat(vos).hasSize(2);
        ReviewVO anonymousOne = vos.get(0);
        assertThat(anonymousOne.getAnonymous()).isTrue();
        assertThat(anonymousOne.getMine()).isFalse();
        assertThat(anonymousOne.getFromUserId()).isNull();
        assertThat(anonymousOne.getFromNickname()).isNull();
        assertThat(anonymousOne.getRating()).isEqualTo(2);
        assertThat(anonymousOne.getContent()).isEqualTo("猫砂没铲干净");

        ReviewVO namedOne = vos.get(1);
        assertThat(namedOne.getAnonymous()).isFalse();
        assertThat(namedOne.getMine()).isTrue();
        assertThat(namedOne.getFromUserId()).isEqualTo(SITTER_ID);
        assertThat(namedOne.getFromNickname()).isEqualTo("sitter");
    }

    @Test
    @DisplayName("同一单匿名评价，作者自己看时署名照常显示")
    void anonymousReviewKeepsAuthorForSelf() {
        Order order = completedOrder();
        when(reviewMapper.selectList(any())).thenReturn(List.of(
                review(20L, OWNER_ID, SITTER_ID, 2, "猫砂没铲干净", 1)));
        stubVoLookups(order);
        loginAs(OWNER_ID, "USER");

        ReviewVO vo = service.listByOrder(ORDER_ID).get(0);

        assertThat(vo.getAnonymous()).isTrue();
        assertThat(vo.getMine()).isTrue();
        assertThat(vo.getFromUserId()).isEqualTo(OWNER_ID);
        assertThat(vo.getFromNickname()).isEqualTo("宠物主人");
    }

    @Test
    @DisplayName("某单评价对当事人双方与管理员可见，陌生人回 2005")
    void orderReviewsAreVisibleToBothPartiesAndAdmin() {
        Order order = completedOrder();
        when(reviewMapper.selectList(any())).thenReturn(List.of());

        loginAs(OWNER_ID, "USER");
        assertThat(service.listByOrder(ORDER_ID)).isEmpty();
        loginAs(SITTER_ID, "SITTER");
        assertThat(service.listByOrder(ORDER_ID)).isEmpty();
        loginAs(ADMIN_ID, "ADMIN");
        assertThat(service.listByOrder(ORDER_ID)).isEmpty();

        loginAs(STRANGER_ID, "USER");
        assertThatThrownBy(() -> service.listByOrder(ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_ACCESS_DENIED.getCode()));
        verify(orderMapper, times(4)).selectById(ORDER_ID);
    }

    // ───────────────────────── 我收到的评价 ─────────────────────────

    @Test
    @DisplayName("我收到的评价：按 id 倒序分页，回显 page/size，收到的评价都不是我写的")
    void pageReceivedReturnsPagedVoWithOrderContext() {
        Order order = newOrder();
        Page<Review> page = new Page<>(2, 5);
        page.setRecords(List.of(review(31L, OWNER_ID, SITTER_ID, 5, "很专业", 0)));
        page.setTotal(11);
        when(reviewMapper.selectPage(any(), any())).thenReturn(page);
        stubVoLookups(order);
        loginAs(SITTER_ID, "SITTER");

        PageQuery query = new PageQuery();
        query.setPage(2);
        query.setSize(5);
        PageResult<ReviewVO> result = service.pageReceived(query);

        assertThat(result.getTotal()).isEqualTo(11);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getRecords()).hasSize(1);
        ReviewVO vo = result.getRecords().get(0);
        assertThat(vo.getMine()).isFalse();
        assertThat(vo.getToUserId()).isEqualTo(SITTER_ID);
        // 收到的评价要能看出是哪一单、什么服务，否则接单员无从对号入座
        assertThat(vo.getOrderNo()).isEqualTo(ORDER_NO);
        assertThat(vo.getCategoryName()).isEqualTo("上门喂养");
        // 整页只批量查一次，逐行转换会 N+1
        verify(orderMapper, times(1)).selectList(any());
        verify(userMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("还没收到任何评价时返回空列表，不去回查订单与昵称")
    void pageReceivedShortCircuitsOnEmptyPage() {
        Page<Review> page = new Page<>(1, 10);
        page.setRecords(List.of());
        when(reviewMapper.selectPage(any(), any())).thenReturn(page);
        loginAs(SITTER_ID, "SITTER");

        PageResult<ReviewVO> result = service.pageReceived(new PageQuery());

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isZero();
        // 空集合必须短路：MyBatis-Plus 的 in() 收到空集合会生成 IN () 直接 SQL 语法错误
        verify(orderMapper, never()).selectList(any());
        verify(userMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("订单不存在时某单评价返回 2001")
    void listByOrderMissingOrderIsNotFound() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(null);
        loginAs(OWNER_ID, "USER");

        assertThatThrownBy(() -> service.listByOrder(ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_NOT_FOUND.getCode()));

        verify(reviewMapper, never()).selectList(any());
    }
}
