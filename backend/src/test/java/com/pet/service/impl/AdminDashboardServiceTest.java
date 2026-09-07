package com.pet.service.impl;

import com.pet.mapper.AdminDashboardMapper;
import com.pet.vo.AdminDashboardVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock private AdminDashboardMapper mapper;

    @Test
    void fillsMissingDatesAndTranslatesOrderStatus() {
        AdminDashboardVO.DailyTrend today = new AdminDashboardVO.DailyTrend();
        today.setStatDate(LocalDate.now());
        today.setOrderCount(3L);
        today.setCompletedCount(1L);
        today.setTurnover(new BigDecimal("35.00"));

        AdminDashboardVO.StatusMetric status = new AdminDashboardVO.StatusMetric();
        status.setStatus(5);
        status.setOrderCount(2L);
        AdminDashboardVO.RecentOrder order = new AdminDashboardVO.RecentOrder();
        order.setStatus(2);

        when(mapper.selectOverview()).thenReturn(new AdminDashboardVO.Overview());
        when(mapper.selectDailyTrend(any())).thenReturn(List.of(today));
        when(mapper.selectCategoryMetrics(any())).thenReturn(new ArrayList<>());
        when(mapper.selectStatusMetrics()).thenReturn(new ArrayList<>(List.of(status)));
        when(mapper.selectRecentOrders()).thenReturn(new ArrayList<>(List.of(order)));

        AdminDashboardVO result = new AdminDashboardServiceImpl(mapper).getDashboard(7);

        assertThat(result.getDailyTrend()).hasSize(7);
        assertThat(result.getDailyTrend().getFirst().getOrderCount()).isZero();
        assertThat(result.getDailyTrend().getLast().getOrderCount()).isEqualTo(3L);
        assertThat(result.getStatusMetrics().getFirst().getStatusText()).isEqualTo("已完成");
        assertThat(result.getRecentOrders().getFirst().getStatusText()).isEqualTo("已接单");
    }

    @Test
    void normalizesPeriodToSupportedRanges() {
        when(mapper.selectOverview()).thenReturn(null);
        when(mapper.selectDailyTrend(any())).thenReturn(List.of());
        when(mapper.selectCategoryMetrics(any())).thenReturn(List.of());
        when(mapper.selectStatusMetrics()).thenReturn(new ArrayList<>());
        when(mapper.selectRecentOrders()).thenReturn(new ArrayList<>());

        AdminDashboardVO result = new AdminDashboardServiceImpl(mapper).getDashboard(21);

        assertThat(result.getPeriodDays()).isEqualTo(30);
        assertThat(result.getDailyTrend()).hasSize(30);
        assertThat(result.getOverview().getPlatformRevenue()).isZero();
    }
}
