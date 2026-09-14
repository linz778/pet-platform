package com.pet.service;

import com.pet.entity.SitterProfile;
import com.pet.entity.User;
import com.pet.mapper.SitterProfileMapper;
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

    @Mock
    private SitterProfileMapper profileMapper;

    @Test
    void disablesUserAccountAtomically() {
        User user = new User();
        user.setId(8L);
        user.setRole("USER");
        user.setStatus(1);
        when(userMapper.selectById(8L)).thenReturn(user);
        when(userMapper.updateStatus(8L, "USER", 1, 0)).thenReturn(1);

        new AdminAccountService(userMapper, profileMapper).setUserStatus(8L, 0);

        verify(userMapper).updateStatus(8L, "USER", 1, 0);
    }

    @Test
    void disablingSitterAlsoStopsTakingOrders() {
        User sitter = new User();
        sitter.setId(9L);
        sitter.setRole("SITTER");
        sitter.setStatus(1);
        when(userMapper.selectById(9L)).thenReturn(sitter);
        when(userMapper.updateStatus(9L, "SITTER", 1, 0)).thenReturn(1);

        new AdminAccountService(userMapper, profileMapper).setSitterStatus(9L, 0);

        verify(userMapper).updateStatus(9L, "SITTER", 1, 0);
        verify(profileMapper).disableAvailability(9L);
    }
}
