package com.pet.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pet.dto.SitterAddressSaveDTO;
import com.pet.entity.SitterAddress;
import com.pet.mapper.SitterAddressMapper;
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import com.pet.service.SitterProfileService;
import com.pet.vo.SitterAddressVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SitterAddressServiceTest {

    private static final long SITTER_ID = 3L;

    @Mock
    private SitterAddressMapper mapper;

    @Mock
    private SitterProfileService profileService;

    private SitterAddressServiceImpl service;

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(SitterAddressMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, SitterAddress.class);
    }

    @BeforeEach
    void setUp() {
        service = new SitterAddressServiceImpl(profileService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        UserContext.set(new LoginUser(SITTER_ID, "sitter", "SITTER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("第一个地址自动成为默认地址并同步备用搜索坐标")
    void firstAddressBecomesDefault() {
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(SitterAddress.class))).thenAnswer(invocation -> {
            SitterAddress address = invocation.getArgument(0);
            address.setId(12L);
            return 1;
        });

        SitterAddressSaveDTO dto = new SitterAddressSaveDTO();
        dto.setLabel("家");
        dto.setProvince("广东省");
        dto.setCity("广州市");
        dto.setDistrict("天河区");
        dto.setDetailAddress("明珠中学");
        dto.setLng(new BigDecimal("113.4915000"));
        dto.setLat(new BigDecimal("23.4505000"));

        SitterAddressVO result = service.create(dto);

        assertThat(result.isDefaultAddress()).isTrue();
        assertThat(result.getLabel()).isEqualTo("家");
        assertThat(result.getId()).isEqualTo(12L);
        verify(profileService).syncSearchLocation(
                new BigDecimal("113.4915000"), new BigDecimal("23.4505000"));
    }
}
