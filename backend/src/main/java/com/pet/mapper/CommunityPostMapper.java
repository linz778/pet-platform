package com.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.entity.CommunityPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommunityPostMapper extends BaseMapper<CommunityPost> {

    @Update("UPDATE t_community_post SET like_count = like_count + 1, update_time = NOW() "
            + "WHERE id = #{id} AND status = 1 AND deleted = 0")
    int incrementLike(@Param("id") Long id);

    @Update("UPDATE t_community_post SET like_count = GREATEST(like_count - 1, 0), update_time = NOW() "
            + "WHERE id = #{id} AND status = 1 AND deleted = 0")
    int decrementLike(@Param("id") Long id);

    @Update("UPDATE t_community_post SET comment_count = comment_count + 1, update_time = NOW() "
            + "WHERE id = #{id} AND status = 1 AND deleted = 0")
    int incrementComment(@Param("id") Long id);
}
