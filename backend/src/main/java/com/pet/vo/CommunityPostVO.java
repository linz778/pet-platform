package com.pet.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 社区动态卡片。 */
@Data
public class CommunityPostVO {

    private Long id;
    private Integer type;
    private String typeText;
    private String authorName;
    private String authorAvatar;
    private String authorRole;
    private Long petId;
    private String petName;
    private String petAvatar;
    private String title;
    private String content;
    private List<String> imageUrls;
    private Integer likeCount;
    private Integer commentCount;
    private boolean liked;
    private LocalDateTime createTime;
}
