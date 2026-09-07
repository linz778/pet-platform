package com.pet.common.enums;

/** 用户服务申诉的审核状态。 */
public enum ArbitrationStatus {

    PENDING(0, "待审核"),
    PROCESSING(1, "处理中"),
    DECIDED(2, "已处理");

    private final int code;
    private final String desc;

    ArbitrationStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public static String descOf(Integer code) {
        if (code != null) {
            for (ArbitrationStatus status : values()) {
                if (status.code == code) {
                    return status.desc;
                }
            }
        }
        return "未知状态";
    }
}
