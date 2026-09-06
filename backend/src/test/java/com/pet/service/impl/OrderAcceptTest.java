package com.pet.service.impl;

import com.pet.common.api.ResultCode;
import com.pet.common.enums.OrderStatus;
import com.pet.common.enums.PayStatus;
import com.pet.common.exception.BusinessException;
import com.pet.entity.Order;
import com.pet.mapper.OrderMapper;
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import com.pet.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 锁死验收这一步的「状态推进 → 结算」次序。
 * <p>
 * 结算是资金流的终点，也是唯一会给接单员和平台同时加钱的地方。次序写反或条件更新失效，
 * 表现是「验收一次钱到账两次」，而这种问题在演示现场只能靠流水倒查。
 * 纯 Mockito，不启 Spring 上下文，不依赖本机 MySQL / Redis。
 */
@ExtendWith(MockitoExtension.class)
class OrderAcceptTest {

    private static final long ORDER_ID = 77L;
    private static final long OWNER_ID = 2L;
    private static final long SITTER_ID = 3L;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private WalletService walletService;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        // 验收路径上只用得到订单 mapper 与钱包服务，其余六个依赖全传 null
        service = new OrderServiceImpl(null, null, null, null, walletService, null, null, null);
        // baseMapper 是 ServiceImpl 的父类字段，@InjectMocks 注不进去，只能反射塞
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        UserContext.set(new LoginUser(OWNER_ID, "user", "USER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    /** 造一笔待验收（服务已完成、担保资金还在冻结中）的订单 */
    private Order pendingAcceptOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(OWNER_ID);
        order.setSitterId(SITTER_ID);
        order.setStatus(OrderStatus.PENDING_ACCEPT.getCode());
        order.setPayStatus(PayStatus.PAID.getCode());
        order.setAmount(new BigDecimal("60.00"));
        order.setCommission(new BigDecimal("6.00"));
        order.setSitterIncome(new BigDecimal("54.00"));
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        return order;
    }

    @Test
    @DisplayName("验收成功：按订单行上的拆分把到手与抽成交给钱包结算")
    void acceptSettlesWithAmountsFromOrderRow() {
        pendingAcceptOrder();
        when(orderMapper.markAccepted(ORDER_ID)).thenReturn(1);

        service.accept(ORDER_ID);

        // 金额必须取自订单行而不是请求参数：前端传什么都不能被采信
        verify(walletService).settleOrder(ORDER_ID, OWNER_ID, SITTER_ID,
                new BigDecimal("54.00"), new BigDecimal("6.00"));
    }

    /**
     * markAccepted 返回 0 说明订单已经不在「待验收」——最常见的就是用户连点两次或前端超时重试。
     * 此时若还去结算，接单员和平台会各收到两笔钱，而用户的冻结额只够扣一次。
     */
    @Test
    @DisplayName("重复验收被条件更新挡住：抛 2003 且完全不碰钱包")
    void repeatAcceptNeverSettlesTwice() {
        pendingAcceptOrder();
        when(orderMapper.markAccepted(ORDER_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.accept(ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_STATUS_ILLEGAL.getCode()));

        verifyNoInteractions(walletService);
    }

    /**
     * 接单员自己点验收是最该挡住的一种：钱会立刻进他自己口袋，而服务可能根本没做完。
     * 控制器的 @RequireRole({"USER"}) 已经挡在门外，这里是第二道防线——
     * 直接调接口、或将来有人把这个方法复用到别的入口时，归属校验不能缺席。
     */
    @Test
    @DisplayName("接单员不能给自己验收：即便这一单是他接的，也只回 2005")
    void sitterCannotAcceptOwnOrder() {
        pendingAcceptOrder();
        UserContext.set(new LoginUser(SITTER_ID, "sitter", "SITTER"));

        assertThatThrownBy(() -> service.accept(ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_ACCESS_DENIED.getCode()));

        verify(orderMapper, never()).markAccepted(any());
        verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("订单不存在返回 2001，而不是含混的「状态不允许」")
    void missingOrderIsNotFound() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.accept(ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_NOT_FOUND.getCode()));

        verify(orderMapper, never()).markAccepted(any());
        verifyNoInteractions(walletService);
    }
}
