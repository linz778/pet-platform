package com.pet.common.enums;

import lombok.Getter;

/**
 * 履约存证类型（对应 t_order_evidence.type）。
 */
@Getter
public enum EvidenceType {

    /** 进门定位打卡，携带经纬度，写入时校验与服务地址的距离 */
    CHECK_IN(1, "进门定位打卡"),
    /** 作业清单逐项拍照，check_item 对应服务类别 checklist_template 中的一项 */
    CHECKLIST(2, "作业清单存证"),
    /** 散步轨迹，track_json 存轨迹点数组 */
    TRACK(3, "散步轨迹");

    private final int code;
    private final String desc;

    EvidenceType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descOf(Integer code) {
        if (code == null) {
            return "";
        }
        for (EvidenceType t : values()) {
            if (t.code == code) {
                return t.desc;
            }
        }
        return "";
    }
}
