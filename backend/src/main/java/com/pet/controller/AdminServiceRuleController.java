package com.pet.controller;

import com.pet.common.api.Result;
import com.pet.dto.ServiceRuleUpdateDTO;
import com.pet.security.RequireRole;
import com.pet.service.ServiceCategoryService;
import com.pet.vo.ServiceCategoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/service-rules")
@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class AdminServiceRuleController {

    private final ServiceCategoryService serviceCategoryService;

    @GetMapping
    public Result<List<ServiceCategoryVO>> list() {
        return Result.success(serviceCategoryService.listAllRules());
    }

    @PutMapping("/{categoryId}")
    public Result<ServiceCategoryVO> update(@PathVariable Long categoryId,
                                             @Valid @RequestBody ServiceRuleUpdateDTO dto) {
        return Result.success(serviceCategoryService.updateRule(categoryId, dto));
    }
}
