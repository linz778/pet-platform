package com.pet.service.impl;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.api.PageResult;
import com.pet.common.api.ResultCode;
import com.pet.common.enums.AuditStatus;
import com.pet.common.exception.BusinessException;
import com.pet.dto.SitterAuditDecisionDTO;
import com.pet.dto.SitterAuditQuery;
import com.pet.entity.SitterProfile;
import com.pet.entity.User;
import com.pet.mapper.SitterProfileMapper;
import com.pet.mapper.UserMapper;
import com.pet.service.SitterAuditService;
import com.pet.vo.SitterAuditVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SitterAuditServiceImpl implements SitterAuditService {

    private final SitterProfileMapper profileMapper;
    private final UserMapper userMapper;

    @Override
    public PageResult<SitterAuditVO> page(SitterAuditQuery query) {
        String keyword = StrUtil.trim(query.getKeyword());
        Page<SitterProfile> result = profileMapper.selectPage(query.toPage(),
                Wrappers.<SitterProfile>lambdaQuery()
                        .isNotNull(SitterProfile::getRealName)
                        .eq(query.getStatus() != null, SitterProfile::getAuditStatus, query.getStatus())
                        .like(StrUtil.isNotBlank(keyword), SitterProfile::getRealName, keyword)
                        .orderByAsc(SitterProfile::getAuditStatus)
                        .orderByDesc(SitterProfile::getUpdateTime));
        Map<Long, User> users = loadUsers(result.getRecords());
        List<SitterAuditVO> records = result.getRecords().stream()
                .map(profile -> toVO(profile, users.get(profile.getUserId())))
                .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public SitterAuditVO decide(Long profileId, SitterAuditDecisionDTO dto) {
        SitterProfile profile = profileId == null ? null : profileMapper.selectById(profileId);
        if (profile == null) {
            throw new BusinessException(ResultCode.SITTER_PROFILE_NOT_FOUND);
        }
        if (!Integer.valueOf(AuditStatus.PENDING.getCode()).equals(profile.getAuditStatus())) {
            throw new BusinessException(ResultCode.SITTER_AUDIT_STATUS_ILLEGAL);
        }

        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        int status = approved ? AuditStatus.APPROVED.getCode() : AuditStatus.REJECTED.getCode();
        int creditLevel = dto.getCreditLevel() == null ? 3 : dto.getCreditLevel();
        String remark = approved ? null : StrUtil.trim(dto.getRemark());
        int available = approved ? 1 : 0;
        if (profileMapper.audit(profileId, status, remark, creditLevel, available) == 0) {
            throw new BusinessException(ResultCode.SITTER_AUDIT_STATUS_ILLEGAL);
        }

        profile.setAuditStatus(status);
        profile.setAuditRemark(remark);
        profile.setCreditLevel(creditLevel);
        profile.setAvailable(available);
        profile.setUpdateTime(LocalDateTime.now());
        return toVO(profile, userMapper.selectById(profile.getUserId()));
    }

    private Map<Long, User> loadUsers(List<SitterProfile> profiles) {
        Set<Long> ids = profiles.stream().map(SitterProfile::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return ids.isEmpty() ? Map.of() : userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private SitterAuditVO toVO(SitterProfile profile, User user) {
        SitterAuditVO vo = new SitterAuditVO();
        vo.setProfileId(profile.getId());
        vo.setUserId(profile.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setNickname(user == null ? null : user.getNickname());
        vo.setPhoneMasked(user == null || StrUtil.isBlank(user.getPhone())
                ? null : DesensitizedUtil.mobilePhone(user.getPhone()));
        vo.setRealName(profile.getRealName());
        vo.setIdCardMasked(StrUtil.isBlank(profile.getIdCard())
                ? null : DesensitizedUtil.idCardNum(profile.getIdCard(), 6, 4));
        vo.setIdCardImg(profile.getIdCardImg());
        vo.setHealthCert(profile.getHealthCert());
        vo.setQualification(profile.getQualification());
        vo.setExperienceYears(profile.getExperienceYears());
        vo.setAuditStatus(profile.getAuditStatus());
        vo.setAuditStatusText(AuditStatus.descOf(profile.getAuditStatus()));
        vo.setAuditRemark(profile.getAuditRemark());
        vo.setCreditLevel(profile.getCreditLevel());
        vo.setCreditScore(profile.getCreditScore() == null ? 100 : profile.getCreditScore());
        vo.setAvailable(profile.getAvailable());
        vo.setSubmitTime(profile.getUpdateTime());
        return vo;
    }
}
