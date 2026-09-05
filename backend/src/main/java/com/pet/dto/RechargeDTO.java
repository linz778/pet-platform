package com.pet.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 充值入参。
 * <p>
 * 这里只校验金额「形状」，1-10000 的业务区间由 WalletService 判定并抛 RECHARGE_AMOUNT_ILLEGAL：
 * 金额规则属于平台策略而非格式，集中在钱包服务一处，改限额时不用同时翻 DTO。
 */
@Data
public class RechargeDTO {

    @NotNull(message = "充值金额不能为空")
    @Digits(integer = 8, fraction = 2, message = "金额最多 2 位小数")
    private BigDecimal amount;
}
