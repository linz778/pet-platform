package com.pet.common.enums;

import lombok.Getter;

/**
 * 接单员资质审核状态（对应 t_sitter_profile.audit_status）。
 * <p>
 * 流转：0 待审 →（管理端）1 通过 / 2 驳回；驳回后接单员重新提交资料会退回 0 待审。
 * 只有 1 通过的接单员能出现在抢单流程里，见 {@code SitterProfileService#requireGrabable}。
 */
@Getter
public enum AuditStatus {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回");

    private final int code;
    private final String desc;

    AuditStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AuditStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (AuditStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    public static String descOf(Integer code) {
        AuditStatus s = of(code);
        return s == null ? "" : s.desc;
    }
}
