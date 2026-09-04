package com.pet.controller;

import com.pet.common.api.Result;
import com.pet.dto.PetSaveDTO;
import com.pet.security.RequireRole;
import com.pet.service.PetService;
import com.pet.vo.PetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 宠物档案。全部接口都按登录态过滤归属，@RequireRole 只管角色、管不了「这条档案是谁的」。
 */
@Tag(name = "宠物档案", description = "宠物主人管理自己的宠物")
@RestController
@RequestMapping("/pet")
@RequiredArgsConstructor
@RequireRole({"USER"})
public class PetController {

    private final PetService petService;

    @Operation(summary = "我的宠物列表")
    @GetMapping("/my")
    public Result<List<PetVO>> my() {
        return Result.success(petService.listMine());
    }

    @Operation(summary = "宠物详情")
    @GetMapping("/{id}")
    public Result<PetVO> detail(@PathVariable Long id) {
        return Result.success(petService.getMine(id));
    }

    @Operation(summary = "新增宠物")
    @PostMapping
    public Result<PetVO> create(@Valid @RequestBody PetSaveDTO dto) {
        return Result.success(petService.create(dto));
    }

    @Operation(summary = "编辑宠物")
    @PutMapping("/{id}")
    public Result<PetVO> update(@PathVariable Long id, @Valid @RequestBody PetSaveDTO dto) {
        return Result.success(petService.update(id, dto));
    }

    @Operation(summary = "删除宠物", description = "逻辑删除，不做物理清除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        petService.delete(id);
        return Result.success();
    }
}
