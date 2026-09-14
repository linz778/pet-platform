package com.pet.service;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.api.PageResult;
import com.pet.common.api.ResultCode;
import com.pet.common.enums.AuditStatus;
import com.pet.common.exception.BusinessException;
import com.pet.dto.AdminSitterQuery;
import com.pet.dto.AdminUserQuery;
import com.pet.entity.SitterProfile;
import com.pet.entity.User;
import com.pet.mapper.SitterProfileMapper;
import com.pet.mapper.UserMapper;
import com.pet.vo.AdminSitterVO;
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
    private static final String SITTER_ROLE = "SITTER";
    private final UserMapper userMapper;
    private final SitterProfileMapper profileMapper;

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

    public PageResult<AdminSitterVO> pageSitters(AdminSitterQuery query) {
        query.setKeyword(StrUtil.trim(query.getKeyword()));
        Page<AdminSitterVO> page = profileMapper.pageAdmin(query.toPage(), query);
        page.getRecords().forEach(item -> item.setAuditStatusText(AuditStatus.descOf(item.getAuditStatus())));
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public void setSitterStatus(Long userId, int status) {
        User sitter = requireSitter(userId);
        if (!Objects.equals(sitter.getStatus(), status)
                && userMapper.updateStatus(userId, SITTER_ROLE, sitter.getStatus(), status) == 0) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (status == 0) {
            profileMapper.disableAvailability(userId);
        }
    }

    @Transactional
    public void setSitterAvailable(Long userId, int status) {
        User sitter = requireSitter(userId);
        if (status == 1 && !Integer.valueOf(1).equals(sitter.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        SitterProfile profile = profileMapper.selectOne(Wrappers.<SitterProfile>lambdaQuery()
                .eq(SitterProfile::getUserId, userId));
        if (profile == null) {
            throw new BusinessException(ResultCode.SITTER_PROFILE_NOT_FOUND);
        }
        if (status == 1 && !Integer.valueOf(AuditStatus.APPROVED.getCode()).equals(profile.getAuditStatus())) {
            throw new BusinessException(ResultCode.SITTER_NOT_AUDITED);
        }
        if (Objects.equals(profile.getAvailable(), status)) {
            return;
        }
        if (profileMapper.updateAvailability(userId, profile.getAvailable(), status) == 0) {
            throw new BusinessException(ResultCode.SITTER_PROFILE_NOT_FOUND);
        }
    }

    private User requireSitter(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !SITTER_ROLE.equals(user.getRole())) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
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
