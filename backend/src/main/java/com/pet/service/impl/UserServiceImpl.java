package com.pet.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.api.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.dto.LoginDTO;
import com.pet.dto.RegisterDTO;
import com.pet.entity.User;
import com.pet.mapper.UserMapper;
import com.pet.service.UserService;
import com.pet.vo.LoginVO;
import com.pet.security.JwtUtil;
import com.pet.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtil jwtUtil;

    @Override
    public LoginVO register(RegisterDTO dto) {
        boolean exists = baseMapper.exists(Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        if (exists) {
            throw new BusinessException("用户名已存在");
        }
        String role = "SITTER".equals(dto.getRole()) ? "SITTER" : "USER";

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setNickname(dto.getNickname() == null ? dto.getUsername() : dto.getNickname());
        user.setRole(role);
        user.setStatus(1);
        save(user);

        return buildLoginVO(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = getOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.ACCOUNT_OR_PWD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        return buildLoginVO(user);
    }

    private LoginVO buildLoginVO(User user) {
        String token = jwtUtil.generateToken(new LoginUser(user.getId(), user.getUsername(), user.getRole()));
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        return vo;
    }
}
