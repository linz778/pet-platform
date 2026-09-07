package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 宠物社区的晒宠分享或养宠问题。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_community_post")
public class CommunityPost extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long authorId;
    /** 1=晒宠分享 2=养宠问答 */
    private Integer type;
    private Long petId;
    private String title;
    private String content;
    /** 最多 5 张图片 URL，逗号分隔。 */
    private String images;
    private Integer likeCount;
    private Integer commentCount;
    /** 0=隐藏 1=公开 */
    private Integer status;
}
