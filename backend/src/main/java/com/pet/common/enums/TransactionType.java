package com.pet.common.enums;

import lombok.Getter;

/**
 * 钱包流水类型（对应 t_wallet_transaction.type）。
 */
@Getter
public enum TransactionType {

    RECHARGE(1, "充值"),
    PAY(2, "支付"),
    COMMISSION_INCOME(3, "佣金入账"),
    WITHDRAW(4, "提现"),
    REFUND(5, "退款"),
    /** 验收结算时平台抽成入账到平台账户（t_wallet.user_id = 0） */
    PLATFORM_COMMISSION(6, "平台佣金");

    private final int code;
    private final String desc;

    TransactionType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descOf(Integer code) {
        if (code == null) {
            return "";
        }
        for (TransactionType t : values()) {
            if (t.code == code) {
                return t.desc;
            }
        }
        return "";
    }
}
