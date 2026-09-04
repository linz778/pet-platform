package com.pet.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前登录用户信息出参（避免直接暴露 User 实体，剔除 password/updateTime/deleted 等内部字段）。
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
}
