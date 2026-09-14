package com.pet.controller;

import com.pet.common.api.PageResult;
import com.pet.common.api.Result;
import com.pet.dto.AdminUserQuery;
import com.pet.security.RequireRole;
import com.pet.service.AdminAccountService;
import com.pet.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端账号管理", description = "查询并启用或禁用平台账号")
@Validated
@RestController
@RequestMapping("/admin/accounts")
@RequireRole({"ADMIN"})
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @Operation(summary = "宠物主人分页")
    @GetMapping("/users/page")
    public Result<PageResult<AdminUserVO>> users(@Valid AdminUserQuery query) {
        return Result.success(adminAccountService.pageUsers(query));
    }

    @Operation(summary = "启用或禁用宠物主人账号")
    @PutMapping("/users/{id}/status")
    public Result<Void> userStatus(@PathVariable Long id, @RequestParam @Min(0) @Max(1) int status) {
        adminAccountService.setUserStatus(id, status);
        return Result.success();
    }
}
