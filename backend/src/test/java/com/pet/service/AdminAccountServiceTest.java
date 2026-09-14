package com.pet.service;

import com.pet.entity.User;
import com.pet.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @Mock
    private UserMapper userMapper;

    @Test
    void disablesUserAccountAtomically() {
        User user = new User();
        user.setId(8L);
        user.setRole("USER");
        user.setStatus(1);
        when(userMapper.selectById(8L)).thenReturn(user);
        when(userMapper.updateStatus(8L, "USER", 1, 0)).thenReturn(1);

        new AdminAccountService(userMapper).setUserStatus(8L, 0);

        verify(userMapper).updateStatus(8L, "USER", 1, 0);
    }
}
