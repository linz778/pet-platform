package com.pet.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pet.dto.SitterLocationDTO;
import com.pet.entity.SitterProfile;
import com.pet.mapper.SitterProfileMapper;
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import com.pet.vo.SitterProfileVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SitterProfileServiceTest {

    private static final long USER_ID = 3L;

    @Mock
    private SitterProfileMapper mapper;

    private SitterProfileServiceImpl service;

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(SitterProfileMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, SitterProfile.class);
    }

    @BeforeEach
    void setUp() {
        service = new SitterProfileServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        UserContext.set(new LoginUser(USER_ID, "sitter", "SITTER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("保存备用位置只更新经纬度，不改变资质审核状态")
    void updateLocationKeepsAuditState() {
        SitterProfile existing = profile("121.4737000", "31.2304000");
        SitterProfile updated = profile("113.4915000", "23.4505000");
        when(mapper.selectOne(any(), eq(true))).thenReturn(existing, updated);
        when(mapper.update(isNull(), any())).thenReturn(1);

        SitterLocationDTO dto = new SitterLocationDTO();
        dto.setLng(new BigDecimal("113.4915000"));
        dto.setLat(new BigDecimal("23.4505000"));

        SitterProfileVO result = service.updateLocation(dto);

        ArgumentCaptor<Wrapper<SitterProfile>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).update(isNull(), captor.capture());
        String sqlSet = ((LambdaUpdateWrapper<SitterProfile>) captor.getValue()).getSqlSet();
        assertThat(sqlSet).contains("current_lng", "current_lat", "update_time");
        assertThat(sqlSet).doesNotContain("audit_status", "real_name", "id_card");
        assertThat(result.getCurrentLng()).isEqualByComparingTo("113.4915000");
        assertThat(result.getCurrentLat()).isEqualByComparingTo("23.4505000");
        assertThat(result.getAuditStatus()).isEqualTo(1);
    }

    private SitterProfile profile(String lng, String lat) {
        SitterProfile profile = new SitterProfile();
        profile.setId(10L);
        profile.setUserId(USER_ID);
        profile.setRealName("演示接单员");
        profile.setAuditStatus(1);
        profile.setAvailable(1);
        profile.setCurrentLng(new BigDecimal(lng));
        profile.setCurrentLat(new BigDecimal(lat));
        return profile;
    }
}
