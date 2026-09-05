package com.pet.service.impl;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.api.ResultCode;
import com.pet.common.enums.AuditStatus;
import com.pet.common.exception.BusinessException;
import com.pet.dto.SitterProfileSaveDTO;
import com.pet.entity.SitterProfile;
import com.pet.mapper.SitterProfileMapper;
import com.pet.security.UserContext;
import com.pet.service.SitterProfileService;
import com.pet.vo.SitterProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SitterProfileServiceImpl extends ServiceImpl<SitterProfileMapper, SitterProfile> implements SitterProfileService {

    /** 可接单 */
    private static final int AVAILABLE = 1;

    @Override
    public void initProfile(Long userId) {
        save(newPendingProfile(userId));
    }

    @Override
    public SitterProfileVO getMine() {
        return toVO(loadOrInit(UserContext.userId()));
    }

    @Override
    public SitterProfileVO submit(SitterProfileSaveDTO dto) {
        SitterProfile profile = loadOrInit(UserContext.userId());
        if (Integer.valueOf(AuditStatus.APPROVED.getCode()).equals(profile.getAuditStatus())) {
            throw new BusinessException(ResultCode.SITTER_ALREADY_AUDITED);
        }

        // 逐列显式 set 而不是 updateById：默认字段策略跳过 null，清空证件照片会「点了没反应」。
        // 驳回原因必须一起置空——留着上一轮的驳回理由，管理端复审时会以为问题还没改。
        // update(null, wrapper) 不触发 MetaObjectHandler，update_time 只能自己写。
        update(Wrappers.<SitterProfile>lambdaUpdate()
                .eq(SitterProfile::getId, profile.getId())
                .set(SitterProfile::getRealName, dto.getRealName())
                .set(SitterProfile::getIdCard, dto.getIdCard())
                .set(SitterProfile::getIdCardImg, dto.getIdCardImg())
                .set(SitterProfile::getHealthCert, dto.getHealthCert())
                .set(SitterProfile::getQualification, dto.getQualification())
                .set(SitterProfile::getExperienceYears, dto.getExperienceYears())
                .set(SitterProfile::getAuditStatus, AuditStatus.PENDING.getCode())
                .set(SitterProfile::getAuditRemark, null)
                .set(SitterProfile::getUpdateTime, LocalDateTime.now()));

        return toVO(getById(profile.getId()));
    }

    @Override
    public void requireGrabable(Long sitterId) {
        SitterProfile profile = requireProfile(sitterId);
        if (!Integer.valueOf(AuditStatus.APPROVED.getCode()).equals(profile.getAuditStatus())) {
            throw new BusinessException(ResultCode.SITTER_NOT_AUDITED);
        }
        if (!Integer.valueOf(AVAILABLE).equals(profile.getAvailable())) {
            throw new BusinessException(ResultCode.SITTER_NOT_AVAILABLE);
        }
    }

    /**
     * 抢单入口绝不能顺手建档：档案缺失意味着注册流程或数据被动过，
     * 静默补一条待审档案只会把问题藏起来，让人以为「注册完就能抢单」。
     */
    private SitterProfile requireProfile(Long userId) {
        SitterProfile profile = getOne(Wrappers.<SitterProfile>lambdaQuery()
                .eq(SitterProfile::getUserId, userId));
        if (profile == null) {
            throw new BusinessException(ResultCode.SITTER_PROFILE_NOT_FOUND);
        }
        return profile;
    }

    /**
     * 资质页缺行时懒初始化。注册流程一定会建档，走到这里说明是历史数据或角色被手工改过；
     * 与其让整页报错，不如补一条待审档案，人还能继续提交资料走审核。
     */
    private SitterProfile loadOrInit(Long userId) {
        SitterProfile profile = getOne(Wrappers.<SitterProfile>lambdaQuery()
                .eq(SitterProfile::getUserId, userId));
        if (profile != null) {
            return profile;
        }
        profile = newPendingProfile(userId);
        save(profile);
        return profile;
    }

    private SitterProfile newPendingProfile(Long userId) {
        SitterProfile profile = new SitterProfile();
        profile.setUserId(userId);
        profile.setExperienceYears(0);
        profile.setAuditStatus(AuditStatus.PENDING.getCode());
        profile.setCreditLevel(3);
        profile.setAvailable(AVAILABLE);
        return profile;
    }

    private SitterProfileVO toVO(SitterProfile p) {
        SitterProfileVO vo = new SitterProfileVO();
        vo.setUserId(p.getUserId());
        vo.setRealName(p.getRealName());
        boolean filled = StrUtil.isNotBlank(p.getIdCard());
        vo.setIdCardFilled(filled);
        // 前 6 位是地址码、后 4 位含顺序码与校验位，中间 8 位出生日期打码
        vo.setIdCardMasked(filled ? DesensitizedUtil.idCardNum(p.getIdCard(), 6, 4) : null);
        vo.setIdCardImg(p.getIdCardImg());
        vo.setHealthCert(p.getHealthCert());
        vo.setQualification(p.getQualification());
        vo.setExperienceYears(p.getExperienceYears());
        vo.setAuditStatus(p.getAuditStatus());
        vo.setAuditStatusText(AuditStatus.descOf(p.getAuditStatus()));
        vo.setAuditRemark(p.getAuditRemark());
        vo.setCreditLevel(p.getCreditLevel());
        vo.setAvailable(p.getAvailable());
        vo.setCurrentLng(p.getCurrentLng());
        vo.setCurrentLat(p.getCurrentLat());
        return vo;
    }
}
