package com.pet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.entity.Wallet;
import com.pet.mapper.WalletMapper;
import com.pet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl extends ServiceImpl<WalletMapper, Wallet> implements WalletService {

    @Override
    public void initWallet(Long userId) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setFrozen(BigDecimal.ZERO);
        wallet.setTotalIncome(BigDecimal.ZERO);
        save(wallet);
    }
}
