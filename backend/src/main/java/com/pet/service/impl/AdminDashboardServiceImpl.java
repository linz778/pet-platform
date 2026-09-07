package com.pet.service.impl;

import com.pet.common.enums.OrderStatus;
import com.pet.mapper.AdminDashboardMapper;
import com.pet.service.AdminDashboardService;
import com.pet.vo.AdminDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AdminDashboardMapper dashboardMapper;

    @Override
    public AdminDashboardVO getDashboard(int requestedDays) {
        int days = requestedDays <= 7 ? 7 : requestedDays <= 14 ? 14 : 30;
        LocalDate startDate = LocalDate.now().minusDays(days - 1L);

        AdminDashboardVO vo = new AdminDashboardVO();
        vo.setGeneratedAt(LocalDateTime.now());
        vo.setPeriodDays(days);
        AdminDashboardVO.Overview overview = dashboardMapper.selectOverview();
        vo.setOverview(overview == null ? emptyOverview() : overview);

        Map<LocalDate, AdminDashboardVO.DailyTrend> trendByDate = dashboardMapper.selectDailyTrend(startDate)
                .stream().collect(Collectors.toMap(AdminDashboardVO.DailyTrend::getStatDate, Function.identity()));
        vo.setDailyTrend(IntStream.range(0, days)
                .mapToObj(startDate::plusDays)
                .map(date -> trendByDate.getOrDefault(date, emptyTrend(date)))
                .toList());

        vo.setCategoryMetrics(dashboardMapper.selectCategoryMetrics(startDate));
        List<AdminDashboardVO.StatusMetric> statuses = dashboardMapper.selectStatusMetrics();
        statuses.forEach(item -> item.setStatusText(OrderStatus.descOf(item.getStatus())));
        vo.setStatusMetrics(statuses);

        List<AdminDashboardVO.RecentOrder> recentOrders = dashboardMapper.selectRecentOrders();
        recentOrders.forEach(item -> item.setStatusText(OrderStatus.descOf(item.getStatus())));
        vo.setRecentOrders(recentOrders);
        return vo;
    }

    private AdminDashboardVO.Overview emptyOverview() {
        AdminDashboardVO.Overview overview = new AdminDashboardVO.Overview();
        overview.setSettledAmount(BigDecimal.ZERO);
        overview.setPlatformRevenue(BigDecimal.ZERO);
        overview.setCompletionRate(BigDecimal.ZERO);
        return overview;
    }

    private AdminDashboardVO.DailyTrend emptyTrend(LocalDate date) {
        AdminDashboardVO.DailyTrend trend = new AdminDashboardVO.DailyTrend();
        trend.setStatDate(date);
        trend.setTurnover(BigDecimal.ZERO);
        return trend;
    }
}
