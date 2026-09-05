package com.pet.common.enums;

import lombok.Getter;

/**
 * 订单支付状态（对应 t_order.pay_status）。
 */
@Getter
public enum PayStatus {

    UNPAID(0, "未支付"),
    PAID(1, "已支付(平台担保)"),
    SETTLED(2, "已结算"),
    REFUNDED(3, "已退款");

    private final int code;
    private final String desc;

    PayStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PayStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (PayStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    public static String descOf(Integer code) {
        PayStatus s = of(code);
        return s == null ? "" : s.desc;
    }
}
