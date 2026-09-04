package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.dto.LoginDTO;
import com.pet.dto.RegisterDTO;
import com.pet.entity.User;
import com.pet.vo.LoginVO;

public interface UserService extends IService<User> {

    LoginVO register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);
}
