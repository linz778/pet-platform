package com.pet.controller;

import com.pet.common.api.Result;
import com.pet.dto.UserAddressSaveDTO;
import com.pet.security.RequireRole;
import com.pet.service.UserAddressService;
import com.pet.service.UserService;
import com.pet.security.UserContext;
import com.pet.vo.UserVO;
import com.pet.vo.UserAddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户", description = "当前登录用户信息")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserAddressService userAddressService;

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.getProfile(UserContext.userId()));
    }

    @Operation(summary = "我的服务地址簿", description = "默认地址排在最前；首次新增的地址自动成为默认地址")
    @GetMapping("/address")
    @RequireRole({"USER"})
    public Result<List<UserAddressVO>> addresses() {
        return Result.success(userAddressService.listMine());
    }

    @Operation(summary = "新增服务地址")
    @PostMapping("/address")
    @RequireRole({"USER"})
    public Result<UserAddressVO> createAddress(@Valid @RequestBody UserAddressSaveDTO dto) {
        return Result.success(userAddressService.create(dto));
    }

    @Operation(summary = "编辑服务地址")
    @PutMapping("/address/{id}")
    @RequireRole({"USER"})
    public Result<UserAddressVO> updateAddress(@PathVariable Long id,
                                               @Valid @RequestBody UserAddressSaveDTO dto) {
        return Result.success(userAddressService.update(id, dto));
    }

    @Operation(summary = "设为默认服务地址")
    @PostMapping("/address/{id}/default")
    @RequireRole({"USER"})
    public Result<UserAddressVO> defaultAddress(@PathVariable Long id) {
        return Result.success(userAddressService.setDefault(id));
    }

    @Operation(summary = "删除服务地址")
    @DeleteMapping("/address/{id}")
    @RequireRole({"USER"})
    public Result<Void> deleteAddress(@PathVariable Long id) {
        userAddressService.delete(id);
        return Result.success();
    }
}
