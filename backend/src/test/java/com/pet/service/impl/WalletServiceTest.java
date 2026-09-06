package com.pet.service.impl;

import com.pet.common.api.ResultCode;
import com.pet.common.enums.TransactionType;
import com.pet.common.exception.BusinessException;
import com.pet.entity.Wallet;
import com.pet.entity.WalletTransaction;
import com.pet.mapper.WalletMapper;
import com.pet.mapper.WalletTransactionMapper;
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import com.pet.vo.WalletVO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 锁死钱包资金流。
 * <p>
 * 这里的每一条都是资损防线：金额符号写反、余额不足没挡住、失败还记流水，
 * 任何一种都会在演示时表现为「钱凭空多了/少了」，且事后只能靠流水倒查。
 * 纯 Mockito，不启 Spring 上下文，不依赖本机 MySQL / Redis。
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final long USER_ID = 2L;
    private static final long SITTER_ID = 3L;
    private static final long ORDER_ID = 77L;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private WalletTransactionMapper transactionMapper;

    private WalletServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WalletServiceImpl(transactionMapper);
        // baseMapper 是 ServiceImpl 的父类字段，@InjectMocks 注不进去，只能反射塞
        ReflectionTestUtils.setField(service, "baseMapper", walletMapper);
        // UserContext 是静态 ThreadLocal，不用 mockStatic；但必须在 tearDown 里 clear，
        // 否则身份会泄漏到下一个测试类，表现为别的测试莫名其妙拿到了这里的 userId
        UserContext.set(new LoginUser(USER_ID, "user", "USER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    /** 造一个已存在的钱包行，让 requireWallet 走不到懒初始化分支 */
    private Wallet existingWallet(String balance, String frozen, String totalIncome) {
        Wallet wallet = new Wallet();
        wallet.setId(10L);
        wallet.setUserId(USER_ID);
        wallet.setBalance(new BigDecimal(balance));
        wallet.setFrozen(new BigDecimal(frozen));
        wallet.setTotalIncome(new BigDecimal(totalIncome));
        when(walletMapper.selectOne(any())).thenReturn(wallet);
        return wallet;
    }

    /** 造任意用户的钱包行，金额全零；结算测试要同时给接单员和平台账户各造一个。 */
    private Wallet walletOf(long userId, long id) {
        Wallet wallet = new Wallet();
        wallet.setId(id);
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setFrozen(BigDecimal.ZERO);
        wallet.setTotalIncome(BigDecimal.ZERO);
        return wallet;
    }

    private WalletTransaction capturedTransaction() {
        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionMapper).insert(captor.capture());
        return captor.getValue();
    }

    // ───────────────────────── 充值 ─────────────────────────

    @Test
    @DisplayName("充值成功：余额增加，流水 type=1 且金额为正")
    void rechargeWritesPositiveLedger() {
        existingWallet("0.00", "0.00", "0.00");
        when(walletMapper.recharge(USER_ID, new BigDecimal("1000.00"))).thenReturn(1);
        when(walletMapper.selectBalance(USER_ID)).thenReturn(new BigDecimal("1000.00"));

        service.recharge(new BigDecimal("1000.00"));

        WalletTransaction tx = capturedTransaction();
        assertThat(tx.getType()).isEqualTo(TransactionType.RECHARGE.getCode());
        assertThat(tx.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(tx.getBalanceAfter()).isEqualByComparingTo("1000.00");
        assertThat(tx.getWalletId()).isEqualTo(10L);
        assertThat(tx.getUserId()).isEqualTo(USER_ID);
        assertThat(tx.getOrderId()).isNull();
    }

    @Test
    @DisplayName("充值低于 1 元或高于 10000 元都拒绝，且不碰钱包不记流水")
    void rechargeRejectsOutOfRangeAmount() {
        assertThatThrownBy(() -> service.recharge(new BigDecimal("0.99")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.RECHARGE_AMOUNT_ILLEGAL.getCode()));
        assertThatThrownBy(() -> service.recharge(new BigDecimal("10000.01")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.RECHARGE_AMOUNT_ILLEGAL.getCode()));

        verifyNoInteractions(walletMapper, transactionMapper);
    }

    @Test
    @DisplayName("充值边界值 1 与 10000 都放行")
    void rechargeAcceptsBoundaryAmounts() {
        existingWallet("0.00", "0.00", "0.00");
        when(walletMapper.recharge(eq(USER_ID), any(BigDecimal.class))).thenReturn(1);
        when(walletMapper.selectBalance(USER_ID)).thenReturn(new BigDecimal("1.00"));

        service.recharge(new BigDecimal("1"));
        service.recharge(new BigDecimal("10000"));

        verify(walletMapper).recharge(USER_ID, new BigDecimal("1"));
        verify(walletMapper).recharge(USER_ID, new BigDecimal("10000"));
    }

    // ───────────────────────── 支付冻结 ─────────────────────────

    @Test
    @DisplayName("支付成功：余额转冻结，流水 type=2 且金额为负、带上订单号")
    void payOrderFreezesAndWritesNegativeLedger() {
        existingWallet("1000.00", "0.00", "0.00");
        when(walletMapper.freeze(USER_ID, new BigDecimal("60.00"))).thenReturn(1);
        when(walletMapper.selectBalance(USER_ID)).thenReturn(new BigDecimal("940.00"));

        service.payOrder(ORDER_ID, USER_ID, new BigDecimal("60.00"));

        WalletTransaction tx = capturedTransaction();
        assertThat(tx.getType()).isEqualTo(TransactionType.PAY.getCode());
        assertThat(tx.getAmount()).isEqualByComparingTo("-60.00");
        assertThat(tx.getBalanceAfter()).isEqualByComparingTo("940.00");
        assertThat(tx.getOrderId()).isEqualTo(ORDER_ID);
    }

    /**
     * 余额不足时必须先抛异常再记流水。反过来的话，扣款失败却留下一条 type=2 的流水，
     * 对账时看起来像钱已经扣了，是演示现场最难解释的一种不一致。
     */
    @Test
    @DisplayName("余额不足抛 BALANCE_NOT_ENOUGH，且绝不记流水")
    void payOrderRejectsInsufficientBalance() {
        existingWallet("10.00", "0.00", "0.00");
        when(walletMapper.freeze(USER_ID, new BigDecimal("60.00"))).thenReturn(0);

        assertThatThrownBy(() -> service.payOrder(ORDER_ID, USER_ID, new BigDecimal("60.00")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.BALANCE_NOT_ENOUGH.getCode()));

        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
    }

    // ───────────────────────── 退款 ─────────────────────────

    @Test
    @DisplayName("取消退款：冻结退回余额，流水 type=5 且金额为正")
    void refundUnfreezesToBalance() {
        existingWallet("940.00", "60.00", "0.00");
        when(walletMapper.unfreezeToBalance(USER_ID, new BigDecimal("60.00"))).thenReturn(1);
        when(walletMapper.selectBalance(USER_ID)).thenReturn(new BigDecimal("1000.00"));

        service.refundOrder(ORDER_ID, USER_ID, new BigDecimal("60.00"));

        WalletTransaction tx = capturedTransaction();
        assertThat(tx.getType()).isEqualTo(TransactionType.REFUND.getCode());
        assertThat(tx.getAmount()).isEqualByComparingTo("60.00");
        assertThat(tx.getOrderId()).isEqualTo(ORDER_ID);
    }

    @Test
    @DisplayName("冻结额不足说明已退过，抛异常且不重复记流水")
    void refundRejectsDoubleRefund() {
        existingWallet("1000.00", "0.00", "0.00");
        when(walletMapper.unfreezeToBalance(USER_ID, new BigDecimal("60.00"))).thenReturn(0);

        assertThatThrownBy(() -> service.refundOrder(ORDER_ID, USER_ID, new BigDecimal("60.00")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_STATUS_ILLEGAL.getCode()));

        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
    }

    // ───────────────────────── 验收结算 ─────────────────────────

    /**
     * 结算的三个动作必须一次到位：担保资金从下单用户冻结额划出、接单员到手入账、平台抽成入账。
     * 少任何一步都表现为「钱消失了」或「平台白干」，且只能靠流水倒查。
     */
    @Test
    @DisplayName("验收结算：冻结划出到手+抽成，接单员与平台各入账一笔，下单用户不再记流水")
    void settleSplitsEscrowBetweenSitterAndPlatform() {
        Wallet sitterWallet = walletOf(SITTER_ID, 11L);
        Wallet platformWallet = walletOf(Wallet.PLATFORM_USER_ID, 12L);
        // settleOrder 只为两个收款方取钱包（下单用户那边只有 deductFrozen 这一条原子 UPDATE），
        // 两次 selectOne 的返回按调用顺序对应：先接单员，后平台账户
        when(walletMapper.selectOne(any())).thenReturn(sitterWallet, platformWallet);
        when(walletMapper.deductFrozen(USER_ID, new BigDecimal("60.00"))).thenReturn(1);
        when(walletMapper.selectBalance(SITTER_ID)).thenReturn(new BigDecimal("54.00"));
        when(walletMapper.selectBalance(Wallet.PLATFORM_USER_ID)).thenReturn(new BigDecimal("6.00"));

        service.settleOrder(ORDER_ID, USER_ID, SITTER_ID, new BigDecimal("54.00"), new BigDecimal("6.00"));

        // 划出的必须恰好是两者之和：多划一分用户亏，少划一分平台垫
        verify(walletMapper).deductFrozen(USER_ID, new BigDecimal("60.00"));
        verify(walletMapper).addIncome(SITTER_ID, new BigDecimal("54.00"));
        verify(walletMapper).addIncome(Wallet.PLATFORM_USER_ID, new BigDecimal("6.00"));

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionMapper, times(2)).insert(captor.capture());
        WalletTransaction income = captor.getAllValues().get(0);
        assertThat(income.getType()).isEqualTo(TransactionType.COMMISSION_INCOME.getCode());
        assertThat(income.getAmount()).isEqualByComparingTo("54.00");
        assertThat(income.getWalletId()).isEqualTo(11L);
        assertThat(income.getUserId()).isEqualTo(SITTER_ID);
        assertThat(income.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(income.getBalanceAfter()).isEqualByComparingTo("54.00");

        WalletTransaction commission = captor.getAllValues().get(1);
        assertThat(commission.getType()).isEqualTo(TransactionType.PLATFORM_COMMISSION.getCode());
        assertThat(commission.getAmount()).isEqualByComparingTo("6.00");
        assertThat(commission.getWalletId()).isEqualTo(12L);
        assertThat(commission.getUserId()).isEqualTo(Wallet.PLATFORM_USER_ID);
        assertThat(commission.getOrderId()).isEqualTo(ORDER_ID);

        // 支付时那笔 type=2 已经记录了资金流出，验收只是释放担保，
        // 再给用户记一笔会让他以为自己被扣了两次
        assertThat(captor.getAllValues()).extracting(WalletTransaction::getUserId).doesNotContain(USER_ID);
        verify(walletMapper, never()).addIncome(eq(USER_ID), any());
    }

    /**
     * 冻结额不足只有「已经结算过 / 已被退款」两种可能。放过去就是凭空给两边各加一遍钱，
     * 这是全项目最贵的一条断言。
     */
    @Test
    @DisplayName("重复结算被 deductFrozen 挡住：一分钱不入账，一条流水都不记")
    void settleRejectsDoubleSettlement() {
        when(walletMapper.deductFrozen(USER_ID, new BigDecimal("60.00"))).thenReturn(0);

        assertThatThrownBy(() -> service.settleOrder(ORDER_ID, USER_ID, SITTER_ID,
                new BigDecimal("54.00"), new BigDecimal("6.00")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ORDER_STATUS_ILLEGAL.getCode()));

        verify(walletMapper, never()).addIncome(any(), any());
        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
    }

    /**
     * 平台佣金账户（user_id = 0）是纯约定 id，从没走过 register，种子脚本没跑或被清库时就缺行。
     * 缺行时 addIncome 影响行数为 0：钱从用户冻结额里划出去了，却没人收到。
     */
    @Test
    @DisplayName("收款方缺钱包行时懒初始化，平台账户照样能收到抽成")
    void settleLazilyCreatesMissingPayeeWallet() {
        when(walletMapper.selectOne(any())).thenReturn(walletOf(SITTER_ID, 11L), null);
        when(walletMapper.deductFrozen(USER_ID, new BigDecimal("60.00"))).thenReturn(1);
        when(walletMapper.selectBalance(SITTER_ID)).thenReturn(new BigDecimal("54.00"));
        when(walletMapper.selectBalance(Wallet.PLATFORM_USER_ID)).thenReturn(new BigDecimal("6.00"));

        service.settleOrder(ORDER_ID, USER_ID, SITTER_ID, new BigDecimal("54.00"), new BigDecimal("6.00"));

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(Wallet.PLATFORM_USER_ID);
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo("0");

        verify(walletMapper).addIncome(Wallet.PLATFORM_USER_ID, new BigDecimal("6.00"));
        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionMapper, times(2)).insert(txCaptor.capture());
        assertThat(txCaptor.getAllValues().get(1).getUserId()).isEqualTo(Wallet.PLATFORM_USER_ID);
    }

    // ───────────────────────── 提现 ─────────────────────────

    @Test
    @DisplayName("提现成功：余额减少，流水 type=4 且金额为负")
    void withdrawWritesNegativeLedger() {
        existingWallet("54.00", "0.00", "54.00");
        when(walletMapper.withdraw(USER_ID, new BigDecimal("20.00"))).thenReturn(1);
        when(walletMapper.selectBalance(USER_ID)).thenReturn(new BigDecimal("34.00"));

        service.withdraw(new BigDecimal("20.00"));

        WalletTransaction tx = capturedTransaction();
        assertThat(tx.getType()).isEqualTo(TransactionType.WITHDRAW.getCode());
        assertThat(tx.getAmount()).isEqualByComparingTo("-20.00");
        assertThat(tx.getOrderId()).isNull();
    }

    @Test
    @DisplayName("提现只能动 withdraw，不能误调 freeze/deductFrozen 把累计收入也改掉")
    void withdrawOnlyTouchesAvailableBalance() {
        existingWallet("54.00", "0.00", "54.00");
        when(walletMapper.withdraw(USER_ID, new BigDecimal("20.00"))).thenReturn(1);
        when(walletMapper.selectBalance(USER_ID)).thenReturn(new BigDecimal("34.00"));

        service.withdraw(new BigDecimal("20.00"));

        verify(walletMapper).withdraw(USER_ID, new BigDecimal("20.00"));
        verify(walletMapper, never()).freeze(any(), any());
        verify(walletMapper, never()).deductFrozen(any(), any());
        verify(walletMapper, never()).addIncome(any(), any());
    }

    @Test
    @DisplayName("超额提现抛 WITHDRAW_AMOUNT_ILLEGAL，且不记流水")
    void withdrawRejectsAmountOverBalance() {
        existingWallet("54.00", "0.00", "54.00");
        when(walletMapper.withdraw(USER_ID, new BigDecimal("54.01"))).thenReturn(0);

        assertThatThrownBy(() -> service.withdraw(new BigDecimal("54.01")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.WITHDRAW_AMOUNT_ILLEGAL.getCode()));

        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("零元与负数提现直接拒绝，连钱包都不必读")
    void withdrawRejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.withdraw(BigDecimal.ZERO))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.WITHDRAW_AMOUNT_ILLEGAL.getCode()));
        assertThatThrownBy(() -> service.withdraw(new BigDecimal("-20.00")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.WITHDRAW_AMOUNT_ILLEGAL.getCode()));

        verifyNoInteractions(walletMapper, transactionMapper);
    }

    // ───────────────────────── 查询与懒初始化 ─────────────────────────

    @Test
    @DisplayName("钱包已存在时按行内数据返回")
    void getMineReturnsExistingWallet() {
        existingWallet("940.00", "60.00", "54.00");

        WalletVO vo = service.getMine();

        assertThat(vo.getBalance()).isEqualByComparingTo("940.00");
        assertThat(vo.getFrozen()).isEqualByComparingTo("60.00");
        assertThat(vo.getTotalIncome()).isEqualByComparingTo("54.00");
        verify(walletMapper, never()).insert(any(Wallet.class));
    }

    /**
     * 种子数据里 admin(id=1) 就没有钱包行。缺行时若抛「钱包不存在」，管理员一点钱包页就报错；
     * 更隐蔽的是余额扣减影响行数为 0 会被误判成「余额不足」，排查方向直接跑偏。
     */
    @Test
    @DisplayName("钱包缺行时懒初始化一个全零钱包，而不是抛异常")
    void getMineLazilyInitializesMissingWallet() {
        when(walletMapper.selectOne(any())).thenReturn(null);

        WalletVO vo = service.getMine();

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo("0");
        assertThat(captor.getValue().getFrozen()).isEqualByComparingTo("0");
        assertThat(captor.getValue().getTotalIncome()).isEqualByComparingTo("0");

        assertThat(vo.getBalance()).isEqualByComparingTo("0");
    }

    /** 平台账户的 user_id 约定值一旦被改动，结算入账就会打到一个不存在的账户上 */
    @Test
    @DisplayName("平台账户 user_id 约定为 0")
    void platformAccountIdConvention() {
        assertThat(Wallet.PLATFORM_USER_ID).isZero();
    }
}
