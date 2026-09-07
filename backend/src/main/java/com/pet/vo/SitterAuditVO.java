package com.pet.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SitterAuditVO {
    private Long profileId;
    private Long userId;
    private String username;
    private String nickname;
    private String phoneMasked;
    private String realName;
    private String idCardMasked;
    private String idCardImg;
    private String healthCert;
    private String qualification;
    private Integer experienceYears;
    private Integer auditStatus;
    private String auditStatusText;
    private String auditRemark;
    private Integer creditLevel;
    private Integer creditScore;
    private Integer available;
    private LocalDateTime submitTime;
}
