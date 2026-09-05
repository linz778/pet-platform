package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 接单员资质档案（接单员自己看的那一份）。
 * <p>
 * 身份证号只给脱敏值：实体上的 {@code idCard} 标了 {@code WRITE_ONLY}，
 * 但直接返回实体仍会在管理端等其他出口漏出去，所以对外统一走这个 VO。
 */
@Data
public class SitterProfileVO {

    private Long userId;

    private String realName;

    /** 脱敏后的身份证号，例如 310101********1234；未填写时为 null（non_null 下该键消失） */
    private String idCardMasked;

    /** 是否已填写身份证号，前端据此决定表单里显示「已填写」还是必填校验 */
    private boolean idCardFilled;

    private String idCardImg;

    private String healthCert;

    private String qualification;

    private Integer experienceYears;

    /** 见 {@link com.pet.common.enums.AuditStatus} */
    private Integer auditStatus;

    private String auditStatusText;

    /** 驳回原因，前端在资质被驳回时展示给接单员 */
    private String auditRemark;

    /** 信誉等级 1-5 */
    private Integer creditLevel;

    /** 0=暂停接单 1=可接单 */
    private Integer available;

    /**
     * 建档时记录的坐标。本期没有位置上报链路，所以它不是实时位置，
     * 前端只在浏览器定位被拒时拿它当大厅检索的兜底圆心（种子接单员的坐标与演示订单一致）。
     */
    private BigDecimal currentLng;

    private BigDecimal currentLat;
}
