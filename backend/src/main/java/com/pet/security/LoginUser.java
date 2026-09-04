package com.pet.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录态用户，存放在 UserContext（ThreadLocal）中。
 * role: USER(宠物主人) / SITTER(接单员) / ADMIN(运营管理员)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    private Long userId;
    private String username;
    private String role;
}
