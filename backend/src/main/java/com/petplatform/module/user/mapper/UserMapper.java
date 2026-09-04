package com.petplatform.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petplatform.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
