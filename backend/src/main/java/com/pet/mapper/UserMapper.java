package com.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE t_user SET status = #{status}, update_time = NOW() " +
            "WHERE id = #{id} AND role = #{role} AND status = #{expectedStatus} AND deleted = 0")
    int updateStatus(@Param("id") Long id,
                     @Param("role") String role,
                     @Param("expectedStatus") Integer expectedStatus,
                     @Param("status") int status);
}
