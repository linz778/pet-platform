package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单。
 * <p>
 * amount / commission / sitterIncome 是<b>下单时刻的价格快照</b>，
 * 管理端后续修改服务类别的单价与抽成比例不会回溯影响已存在的订单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order")
public class Order extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    /** 下单用户 */
    private Long userId;

    private Long petId;

    private Long categoryId;

    /** 接单员 user_id，未接单时为 null */
    private Long sitterId;

    private String serviceAddress;

    private BigDecimal addressLat;

    private BigDecimal addressLng;

    private LocalDateTime serviceStart;

    private LocalDateTime serviceEnd;

    private BigDecimal amount;

    private BigDecimal commission;

    private BigDecimal sitterIncome;

    /** 见 {@link com.pet.common.enums.OrderStatus} */
    private Integer status;

    /** 见 {@link com.pet.common.enums.PayStatus} */
    private Integer payStatus;

    private LocalDateTime payTime;

    private LocalDateTime takenTime;

    private LocalDateTime checkinTime;

    private LocalDateTime finishTime;

    private LocalDateTime acceptTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private String remark;
}
