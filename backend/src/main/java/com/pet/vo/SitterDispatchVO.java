package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 管理员人工派单时的接单员候选项。 */
@Data
public class SitterDispatchVO {
    private Long userId;
    private String displayName;
    private String realName;
    private Integer creditLevel;
    private Integer creditScore;
    /** 接单员保存位置到订单服务地址的直线距离；未保存位置时为空。 */
    private BigDecimal distanceKm;
}
