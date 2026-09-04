package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 计价预览出参。
 * <p>
 * 下单前给用户看总价，也是管理端改完 basePrice / commissionRate 后立刻验证联动效果的入口。
 * 计价口径与 OrderService 下单时完全一致（同一个方法算出来的），避免「预览 60 实付 61」。
 */
@Data
public class PricePreviewVO {

    private Long categoryId;
    private String categoryName;
    private LocalDateTime serviceStart;

    /** serviceStart 是否落在周六或周日，为真时按 holidayRate 溢价 */
    private boolean holiday;

    /** 用户实付 = basePrice × (holiday ? holidayRate : 1)，2 位小数四舍五入 */
    private BigDecimal amount;

    /** 平台抽成 = amount × commissionRate，2 位小数四舍五入 */
    private BigDecimal commission;

    /** 接单员到手 = amount − commission。用减法而不是各自四舍五入，保证三者精确守恒 */
    private BigDecimal sitterIncome;
}
