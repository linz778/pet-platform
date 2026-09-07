package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户给社区帖子留下的一枚“爪印”。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_community_like")
public class CommunityLike extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long userId;
}
