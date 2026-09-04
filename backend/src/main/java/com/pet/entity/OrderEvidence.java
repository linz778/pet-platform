package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 履约存证（定位打卡 / 作业清单拍照 / 散步轨迹）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_evidence")
public class OrderEvidence extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 上传接单员 user_id */
    private Long sitterId;

    /** 见 {@link com.pet.common.enums.EvidenceType} */
    private Integer type;

    /** 清单项，如「换粮」「铲砂」；仅 type=2 时有值 */
    private String checkItem;

    private String imageUrl;

    private BigDecimal lat;

    private BigDecimal lng;

    /** 散步轨迹点 JSON 数组；仅 type=3 时有值 */
    private String trackJson;

    private String remark;
}
