package com.pet.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 社区评论；authorRole=SITTER 时由前端标记为接单员专业答复。 */
@Data
public class CommunityCommentVO {

    private Long id;
    private String authorName;
    private String authorAvatar;
    private String authorRole;
    private String content;
    private LocalDateTime createTime;
}
