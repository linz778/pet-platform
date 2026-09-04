package com.pet.security;

import com.pet.common.api.ResultCode;
import com.pet.common.exception.BusinessException;

/**
 * 当前登录用户上下文（ThreadLocal）。请求结束务必调用 clear() 防止内存泄漏。
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 获取当前登录用户，未登录抛 401。 */
    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long userId() {
        return require().getUserId();
    }

    /** 当前登录用户角色：USER / SITTER / ADMIN。 */
    public static String role() {
        return require().getRole();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(role());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
