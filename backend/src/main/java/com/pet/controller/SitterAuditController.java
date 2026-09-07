package com.pet.controller;

import com.pet.common.api.PageResult;
import com.pet.common.api.Result;
import com.pet.dto.SitterAuditDecisionDTO;
import com.pet.dto.SitterAuditQuery;
import com.pet.security.RequireRole;
import com.pet.service.SitterAuditService;
import com.pet.vo.SitterAuditVO;
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

@Tag(name = "管理端资质审核", description = "读取接单员提交资料并通过或驳回")
@RestController
@RequestMapping("/admin/sitter-audit")
@RequireRole({"ADMIN"})
@RequiredArgsConstructor
public class SitterAuditController {

    private final SitterAuditService sitterAuditService;

    @Operation(summary = "资质申请分页", description = "只列出已提交实名资料的接单员，支持按状态和真实姓名筛选")
    @GetMapping("/page")
    public Result<PageResult<SitterAuditVO>> page(@Valid SitterAuditQuery query) {
        return Result.success(sitterAuditService.page(query));
    }

    @Operation(summary = "审核资质", description = "通过后接单员立即变为可接单；驳回必须填写原因并回传接单员端")
    @PostMapping("/{profileId}/decision")
    public Result<SitterAuditVO> decide(@PathVariable Long profileId,
                                        @Valid @RequestBody SitterAuditDecisionDTO dto) {
        return Result.success(sitterAuditService.decide(profileId, dto));
    }
}
