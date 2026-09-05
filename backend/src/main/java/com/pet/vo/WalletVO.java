package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 钱包出参。
 */
@Data
public class WalletVO {

    /** 可用余额：充值累积、支付时扣减、提现时扣减 */
    private BigDecimal balance;

    /** 冻结金额：已支付但订单尚未验收结算，即平台担保中的资金 */
    private BigDecimal frozen;

    /** 累计收入：接单员佣金与平台抽成的历史总和，提现不减少此值 */
    private BigDecimal totalIncome;
}
