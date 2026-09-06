package com.pet.service.impl;

import com.pet.common.api.ResultCode;
import com.pet.common.enums.OrderStatus;
import com.pet.common.enums.PayStatus;
import com.pet.common.exception.BusinessException;
import com.pet.dto.SitterOrderCancelDTO;
import com.pet.entity.Order;
import com.pet.mapper.OrderMapper;
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import com.pet.service.SitterProfileService;
import com.pet.service.WalletService;
import com.pet.vo.SitterCancelResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 接单员取消订单的退款、时限扣分与幂等性测试。 */
@ExtendWith(MockitoExtension.class)
class SitterOrderCancelTest {

    private static final long ORDER_ID = 88L;
    private static final long OWNER_ID = 2L;
    private static final long SITTER_ID = 3L;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private WalletService walletService;

    @Mock
    private SitterProfileService sitterProfileService;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(null, null, null, null, walletService,
                sitterProfileService, null, null);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        UserContext.set(new LoginUser(SITTER_ID, "sitter", "SITTER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private Order takenOrder(int minutesAgo) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(OWNER_ID);
        order.setSitterId(SITTER_ID);
        order.setStatus(OrderStatus.TAKEN.getCode());
        order.setPayStatus(PayStatus.PAID.getCode());
        order.setTakenTime(LocalDateTime.now().minusMinutes(minutesAgo));
        order.setAmount(new BigDecimal("40.00"));
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        return order;
    }

    private SitterOrderCancelDTO reason(String value) {
        SitterOrderCancelDTO dto = new SitterOrderCancelDTO();
        dto.setReason(value);
        return dto;
    }

    @Test
    @DisplayName("接单不足 30 分钟取消：全额退款但不扣信誉分")
    void cancellationDuringGracePeriodDoesNotDeductCredit() {
        takenOrder(29);
        when(orderMapper.markCancelledBySitter(ORDER_ID, SITTER_ID, "临时身体不适")).thenReturn(1);
        when(orderMapper.markRefunded(ORDER_ID)).thenReturn(1);
        when(sitterProfileService.getCreditScore(SITTER_ID)).thenReturn(100);

        SitterCancelResultVO result = service.cancelBySitter(ORDER_ID, reason("  临时身体不适  "));

        assertThat(result.isCreditDeducted()).isFalse();
        assertThat(result.getDeductedPoints()).isZero();
        assertThat(result.getCreditScore()).isEqualTo(100);
        verify(walletService).refundOrder(ORDER_ID, OWNER_ID, new BigDecimal("40.00"));
        verify(sitterProfileService, never()).deductCreditScore(SITTER_ID, 5);
    }

    @Test
    @DisplayName("接单满 30 分钟后取消：退款并扣 5 分")
    void lateCancellationDeductsFivePoints() {
        takenOrder(31);
        when(orderMapper.markCancelledBySitter(ORDER_ID, SITTER_ID, "时间冲突")).thenReturn(1);
        when(orderMapper.markRefunded(ORDER_ID)).thenReturn(1);
        when(sitterProfileService.deductCreditScore(SITTER_ID, 5)).thenReturn(95);

        SitterCancelResultVO result = service.cancelBySitter(ORDER_ID, reason("时间冲突"));

        assertThat(result.isCreditDeducted()).isTrue();
        assertThat(result.getDeductedPoints()).isEqualTo(5);
        assertThat(result.getCreditScore()).isEqualTo(95);
        verify(walletService).refundOrder(ORDER_ID, OWNER_ID, new BigDecimal("40.00"));
    }

    @Test
    @DisplayName("取消原因空白：请求失败且订单、钱包、信誉分均不变化")
    void blankReasonIsRejected() {
        takenOrder(31);

        assertThatThrownBy(() -> service.cancelBySitter(ORDER_ID, reason("  ")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode()));

        verify(orderMapper, never()).markCancelledBySitter(ORDER_ID, SITTER_ID, "");
        verifyNoInteractions(walletService, sitterProfileService);
    }

    @Test
    @DisplayName("并发或重复取消被条件更新挡住：绝不重复退款或扣分")
    void repeatedCancellationHasNoSideEffects() {
        takenOrder(31);
        when(orderMapper.markCancelledBySitter(ORDER_ID, SITTER_ID, "时间冲突")).thenReturn(0);

        assertThatThrownBy(() -> service.cancelBySitter(ORDER_ID, reason("时间冲突")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_STATUS_ILLEGAL.getCode()));

        verifyNoInteractions(walletService, sitterProfileService);
    }

    @Test
    @DisplayName("不能取消其他接单员的订单")
    void cannotCancelAnotherSittersOrder() {
        Order order = takenOrder(31);
        order.setSitterId(99L);

        assertThatThrownBy(() -> service.cancelBySitter(ORDER_ID, reason("时间冲突")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_ACCESS_DENIED.getCode()));

        verifyNoInteractions(walletService, sitterProfileService);
    }
}
