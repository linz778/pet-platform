package com.pet.controller;

import com.pet.common.api.Result;
import com.pet.service.ServiceCategoryService;
import com.pet.vo.PricePreviewVO;
import com.pet.vo.ServiceCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务目录与计价。登录即可访问，不限角色——用户选购、接单员看单价、管理员验证改价效果都要用。
 */
@Tag(name = "服务类别", description = "服务目录查询与计价预览")
@RestController
@RequestMapping("/service-category")
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final ServiceCategoryService serviceCategoryService;

    @Operation(summary = "上架中的服务目录")
    @GetMapping("/list")
    public Result<List<ServiceCategoryVO>> list() {
        return Result.success(serviceCategoryService.listOnShelf());
    }

    /**
     * GET query 上的 LocalDateTime 走 ConversionService，JacksonConfig 里注册的反序列化器管不到它。
     * <p>
     * 实测（摘掉本注解重启验证）：Spring 默认只认 ISO 的 {@code 2026-09-05T10:00:00}，
     * 前端 el-date-picker 发来的 {@code 2026-09-05 10:00:00} 会直接 400；
     * 标上 pattern 之后空格分隔与 ISO 两种写法都能解析。
     */
    @Operation(summary = "计价预览", description = "按预约开始时间判断是否周末溢价，返回实付/平台抽成/接单员到手")
    @GetMapping("/price-preview")
    public Result<PricePreviewVO> pricePreview(
            @RequestParam Long categoryId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime serviceStart) {
        return Result.success(serviceCategoryService.previewPrice(categoryId, serviceStart));
    }

    @Operation(summary = "服务详情", description = "不校验上下架状态，管理端要能查看已下架的类别")
    @GetMapping("/{id}")
    public Result<ServiceCategoryVO> detail(@PathVariable Long id) {
        return Result.success(serviceCategoryService.getDetail(id));
    }
}
