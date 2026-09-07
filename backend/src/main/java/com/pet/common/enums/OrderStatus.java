package com.pet.common.enums;

import lombok.Getter;

/**
 * 订单状态（对应 t_order.status）。
 * <p>
 * 合法流转：0→1（支付）→2（抢单/指派）→3（到达打卡）→4（完成服务）→5（用户验收）。
 * 0/1 可取消转 6；1 取消时触发全额退款。待验收订单可由用户申诉从 4→7；平台驳回后
 * 7→4，平台支持申诉并退款后 7→6。
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

    /** 仅待支付与待接单允许普通取消；完成服务后有异议走申诉仲裁。 */
    public boolean cancellable() {
        return this == UNPAID || this == PENDING;
    }
}
