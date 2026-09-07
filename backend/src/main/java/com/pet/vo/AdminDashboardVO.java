package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminDashboardVO {

    private LocalDateTime generatedAt;
    private Integer periodDays;
    private Overview overview;
    private List<DailyTrend> dailyTrend;
    private List<CategoryMetric> categoryMetrics;
    private List<StatusMetric> statusMetrics;
    private List<RecentOrder> recentOrders;

    @Data
    public static class Overview {
        private Long ownerCount;
        private Long approvedSitterCount;
        private Long activeSitterCount;
        private Long pendingAuditCount;
        private Long pendingArbitrationCount;
        private Long totalOrderCount;
        private Long todayOrderCount;
        private BigDecimal settledAmount;
        private BigDecimal platformRevenue;
        private BigDecimal completionRate;
    }

    @Data
    public static class DailyTrend {
        private LocalDate statDate;
        private Long orderCount = 0L;
        private Long completedCount = 0L;
        private BigDecimal turnover;
    }

    @Data
    public static class CategoryMetric {
        private String categoryName;
        private Long orderCount;
        private BigDecimal settledAmount;
    }

    @Data
    public static class StatusMetric {
        private Integer status;
        private String statusText;
        private Long orderCount;
    }

    @Data
    public static class RecentOrder {
        private Long id;
        private String orderNo;
        private String categoryName;
        private String petName;
        private BigDecimal amount;
        private Integer status;
        private String statusText;
        private LocalDateTime createTime;
    }
}
