package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 钱包流水。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_wallet_transaction")
public class WalletTransaction extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long walletId;

    private Long userId;

    /** 见 {@link com.pet.common.enums.TransactionType} */
    private Integer type;

    /** 金额，正负表示收支 */
    private BigDecimal amount;

    private Long orderId;

    /** 变动后余额，便于对账 */
    private BigDecimal balanceAfter;

    private String remark;
}
