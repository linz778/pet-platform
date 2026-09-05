package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.common.api.PageQuery;
import com.pet.common.api.PageResult;
import com.pet.entity.Wallet;
import com.pet.vo.WalletTransactionVO;
import com.pet.vo.WalletVO;

import java.math.BigDecimal;

public interface WalletService extends IService<Wallet> {

    /**
     * 为新注册用户开通钱包（余额/冻结/累计收入均为 0），返回已落库的实体。
     * <p>
     * 必须在注册事务内调用：t_wallet 是下单支付的前置依赖，缺行会导致支付时余额扣减影响行数为 0，
     * 表现为「余额不足」而非真正的缺钱包，很难排查。
     */
    Wallet initWallet(Long userId);

    /** 当前登录用户的钱包；查不到行时懒初始化后再返回，绝不抛「钱包不存在」。 */
    WalletVO getMine();

    /**
     * 当前登录用户的钱包流水，按时间倒序。
     *
     * @param type 流水类型过滤，为空表示全部
     */
    PageResult<WalletTransactionVO> pageMyTransactions(Integer type, PageQuery query);

    /** 充值（模拟，即时到账）。金额区间不合法抛 RECHARGE_AMOUNT_ILLEGAL。 */
    void recharge(BigDecimal amount);

    /**
     * 支付订单：下单用户的可用余额转入冻结，即资金进入平台担保。余额不足抛 BALANCE_NOT_ENOUGH。
     * <p>
     * 必须由调用方（OrderService#pay）在同一事务里先完成订单的条件更新并确认影响行数为 1，
     * 否则重复支付会给同一笔订单冻结两次。
     */
    void payOrder(Long orderId, Long userId, BigDecimal amount);

    /** 取消已支付订单：担保资金原路退回可用余额。 */
    void refundOrder(Long orderId, Long userId, BigDecimal amount);

    /** 提现（模拟，即时成功）。超出可用余额抛 WITHDRAW_AMOUNT_ILLEGAL；累计收入不因此减少。 */
    void withdraw(BigDecimal amount);
}
