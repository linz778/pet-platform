package com.pet.common.api;

/**
 * 业务状态码。
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),

    // 业务相关 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    ACCOUNT_OR_PWD_ERROR(1002, "账号或密码错误"),
    ACCOUNT_DISABLED(1003, "账号已被禁用"),

    // 订单 / 抢单 2xxx
    ORDER_NOT_FOUND(2001, "订单不存在"),
    ORDER_ALREADY_TAKEN(2002, "订单已被抢，请勿重复操作"),
    ORDER_STATUS_ILLEGAL(2003, "订单状态不允许该操作"),
    GEO_CHECK_IN_FAILED(2004, "定位打卡失败：不在服务地址允许范围内");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
