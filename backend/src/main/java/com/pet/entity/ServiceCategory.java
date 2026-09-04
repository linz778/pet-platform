package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 服务类别与计价规则。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_service_category")
public class ServiceCategory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 服务编码：FEEDING / GROOMING / WALKING / COMPANION */
    private String code;

    private BigDecimal basePrice;

    /**
     * 计价单位：次 / 小时。
     * <p>
     * 本期仅作展示，不参与计价——t_order 没有数量或时长字段，计价模型固定为「一单一服务一次」。
     */
    private String unit;

    /** 节假日（周六/周日）溢价倍数 */
    private BigDecimal holidayRate;

    /** 平台抽成比例，小数形式，0.120 表示 12% */
    private BigDecimal commissionRate;

    /** 标准作业清单模板，逗号分隔，如「换粮,添水,铲砂」 */
    private String checklistTemplate;

    /** 0=下架 1=上架 */
    private Integer status;
}
