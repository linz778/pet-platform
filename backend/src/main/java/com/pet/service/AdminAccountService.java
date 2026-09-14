package com.pet.service;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.api.PageResult;
import com.pet.common.api.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.dto.AdminUserQuery;
import com.pet.entity.User;
import com.pet.mapper.UserMapper;
import com.pet.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private static final String USER_ROLE = "USER";
    private final UserMapper userMapper;

    public PageResult<AdminUserVO> pageUsers(AdminUserQuery query) {
        String keyword = StrUtil.trim(query.getKeyword());
        Page<User> page = userMapper.selectPage(query.toPage(), Wrappers.<User>lambdaQuery()
                .eq(User::getRole, USER_ROLE)
                .eq(query.getStatus() != null, User::getStatus, query.getStatus())
                .and(StrUtil.isNotBlank(keyword), w -> w.like(User::getUsername, keyword)
                        .or().like(User::getNickname, keyword)
                        .or().like(User::getPhone, keyword))
                .orderByDesc(User::getId));
        List<AdminUserVO> records = page.getRecords().stream().map(this::toUserVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public void setUserStatus(Long userId, int status) {
        User user = userMapper.selectById(userId);
        if (user == null || !USER_ROLE.equals(user.getRole())) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (Objects.equals(user.getStatus(), status)) {
            return;
        }
        if (userMapper.updateStatus(userId, USER_ROLE, user.getStatus(), status) == 0) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
    }

    private AdminUserVO toUserVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhoneMasked(StrUtil.isBlank(user.getPhone()) ? null : DesensitizedUtil.mobilePhone(user.getPhone()));
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
