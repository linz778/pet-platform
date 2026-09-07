package com.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.entity.SitterProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SitterProfileMapper extends BaseMapper<SitterProfile> {

    /** 原子扣减信誉分，最低保持 0，防止并发取消把分数扣成负数。 */
    @Update("UPDATE t_sitter_profile SET credit_score = GREATEST(credit_score - #{points}, 0), update_time = NOW() "
            + "WHERE user_id = #{userId} AND deleted = 0")
    int deductCreditScore(@Param("userId") Long userId, @Param("points") int points);

    /** 管理端审核待审资料，带前置状态条件避免两名管理员重复审核。 */
    @Update("UPDATE t_sitter_profile SET audit_status = #{status}, audit_remark = #{remark}, "
            + "credit_level = #{creditLevel}, available = #{available}, update_time = NOW() "
            + "WHERE id = #{id} AND audit_status = 0 AND deleted = 0")
    int audit(@Param("id") Long id, @Param("status") int status, @Param("remark") String remark,
              @Param("creditLevel") int creditLevel, @Param("available") int available);
}
