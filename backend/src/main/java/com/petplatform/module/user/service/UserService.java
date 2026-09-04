package com.petplatform.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.petplatform.module.user.dto.LoginDTO;
import com.petplatform.module.user.dto.RegisterDTO;
import com.petplatform.module.user.entity.User;
import com.petplatform.module.user.vo.LoginVO;

public interface UserService extends IService<User> {

    LoginVO register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);
}
