package com.pet.service.impl;

import com.pet.common.enums.OrderStatus;
import com.pet.entity.Order;
import com.pet.entity.User;
import com.pet.mapper.OrderMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.UserMapper;
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import com.pet.service.ServiceCategoryService;
import com.pet.vo.OrderDetailVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

/** 用户查看已接订单详情时的接单员显示名测试。 */
@ExtendWith(MockitoExtension.class)
class OrderDetailSitterNameTest {

    private static final long ORDER_ID = 77L;
    private static final long OWNER_ID = 2L;
    private static final long SITTER_ID = 3L;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PetMapper petMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ServiceCategoryService serviceCategoryService;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(null, petMapper, userMapper, serviceCategoryService,
                null, null, null, null);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        UserContext.set(new LoginUser(OWNER_ID, "user", "USER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("用户查看已接订单：详情返回接单员公开昵称")
    void ownerCanSeeAssignedSitterDisplayName() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(OWNER_ID);
        order.setSitterId(SITTER_ID);
        order.setCategoryId(1L);
        order.setPetId(1L);
        order.setStatus(OrderStatus.TAKEN.getCode());
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(serviceCategoryService.listByIds(anyCollection())).thenReturn(List.of());
        when(petMapper.selectSnapshots(anyCollection())).thenReturn(List.of());

        User sitter = new User();
        sitter.setId(SITTER_ID);
        sitter.setUsername("sitter");
        sitter.setNickname("演示接单员");
        when(userMapper.selectById(SITTER_ID)).thenReturn(sitter);

        OrderDetailVO detail = service.getDetail(ORDER_ID);

        assertThat(detail.getSitterName()).isEqualTo("演示接单员");
    }
}
