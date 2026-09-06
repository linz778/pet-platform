package com.pet.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pet.dto.UserAddressSaveDTO;
import com.pet.entity.UserAddress;
import com.pet.mapper.UserAddressMapper;
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import com.pet.vo.UserAddressVO;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    private static final long USER_ID = 2L;

    @Mock
    private UserAddressMapper mapper;

    private UserAddressServiceImpl service;

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(UserAddressMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, UserAddress.class);
    }

    @BeforeEach
    void setUp() {
        service = new UserAddressServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        UserContext.set(new LoginUser(USER_ID, "user", "USER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("用户第一次保存的服务地址自动成为默认地址")
    void firstAddressBecomesDefault() {
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress address = invocation.getArgument(0);
            address.setId(21L);
            return 1;
        });

        UserAddressSaveDTO dto = new UserAddressSaveDTO();
        dto.setLabel("家");
        dto.setProvince("广东省");
        dto.setCity("广州市");
        dto.setDistrict("天河区");
        dto.setDetailAddress("明珠中学附近");
        dto.setLng(new BigDecimal("113.4915000"));
        dto.setLat(new BigDecimal("23.4505000"));

        UserAddressVO result = service.create(dto);

        assertThat(result.getId()).isEqualTo(21L);
        assertThat(result.isDefaultAddress()).isTrue();
        assertThat(result.getLabel()).isEqualTo("家");
    }
}
