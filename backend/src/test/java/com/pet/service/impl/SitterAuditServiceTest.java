package com.pet.service.impl;

import com.pet.common.api.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.dto.SitterAuditDecisionDTO;
import com.pet.entity.SitterProfile;
import com.pet.entity.User;
import com.pet.mapper.SitterProfileMapper;
import com.pet.mapper.UserMapper;
import com.pet.vo.SitterAuditVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SitterAuditServiceTest {

    @Mock private SitterProfileMapper profileMapper;
    @Mock private UserMapper userMapper;

    @Test
    void approvalMakesSitterAvailableAndReturnsUpdatedStatus() {
        SitterProfile profile = pendingProfile();
        when(profileMapper.selectById(11L)).thenReturn(profile);
        when(profileMapper.audit(11L, 1, null, 4, 1)).thenReturn(1);
        when(userMapper.selectById(3L)).thenReturn(sitterUser());
        SitterAuditDecisionDTO dto = new SitterAuditDecisionDTO();
        dto.setApproved(true);
        dto.setCreditLevel(4);

        SitterAuditVO result = new SitterAuditServiceImpl(profileMapper, userMapper).decide(11L, dto);

        assertThat(result.getAuditStatus()).isEqualTo(1);
        assertThat(result.getAuditStatusText()).isEqualTo("已通过");
        assertThat(result.getAvailable()).isEqualTo(1);
        assertThat(result.getCreditLevel()).isEqualTo(4);
        assertThat(result.getIdCardMasked()).doesNotContain("19900101");
    }

    @Test
    void rejectionDisablesSitterAndReturnsReason() {
        SitterProfile profile = pendingProfile();
        when(profileMapper.selectById(11L)).thenReturn(profile);
        when(profileMapper.audit(11L, 2, "健康证明已过期", 3, 0)).thenReturn(1);
        when(userMapper.selectById(3L)).thenReturn(sitterUser());
        SitterAuditDecisionDTO dto = new SitterAuditDecisionDTO();
        dto.setApproved(false);
        dto.setRemark("  健康证明已过期  ");

        SitterAuditVO result = new SitterAuditServiceImpl(profileMapper, userMapper).decide(11L, dto);

        assertThat(result.getAuditStatus()).isEqualTo(2);
        assertThat(result.getAuditRemark()).isEqualTo("健康证明已过期");
        assertThat(result.getAvailable()).isZero();
    }

    @Test
    void alreadyReviewedProfileCannotBeReviewedAgain() {
        SitterProfile profile = pendingProfile();
        profile.setAuditStatus(1);
        when(profileMapper.selectById(11L)).thenReturn(profile);
        SitterAuditDecisionDTO dto = new SitterAuditDecisionDTO();
        dto.setApproved(true);

        assertThatThrownBy(() -> new SitterAuditServiceImpl(profileMapper, userMapper).decide(11L, dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SITTER_AUDIT_STATUS_ILLEGAL.getCode());
    }

    private SitterProfile pendingProfile() {
        SitterProfile profile = new SitterProfile();
        profile.setId(11L);
        profile.setUserId(3L);
        profile.setRealName("测试接单员");
        profile.setIdCard("440101199001011234");
        profile.setAuditStatus(0);
        profile.setCreditLevel(3);
        profile.setCreditScore(100);
        profile.setAvailable(1);
        return profile;
    }

    private User sitterUser() {
        User user = new User();
        user.setId(3L);
        user.setUsername("sitter");
        user.setNickname("接单员");
        user.setPhone("13800000002");
        return user;
    }
}
