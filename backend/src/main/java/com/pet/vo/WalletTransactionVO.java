package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包流水出参。
 */
@Data
public class WalletTransactionVO {

    private Long id;

    /** 见 {@link com.pet.common.enums.TransactionType} */
    private Integer type;

    /** 类型中文，由后端枚举给出，前端不再维护一份映射以免两边漂移 */
    private String typeDesc;

    /** 正负表示收支：充值/退款/佣金入账为正，支付/提现为负 */
    private BigDecimal amount;

    /** 关联订单，充值与提现为 null（non_null 下该键会消失，前端需判空） */
    private Long orderId;

    /** 变动后的可用余额，用于逐笔对账 */
    private BigDecimal balanceAfter;

    private String remark;

    private LocalDateTime createTime;
}
