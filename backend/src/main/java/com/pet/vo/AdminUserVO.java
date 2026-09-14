package com.pet.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String phoneMasked;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
}
