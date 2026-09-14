package com.pet.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SiteNotificationVO {
    private Long id;
    private String title;
    private String content;
    private String type;
    private Long businessId;
    private Integer readStatus;
    private LocalDateTime createTime;
}
