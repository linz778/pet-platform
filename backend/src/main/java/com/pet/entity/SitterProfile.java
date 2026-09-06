package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 接单员资质与信誉。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sitter_profile")
public class SitterProfile extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String realName;

    /** 身份证号；WRITE_ONLY 确保永不随响应体下发，对外一律输出脱敏值 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String idCard;

    private String idCardImg;

    private String healthCert;

    /** 训犬 / 美容等资质证书图片 */
    private String qualification;

    private Integer experienceYears;

    /** 0=待审 1=通过 2=驳回 */
    private Integer auditStatus;

    private String auditRemark;

    /** 信誉等级 1-5 */
    private Integer creditLevel;

    /** 信誉分 0-100，新接单员初始 100 分 */
    private Integer creditScore;

    private BigDecimal currentLat;

    private BigDecimal currentLng;

    /** 是否可接单 */
    private Integer available;
}
