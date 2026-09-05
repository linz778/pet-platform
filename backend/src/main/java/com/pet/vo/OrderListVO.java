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

    private String serviceAddress;

    private LocalDateTime serviceStart;

    private LocalDateTime serviceEnd;

    /** 下单时刻的价格快照，管理端后续改价不回溯 */
    private BigDecimal amount;

    /** 见 {@link com.pet.common.enums.OrderStatus} */
    private Integer status;

    private String statusText;

    /** 见 {@link com.pet.common.enums.PayStatus} */
    private Integer payStatus;

    private String payStatusText;

    private LocalDateTime createTime;
}
