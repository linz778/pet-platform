package com.pet.common.enums;

import lombok.Getter;

/**
 * 订单状态（对应 t_order.status）。
 * <p>
 * 合法流转：0→1（支付）→2（抢单/指派）→3（到达打卡）→4（完成服务）→5（用户验收）。
 * 0/1 可取消转 6；1 取消时触发全额退款。
 * <p>
 * {@link #ARBITRATING} 本期无任何代码路径会产生，仅为与表注释保持对齐而保留。
 */
@Getter
public enum OrderStatus {

    UNPAID(0, "待支付"),
    PENDING(1, "待接单"),
    TAKEN(2, "已接单"),
    IN_SERVICE(3, "服务中"),
    PENDING_ACCEPT(4, "待验收"),
    COMPLETED(5, "已完成"),
    CANCELLED(6, "已取消"),
    ARBITRATING(7, "仲裁中");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    public static String descOf(Integer code) {
        OrderStatus s = of(code);
        return s == null ? "" : s.desc;
    }

    /** 仅待支付与待接单允许取消；已接单后需走仲裁流程（本期未实现）。 */
    public boolean cancellable() {
        return this == UNPAID || this == PENDING;
    }
}
