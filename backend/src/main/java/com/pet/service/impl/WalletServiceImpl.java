package com.pet.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.api.PageQuery;
import com.pet.common.api.PageResult;
import com.pet.common.api.ResultCode;
import com.pet.common.enums.TransactionType;
import com.pet.common.exception.BusinessException;
import com.pet.entity.Wallet;
import com.pet.entity.WalletTransaction;
import com.pet.mapper.WalletMapper;
import com.pet.mapper.WalletTransactionMapper;
import com.pet.security.UserContext;
import com.pet.service.WalletService;
import com.pet.vo.WalletTransactionVO;
import com.pet.vo.WalletVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl extends ServiceImpl<WalletMapper, Wallet> implements WalletService {

    private static final BigDecimal MIN_RECHARGE = new BigDecimal("1");
    private static final BigDecimal MAX_RECHARGE = new BigDecimal("10000");

    private final WalletTransactionMapper transactionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Wallet initWallet(Long userId) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setFrozen(BigDecimal.ZERO);
        wallet.setTotalIncome(BigDecimal.ZERO);
        save(wallet);
        return wallet;
    }

    /**
     * 取钱包，缺行就补。
     * <p>
     * 懒初始化而不是抛 WALLET_NOT_FOUND：种子数据里 admin(id=1) 就没有钱包行，
     * 任何绕过 register 的建号方式（直接插库、后续导入脚本）都会缺行。缺行时余额扣减的
     * 影响行数为 0，会被误判成「余额不足」，排查成本远高于这里补一行。
     */
    private Wallet requireWallet(Long userId) {
        // 直接走 baseMapper 而不是 ServiceImpl#getOne：getOne 在 IService/IRepository 之间层层转发，
        // 最终落到哪个 selectOne 重载由框架版本决定，单测里桩不好打；这里只有一个确定的拦截点。
        Wallet wallet = baseMapper.selectOne(Wrappers.<Wallet>lambdaQuery().eq(Wallet::getUserId, userId));
        return wallet != null ? wallet : initWallet(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletVO getMine() {
        Wallet wallet = requireWallet(UserContext.userId());
        WalletVO vo = new WalletVO();
        vo.setBalance(wallet.getBalance());
        vo.setFrozen(wallet.getFrozen());
        vo.setTotalIncome(wallet.getTotalIncome());
        return vo;
    }

    @Override
    public PageResult<WalletTransactionVO> pageMyTransactions(Integer type, PageQuery query) {
        Long userId = UserContext.userId();
        Page<WalletTransaction> page = transactionMapper.selectPage(query.toPage(),
                Wrappers.<WalletTransaction>lambdaQuery()
                        .eq(WalletTransaction::getUserId, userId)
                        .eq(type != null, WalletTransaction::getType, type)
                        .orderByDesc(WalletTransaction::getId));
        return PageResult.of(page, this::toTransactionVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(BigDecimal amount) {
        // 用 compareTo 而非 equals：BigDecimal 的 equals 连 scale 一起比，
        // new BigDecimal("1.0").equals(new BigDecimal("1")) 是 false
        if (amount.compareTo(MIN_RECHARGE) < 0 || amount.compareTo(MAX_RECHARGE) > 0) {
            throw new BusinessException(ResultCode.RECHARGE_AMOUNT_ILLEGAL);
        }
        Long userId = UserContext.userId();
        Wallet wallet = requireWallet(userId);
        if (baseMapper.recharge(userId, amount) == 0) {
            throw new BusinessException(ResultCode.WALLET_NOT_FOUND);
        }
        writeTransaction(wallet, TransactionType.RECHARGE, amount, null, "账户充值");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, Long userId, BigDecimal amount) {
        Wallet wallet = requireWallet(userId);
        // freeze 的 SQL 条件是 balance >= amount，钱包行又刚由 requireWallet 保证存在，
        // 所以影响行数为 0 只有余额不足这一种可能
        if (baseMapper.freeze(userId, amount) == 0) {
            throw new BusinessException(ResultCode.BALANCE_NOT_ENOUGH);
        }
        writeTransaction(wallet, TransactionType.PAY, amount.negate(), orderId, "支付订单，资金进入平台担保");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long orderId, Long userId, BigDecimal amount) {
        Wallet wallet = requireWallet(userId);
        // unfreezeToBalance 的条件是 frozen >= amount，返回 0 说明这笔担保资金已经退过或从未冻结，
        // 属于重复退款，必须挡住而不是把余额凭空加一遍
        if (baseMapper.unfreezeToBalance(userId, amount) == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
        }
        writeTransaction(wallet, TransactionType.REFUND, amount, orderId, "订单取消，担保资金退回余额");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.WITHDRAW_AMOUNT_ILLEGAL);
        }
        Long userId = UserContext.userId();
        Wallet wallet = requireWallet(userId);
        // withdraw 的 SQL 条件是 balance >= amount，返回 0 即余额不足
        if (baseMapper.withdraw(userId, amount) == 0) {
            throw new BusinessException(ResultCode.WITHDRAW_AMOUNT_ILLEGAL);
        }
        writeTransaction(wallet, TransactionType.WITHDRAW, amount.negate(), null, "提现（模拟，即时到账）");
    }

    /**
     * 记一笔流水。
     * <p>
     * balance_after 必须在余额 UPDATE <b>之后</b>读：同事务内 UPDATE 已对该行持有排他锁并保持到提交，
     * 所以这里读到的一定是本次变动后的值，不会被并发事务插队。
     */
    private void writeTransaction(Wallet wallet, TransactionType type, BigDecimal signedAmount,
                                  Long orderId, String remark) {
        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(wallet.getId());
        tx.setUserId(wallet.getUserId());
        tx.setType(type.getCode());
        tx.setAmount(signedAmount);
        tx.setOrderId(orderId);
        tx.setBalanceAfter(baseMapper.selectBalance(wallet.getUserId()));
        tx.setRemark(remark);
        transactionMapper.insert(tx);
    }

    private WalletTransactionVO toTransactionVO(WalletTransaction tx) {
        WalletTransactionVO vo = new WalletTransactionVO();
        vo.setId(tx.getId());
        vo.setType(tx.getType());
        vo.setTypeDesc(TransactionType.descOf(tx.getType()));
        vo.setAmount(tx.getAmount());
        vo.setOrderId(tx.getOrderId());
        vo.setBalanceAfter(tx.getBalanceAfter());
        vo.setRemark(tx.getRemark());
        vo.setCreateTime(tx.getCreateTime());
        return vo;
    }
}
