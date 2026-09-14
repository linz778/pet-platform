package com.pet.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminSitterVO {
    private Long profileId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String phoneMasked;
    private Integer userStatus;
    private String realName;
    private Integer auditStatus;
    private String auditStatusText;
    private Integer creditLevel;
    private Integer creditScore;
    private Integer experienceYears;
    private Integer available;
    private LocalDateTime createTime;
}
