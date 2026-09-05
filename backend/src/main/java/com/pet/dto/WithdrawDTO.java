package com.pet.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 提现入参。是否超过可用余额由 WalletService 判定并抛 WITHDRAW_AMOUNT_ILLEGAL。
 */
@Data
public class WithdrawDTO {

    @NotNull(message = "提现金额不能为空")
    @Digits(integer = 8, fraction = 2, message = "金额最多 2 位小数")
    private BigDecimal amount;
}
