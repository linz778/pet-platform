package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 社区帖子下的评论或问题回答。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_community_comment")
public class CommunityComment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long authorId;
    private String content;
    private Integer status;
}
