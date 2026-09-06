package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 接单员地址簿。经纬度由地图选点生成，仅用于附近订单检索。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sitter_address")
public class SitterAddress extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sitterId;

    private String label;

    private String province;

    private String city;

    private String district;

    private String detailAddress;

    private BigDecimal lng;

    private BigDecimal lat;

    /** 0=普通地址 1=默认搜索地址 */
    private Integer defaultAddress;
}
