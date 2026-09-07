package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 用户对服务结果提出的申诉及平台裁定。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_arbitration")
public class Arbitration extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long complainantId;

    private String reason;

    /** 最多 5 张证据图片 URL，逗号分隔存储。 */
    private String evidence;

    /** 0=待审核 1=处理中 2=已处理 */
    private Integer status;

    private String result;

    private BigDecimal refundAmount;

    private Long handlerId;
}
