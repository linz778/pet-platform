package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 用户服务地址簿；下单时把选中地址复制为订单快照。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_address")
public class UserAddress extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String label;

    private String province;

    private String city;

    private String district;

    private String detailAddress;

    private BigDecimal lng;

    private BigDecimal lat;

    /** 0=普通地址 1=默认服务地址 */
    private Integer defaultAddress;
}
