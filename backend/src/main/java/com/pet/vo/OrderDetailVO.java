package com.pet.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单详情，同一份 VO 服务下单用户、接单员、管理员三种角色。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailVO extends OrderListVO {

    /** 服务地址坐标，前端回显地图标记用 */
    private BigDecimal addressLat;

    private BigDecimal addressLng;

    private String remark;

    /** 宠物档案快照的补充信息，档案已删除时依然能取到 */
    private String petSpecies;

    private String petBreed;

    private String petAvatar;

    /**
     * 平台抽成。<b>只对管理员与该单接单员赋值</b>，对下单用户保持 null。
     * <p>
     * 这不是漏赋值、不要「顺手补上」：全局 Jackson 配了 {@code default-property-inclusion: non_null}，
     * 保持 null 能让这个键在用户的响应里<b>彻底消失</b>，从根上避免把平台分成比例泄露给下单方
     * （用户看到 60 元里平台抽走 6 元、接单员拿 54 元，会直接影响议价与信任）。
     * <p>
     * 接单员到手金额 {@code sitterIncome} 同理，但它已上移到父类 {@link OrderListVO}——
     * 接单员的「我的接单」列表也要显示到手多少，留在子类会与父类字段形成遮蔽。
     */
    private BigDecimal commission;

    // ── 履约时间轴：未发生的节点为 null，键会消失，前端时间轴必须逐节点 v-if 守卫
    private LocalDateTime payTime;

    private LocalDateTime takenTime;

    private LocalDateTime checkinTime;

    private LocalDateTime finishTime;

    private LocalDateTime acceptTime;

    private LocalDateTime cancelTime;

    private String cancelReason;
}
