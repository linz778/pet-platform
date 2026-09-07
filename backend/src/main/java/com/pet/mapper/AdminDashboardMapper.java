package com.pet.mapper;

import com.pet.vo.AdminDashboardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AdminDashboardMapper {

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM t_user WHERE role = 'USER' AND deleted = 0) AS owner_count,
              (SELECT COUNT(*) FROM t_sitter_profile WHERE audit_status = 1 AND deleted = 0) AS approved_sitter_count,
              (SELECT COUNT(*) FROM t_sitter_profile WHERE audit_status = 1 AND available = 1 AND deleted = 0) AS active_sitter_count,
              (SELECT COUNT(*) FROM t_sitter_profile WHERE audit_status = 0 AND deleted = 0) AS pending_audit_count,
              (SELECT COUNT(*) FROM t_arbitration WHERE status = 0 AND deleted = 0) AS pending_arbitration_count,
              COUNT(*) AS total_order_count,
              COALESCE(SUM(CASE WHEN DATE(create_time) = CURDATE() THEN 1 ELSE 0 END), 0) AS today_order_count,
              COALESCE(SUM(CASE WHEN status = 5 THEN amount ELSE 0 END), 0) AS settled_amount,
              COALESCE(SUM(CASE WHEN status = 5 THEN commission ELSE 0 END), 0) AS platform_revenue,
              CASE WHEN COUNT(*) = 0 THEN 0
                   ELSE ROUND(SUM(CASE WHEN status = 5 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1)
              END AS completion_rate
            FROM t_order WHERE deleted = 0
            """)
    AdminDashboardVO.Overview selectOverview();

    @Select("""
            SELECT DATE(create_time) AS stat_date,
                   COUNT(*) AS order_count,
                   SUM(CASE WHEN status = 5 THEN 1 ELSE 0 END) AS completed_count,
                   COALESCE(SUM(CASE WHEN status = 5 THEN amount ELSE 0 END), 0) AS turnover
            FROM t_order
            WHERE deleted = 0 AND create_time >= #{startDate}
            GROUP BY DATE(create_time)
            ORDER BY stat_date
            """)
    List<AdminDashboardVO.DailyTrend> selectDailyTrend(@Param("startDate") LocalDate startDate);

    @Select("""
            SELECT COALESCE(c.name, '未知服务') AS category_name,
                   COUNT(*) AS order_count,
                   COALESCE(SUM(CASE WHEN o.status = 5 THEN o.amount ELSE 0 END), 0) AS settled_amount
            FROM t_order o
            LEFT JOIN t_service_category c ON c.id = o.category_id
            WHERE o.deleted = 0 AND o.create_time >= #{startDate}
            GROUP BY o.category_id, c.name
            ORDER BY order_count DESC
            """)
    List<AdminDashboardVO.CategoryMetric> selectCategoryMetrics(@Param("startDate") LocalDate startDate);

    @Select("""
            SELECT status, COUNT(*) AS order_count
            FROM t_order
            WHERE deleted = 0
            GROUP BY status
            ORDER BY status
            """)
    List<AdminDashboardVO.StatusMetric> selectStatusMetrics();

    @Select("""
            SELECT o.id, o.order_no, COALESCE(c.name, '未知服务') AS category_name,
                   COALESCE(p.name, '宠物档案已删除') AS pet_name,
                   o.amount, o.status, o.create_time
            FROM t_order o
            LEFT JOIN t_service_category c ON c.id = o.category_id
            LEFT JOIN t_pet p ON p.id = o.pet_id
            WHERE o.deleted = 0
            ORDER BY o.id DESC
            LIMIT 8
            """)
    List<AdminDashboardVO.RecentOrder> selectRecentOrders();
}
