package com.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.entity.CommunityComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommunityCommentMapper extends BaseMapper<CommunityComment> {

    @Update("UPDATE t_community_comment SET status = #{status}, update_time = NOW() "
            + "WHERE id = #{id} AND status = #{expectedStatus} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("expectedStatus") int expectedStatus,
                     @Param("status") int status);
}
