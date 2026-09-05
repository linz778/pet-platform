package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表项。
 */
@Data
public class OrderListVO {

    private Long id;

    private String orderNo;

    private Long categoryId;

    private String categoryName;

    /** 计价单位（次/小时），本期仅展示 */
    private String unit;

    private Long petId;

    /**
     * 服务宠物昵称。
     * <p>
     * t_order 只存 pet_id、没有宠物名快照，因此这里回查宠物档案拼装。
     * 查询<b>刻意绕过逻辑删除</b>：主人删掉宠物后，历史订单仍应能说清当时服务的是谁。
     */
    private String petName;

    /** 宠物档案已被主人删除，前端据此加一个「档案已删除」标记 */
    private boolean petDeleted;

    /**
     * 下单用户昵称。<b>只在接单员的「我的接单」列表里赋值</b>，用户自己的列表保持 null。
     * <p>
     * 接单员要知道自己在给谁服务；用户看自己的列表时这个字段就是他本人，填了纯属噪音，
     * non_null 下保持 null 这个键就不会出现在响应里。
     */
    private String ownerNickname;

    private String serviceAddress;

    private LocalDateTime serviceStart;

    private LocalDateTime serviceEnd;

    /** 下单时刻的价格快照，管理端后续改价不回溯 */
    private BigDecimal amount;

    /**
     * 接单员到手金额。<b>只在接单员视角赋值</b>——「我的接单」列表与详情对本人 / 管理员可见，
     * 下单用户自己的列表与详情保持 null。
     * <p>
     * 理由与 {@link OrderDetailVO#getCommission()} 一致：全局 Jackson 配了 non_null，
     * 保持 null 这个键就会彻底消失，从根上避免让下单方看到「60 元里平台抽 6 元、接单员拿 54 元」。
     */
    private BigDecimal sitterIncome;

    /** 见 {@link com.pet.common.enums.OrderStatus} */
    private Integer status;

    private String statusText;

    /** 见 {@link com.pet.common.enums.PayStatus} */
    private Integer payStatus;

    private String payStatusText;

    private LocalDateTime createTime;
}
