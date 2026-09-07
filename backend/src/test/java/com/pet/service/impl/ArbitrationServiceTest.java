package com.pet.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pet.common.api.ResultCode;
import com.pet.common.enums.ArbitrationStatus;
import com.pet.common.enums.OrderStatus;
import com.pet.common.enums.PayStatus;
import com.pet.common.exception.BusinessException;
import com.pet.dto.ArbitrationCreateDTO;
import com.pet.dto.ArbitrationDecisionDTO;
import com.pet.entity.Arbitration;
import com.pet.entity.Order;
import com.pet.mapper.ArbitrationMapper;
import com.pet.mapper.OrderMapper;
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import com.pet.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 申诉状态机与退款副作用测试。 */
@ExtendWith(MockitoExtension.class)
class ArbitrationServiceTest {

    private static final long ARBITRATION_ID = 9L;
    private static final long ORDER_ID = 77L;
    private static final long OWNER_ID = 2L;
    private static final long SITTER_ID = 3L;
    private static final long ADMIN_ID = 1L;

    @Mock
    private ArbitrationMapper arbitrationMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private WalletService walletService;

    private ArbitrationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArbitrationServiceImpl(orderMapper, null, null, walletService);
        ReflectionTestUtils.setField(service, "baseMapper", arbitrationMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private Order pendingAcceptOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setOrderNo("PO-TEST");
        order.setUserId(OWNER_ID);
        order.setSitterId(SITTER_ID);
        order.setStatus(OrderStatus.PENDING_ACCEPT.getCode());
        order.setPayStatus(PayStatus.PAID.getCode());
        order.setAmount(new BigDecimal("35.00"));
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        return order;
    }

    private Arbitration pendingArbitration() {
        Arbitration arbitration = new Arbitration();
        arbitration.setId(ARBITRATION_ID);
        arbitration.setOrderId(ORDER_ID);
        arbitration.setComplainantId(OWNER_ID);
        arbitration.setStatus(ArbitrationStatus.PENDING.getCode());
        when(arbitrationMapper.selectById(ARBITRATION_ID)).thenReturn(arbitration);
        return arbitration;
    }

    private ArbitrationDecisionDTO decision(boolean approved) {
        ArbitrationDecisionDTO dto = new ArbitrationDecisionDTO();
        dto.setApproved(approved);
        dto.setResult(approved ? "服务未按清单完成，同意退款" : "现有证据不足，申诉驳回");
        return dto;
    }

    @Test
    @DisplayName("用户提交申诉：订单先进入仲裁中，再保存原因与多张证据")
    void submitMovesOrderAndPersistsEvidence() {
        UserContext.set(new LoginUser(OWNER_ID, "user", "USER"));
        pendingAcceptOrder();
        when(arbitrationMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(orderMapper.markArbitrating(ORDER_ID, OWNER_ID)).thenReturn(1);
        when(arbitrationMapper.insert(any(Arbitration.class))).thenReturn(1);

        ArbitrationCreateDTO dto = new ArbitrationCreateDTO();
        dto.setReason("  未按约定完成喂食  ");
        dto.setEvidenceUrls(List.of("/uploads/evidence/a.jpg", "/uploads/evidence/b.jpg"));
        service.submit(ORDER_ID, dto);

        verify(orderMapper).markArbitrating(ORDER_ID, OWNER_ID);
        ArgumentCaptor<Arbitration> captor = ArgumentCaptor.forClass(Arbitration.class);
        verify(arbitrationMapper).insert(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("未按约定完成喂食");
        assertThat(captor.getValue().getEvidence())
                .isEqualTo("/uploads/evidence/a.jpg,/uploads/evidence/b.jpg");
        assertThat(captor.getValue().getStatus()).isEqualTo(ArbitrationStatus.PENDING.getCode());
    }

    @Test
    @DisplayName("申诉通过：裁定、订单退款状态和钱包退款必须全部成功")
    void approvalRefundsFullFrozenAmount() {
        UserContext.set(new LoginUser(ADMIN_ID, "admin", "ADMIN"));
        pendingArbitration();
        Order order = pendingAcceptOrder();
        order.setStatus(OrderStatus.ARBITRATING.getCode());
        when(arbitrationMapper.markDecided(eq(ARBITRATION_ID), eq(ADMIN_ID), any(), eq(order.getAmount())))
                .thenReturn(1);
        when(orderMapper.markArbitrationRefunded(eq(ORDER_ID), any())).thenReturn(1);

        service.decide(ARBITRATION_ID, decision(true));

        verify(orderMapper).markArbitrationRefunded(eq(ORDER_ID), any());
        verify(walletService).refundOrder(ORDER_ID, OWNER_ID, new BigDecimal("35.00"),
                "平台仲裁通过，担保资金退回余额");
    }

    @Test
    @DisplayName("申诉驳回：订单回到待验收且不动钱包")
    void rejectionRestoresPendingAcceptanceWithoutRefund() {
        UserContext.set(new LoginUser(ADMIN_ID, "admin", "ADMIN"));
        pendingArbitration();
        Order order = pendingAcceptOrder();
        order.setStatus(OrderStatus.ARBITRATING.getCode());
        when(arbitrationMapper.markDecided(eq(ARBITRATION_ID), eq(ADMIN_ID), any(), eq(BigDecimal.ZERO)))
                .thenReturn(1);
        when(orderMapper.markArbitrationRejected(ORDER_ID)).thenReturn(1);

        service.decide(ARBITRATION_ID, decision(false));

        verify(orderMapper).markArbitrationRejected(ORDER_ID);
        verify(walletService, never()).refundOrder(any(), any(), any(), any());
    }

    @Test
    @DisplayName("重复裁定被条件更新挡住，不会重复退款")
    void duplicateDecisionCannotRefundTwice() {
        UserContext.set(new LoginUser(ADMIN_ID, "admin", "ADMIN"));
        pendingArbitration();
        Order order = pendingAcceptOrder();
        order.setStatus(OrderStatus.ARBITRATING.getCode());
        when(arbitrationMapper.markDecided(eq(ARBITRATION_ID), eq(ADMIN_ID), any(), eq(order.getAmount())))
                .thenReturn(0);

        assertThatThrownBy(() -> service.decide(ARBITRATION_ID, decision(true)))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.ARBITRATION_ALREADY_DECIDED.getCode());

        verify(orderMapper, never()).markArbitrationRefunded(any(), any());
        verify(walletService, never()).refundOrder(any(), any(), any(), any());
    }
}
