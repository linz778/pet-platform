package com.pet.controller;

import com.pet.common.api.Result;
import com.pet.service.SiteNotificationService;
import com.pet.vo.NotificationInboxVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "站内消息", description = "查询、已读处理当前账号的消息")
@Validated
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class SiteNotificationController {

    private final SiteNotificationService notificationService;

    @Operation(summary = "消息与未读数")
    @GetMapping
    public Result<NotificationInboxVO> inbox(@RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return Result.success(notificationService.inbox(limit));
    }

    @Operation(summary = "标记一条消息已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.success();
    }
}
