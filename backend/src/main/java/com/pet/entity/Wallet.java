package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 钱包。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_wallet")
public class Wallet extends BaseEntity {

    /** 平台佣金账户的 user_id 约定值 */
    public static final long PLATFORM_USER_ID = 0L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 可用余额 */
    private BigDecimal balance;

    /** 冻结金额：用户已支付但订单尚未验收结算，即平台担保中的资金 */
    private BigDecimal frozen;

    /** 累计收入，提现不减少此值 */
    private BigDecimal totalIncome;
}
