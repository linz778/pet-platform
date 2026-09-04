package com.petplatform.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.petplatform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户（含宠物主人 / 接单员 / 管理员，用 role 区分）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 加密后的密码 */
    private String password;

    private String phone;

    private String nickname;

    private String avatar;

    /** USER / SITTER / ADMIN */
    private String role;

    /** 0=禁用 1=正常 */
    private Integer status;
}
