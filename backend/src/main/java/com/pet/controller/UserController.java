package com.pet.controller;

import com.pet.common.api.Result;
import com.pet.entity.User;
import com.pet.service.UserService;
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户", description = "当前登录用户信息")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<User> me() {
        LoginUser loginUser = UserContext.require();
        return Result.success(userService.getById(loginUser.getUserId()));
    }
}
