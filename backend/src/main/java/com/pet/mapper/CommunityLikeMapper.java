package com.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.entity.CommunityLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommunityLikeMapper extends BaseMapper<CommunityLike> {

    /** 爪印允许反复取消再送出，必须物理删除，否则逻辑删除行仍会占住唯一键。 */
    @Delete("DELETE FROM t_community_like WHERE id = #{id}")
    int removeLike(@Param("id") Long id);
}
