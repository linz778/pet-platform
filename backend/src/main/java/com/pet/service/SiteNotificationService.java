package com.pet.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pet.entity.SiteNotification;
import com.pet.entity.User;
import com.pet.mapper.SiteNotificationMapper;
import com.pet.mapper.UserMapper;
import com.pet.security.UserContext;
import com.pet.vo.NotificationInboxVO;
import com.pet.vo.SiteNotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteNotificationService {

    public static final String SITTER_AUDIT = "SITTER_AUDIT";
    public static final String ARBITRATION = "ARBITRATION";
    public static final String BOUNTY_REVIEW = "BOUNTY_REVIEW";

    private final SiteNotificationMapper notificationMapper;
    private final UserMapper userMapper;

    public NotificationInboxVO inbox(int limit) {
        Long userId = UserContext.userId();
        List<SiteNotificationVO> items = notificationMapper.selectList(Wrappers.<SiteNotification>lambdaQuery()
                        .eq(SiteNotification::getUserId, userId)
                        .orderByDesc(SiteNotification::getId)
                        .last("LIMIT " + limit)).stream()
                .map(this::toVO)
                .toList();
        long unread = notificationMapper.selectCount(Wrappers.<SiteNotification>lambdaQuery()
                .eq(SiteNotification::getUserId, userId)
                .eq(SiteNotification::getReadStatus, 0));
        return new NotificationInboxVO(items, unread);
    }

    public void markRead(Long id) {
        notificationMapper.markRead(id, UserContext.userId());
    }

    public void markAllRead() {
        notificationMapper.markAllRead(UserContext.userId());
    }

    public void send(Long userId, String title, String content, String type, Long businessId) {
        if (userId == null) return;
        SiteNotification notification = new SiteNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setBusinessId(businessId);
        notification.setReadStatus(0);
        notificationMapper.insert(notification);
    }

    public void sendToAdmins(String title, String content, String type, Long businessId) {
        userMapper.selectList(Wrappers.<User>lambdaQuery()
                        .eq(User::getRole, "ADMIN")
                        .eq(User::getStatus, 1))
                .forEach(admin -> send(admin.getId(), title, content, type, businessId));
    }

    private SiteNotificationVO toVO(SiteNotification notification) {
        SiteNotificationVO vo = new SiteNotificationVO();
        vo.setId(notification.getId());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setType(notification.getType());
        vo.setBusinessId(notification.getBusinessId());
        vo.setReadStatus(notification.getReadStatus());
        vo.setCreateTime(notification.getCreateTime());
        return vo;
    }
}
