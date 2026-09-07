package com.pet.controller;

import com.pet.common.api.PageResult;
import com.pet.common.api.Result;
import com.pet.dto.ArbitrationCreateDTO;
import com.pet.dto.ArbitrationDecisionDTO;
import com.pet.dto.ArbitrationQuery;
import com.pet.security.RequireRole;
import com.pet.service.ArbitrationService;
import com.pet.vo.ArbitrationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "纠纷仲裁", description = "用户申诉、双方查看与管理员裁定退款")
@RestController
@RequestMapping("/arbitration")
@RequiredArgsConstructor
public class ArbitrationController {

    private final ArbitrationService arbitrationService;

    @Operation(summary = "提交服务申诉", description = "仅待验收订单可提交；提交后订单进入仲裁中，担保资金继续冻结")
    @PostMapping("/order/{orderId}")
    @RequireRole({"USER"})
    public Result<Void> submit(@PathVariable Long orderId, @Valid @RequestBody ArbitrationCreateDTO dto) {
        arbitrationService.submit(orderId, dto);
        return Result.success();
    }

    @Operation(summary = "查看订单申诉", description = "下单用户、该单接单员和管理员可查看；尚未申诉时 data 为 null")
    @GetMapping("/order/{orderId}")
    @RequireRole({"USER", "SITTER", "ADMIN"})
    public Result<ArbitrationVO> byOrder(@PathVariable Long orderId) {
        return Result.success(arbitrationService.getByOrder(orderId));
    }

    @Operation(summary = "管理端申诉列表")
    @GetMapping("/admin/page")
    @RequireRole({"ADMIN"})
    public Result<PageResult<ArbitrationVO>> adminPage(@Valid ArbitrationQuery query) {
        return Result.success(arbitrationService.pageAdmin(query));
    }

    @Operation(summary = "管理员裁定", description = "通过则全额退回用户余额并关闭订单；驳回则订单回到待验收")
    @PostMapping("/{id}/decision")
    @RequireRole({"ADMIN"})
    public Result<Void> decide(@PathVariable Long id, @Valid @RequestBody ArbitrationDecisionDTO dto) {
        arbitrationService.decide(id, dto);
        return Result.success();
    }
}
