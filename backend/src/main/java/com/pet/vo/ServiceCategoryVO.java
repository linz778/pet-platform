package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务类别出参：用户端选购目录与管理端规则配置共用。
 */
@Data
public class ServiceCategoryVO {

    private Long id;
    private String name;

    /** 服务编码：FEEDING / GROOMING / WALKING / COMPANION */
    private String code;

    private BigDecimal basePrice;

    /** 计价单位。本期仅作展示，不参与计价——t_order 没有数量或时长字段，固定「一单一服务一次」 */
    private String unit;

    /** 节假日（周六/周日）溢价倍数 */
    private BigDecimal holidayRate;

    /** 平台抽成比例，小数形式，0.120 表示 12% */
    private BigDecimal commissionRate;

    /** 标准作业清单，接单员上门时按此逐项拍照存证 */
    private List<String> checklist = new ArrayList<>();

    /** 0=下架 1=上架 */
    private Integer status;
}
