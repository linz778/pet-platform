package com.pet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class NotificationInboxVO {
    private List<SiteNotificationVO> items;
    private long unreadCount;
}
