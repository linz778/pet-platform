package com.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.dto.AdminSitterQuery;
import com.pet.entity.SitterProfile;
import com.pet.vo.AdminSitterVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    @Select("""
            <script>
            SELECT p.id AS profile_id, p.user_id, u.username, u.nickname, u.avatar,
                   CASE WHEN u.phone IS NULL OR u.phone = '' THEN NULL
                        ELSE CONCAT(LEFT(u.phone, 3), '****', RIGHT(u.phone, 4)) END AS phone_masked,
                   u.status AS user_status, p.real_name, p.audit_status, p.credit_level,
                   p.credit_score, p.experience_years, p.available, u.create_time
            FROM t_user u
            LEFT JOIN t_sitter_profile p ON p.user_id = u.id AND p.deleted = 0
            WHERE u.deleted = 0 AND u.role = 'SITTER'
            <if test="query.keyword != null and query.keyword != ''">
              AND (u.username LIKE CONCAT('%', #{query.keyword}, '%')
                OR u.nickname LIKE CONCAT('%', #{query.keyword}, '%')
                OR u.phone LIKE CONCAT('%', #{query.keyword}, '%')
                OR p.real_name LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            <if test="query.userStatus != null">AND u.status = #{query.userStatus}</if>
            <if test="query.auditStatus != null">AND p.audit_status = #{query.auditStatus}</if>
            <if test="query.available != null">AND p.available = #{query.available}</if>
            ORDER BY u.id DESC
            </script>
            """)
    Page<AdminSitterVO> pageAdmin(Page<AdminSitterVO> page, @Param("query") AdminSitterQuery query);

    @Update("UPDATE t_sitter_profile SET available = #{status}, update_time = NOW() " +
            "WHERE user_id = #{userId} AND available = #{expectedStatus} AND deleted = 0")
    int updateAvailability(@Param("userId") Long userId,
                           @Param("expectedStatus") Integer expectedStatus,
                           @Param("status") int status);

    @Update("UPDATE t_sitter_profile SET available = 0, update_time = NOW() " +
            "WHERE user_id = #{userId} AND available != 0 AND deleted = 0")
    int disableAvailability(@Param("userId") Long userId);
}
