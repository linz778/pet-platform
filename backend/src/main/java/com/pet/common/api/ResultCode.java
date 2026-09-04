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
    SITTER_PROFILE_NOT_FOUND(1004, "接单员资质信息不存在"),
    SITTER_NOT_AUDITED(1005, "资质未通过审核，暂不能接单"),
    SITTER_NOT_AVAILABLE(1006, "当前处于不可接单状态"),
    SITTER_ALREADY_AUDITED(1007, "资质已通过审核，如需修改请联系平台"),

    // 订单 / 抢单 / 履约 2xxx
    ORDER_NOT_FOUND(2001, "订单不存在"),
    ORDER_ALREADY_TAKEN(2002, "订单已被抢，请勿重复操作"),
    ORDER_STATUS_ILLEGAL(2003, "订单状态不允许该操作"),
    GEO_CHECK_IN_FAILED(2004, "定位打卡失败：不在服务地址允许范围内"),
    ORDER_ACCESS_DENIED(2005, "无权操作该订单"),
    ORDER_ALREADY_PAID(2006, "订单已支付，请勿重复操作"),
    SERVICE_TIME_ILLEGAL(2007, "预约开始时间必须晚于当前时间"),
    EVIDENCE_REQUIRED(2008, "请先提交作业清单存证，再标记服务完成"),
    REVIEW_ALREADY_EXISTS(2009, "该订单您已评价过"),
    PET_NOT_FOUND(2010, "宠物档案不存在"),
    CATEGORY_OFF_SHELF(2011, "该服务已下架"),

    // 资金 3xxx
    WALLET_NOT_FOUND(3001, "钱包不存在"),
    BALANCE_NOT_ENOUGH(3002, "钱包余额不足，请先充值"),
    RECHARGE_AMOUNT_ILLEGAL(3003, "充值金额必须在 1-10000 之间"),
    WITHDRAW_AMOUNT_ILLEGAL(3004, "提现金额不能超过可用余额"),

    // 文件 4xxx
    FILE_EMPTY(4001, "上传文件不能为空"),
    FILE_TYPE_NOT_ALLOWED(4002, "仅支持 jpg/jpeg/png/webp 格式的图片"),
    FILE_SIZE_EXCEEDED(4003, "文件大小超出限制"),
    FILE_UPLOAD_FAILED(4004, "文件上传失败，请稍后重试");

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
