package com.pet.controller;

import com.pet.common.api.Result;
import com.pet.security.RequireRole;
import com.pet.service.AdminDashboardService;
import com.pet.vo.AdminDashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端数据看板", description = "平台核心指标、经营趋势与订单分布")
@Validated
@RestController
@RequestMapping("/admin/dashboard")
@RequireRole({"ADMIN"})
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "数据分析看板", description = "days 支持 7、14 或 30 天；缺少订单的日期自动补零")
    @GetMapping
    public Result<AdminDashboardVO> dashboard(
            @RequestParam(defaultValue = "7") @Min(7) @Max(30) int days) {
        return Result.success(adminDashboardService.getDashboard(days));
    }
}
