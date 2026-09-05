package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 接单大厅列表项。
 * <p>
 * 刻意<b>不包含</b>下单用户的昵称、手机号与订单备注：大厅是公开抢单池，
 * 抢单成功之前接单员只需要知道「什么服务、什么时间、离我多远、能挣多少」，
 * 联系方式与门禁备注属于履约信息，抢到单之后从订单详情里取。
 */
@Data
public class HallOrderVO {

    private Long id;

    private String orderNo;

    private Long categoryId;

    /** 类别编码（FEEDING / GROOMING / WALKING / COMPANION），前端据此挑图标，名称是可以在后台改的 */
    private String categoryCode;

    private String categoryName;

    /** 计价单位（次/小时） */
    private String unit;

    private String petName;

    /** 物种，帮接单员判断自己能不能应付（例如不会处理爬宠） */
    private String petSpecies;

    private String serviceAddress;

    /** 服务地址坐标，前端地图打点用；顺序是经度在前 */
    private BigDecimal addressLng;

    private BigDecimal addressLat;

    private LocalDateTime serviceStart;

    private LocalDateTime serviceEnd;

    /** 用户支付的订单总额 */
    private BigDecimal amount;

    /** 接单员验收通过后的到手金额，抢单决策看的是这个数而不是 amount */
    private BigDecimal sitterIncome;

    /** 与接单员当前位置的距离（公里），保留两位小数 */
    private BigDecimal distanceKm;
}
