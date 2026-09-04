package com.petplatform.module.user.controller;

import com.petplatform.common.api.Result;
import com.petplatform.module.user.entity.User;
import com.petplatform.module.user.service.UserService;
import com.petplatform.security.LoginUser;
import com.petplatform.security.UserContext;
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
        User user = userService.getById(loginUser.getUserId());
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
}
