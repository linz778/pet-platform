package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.entity.Wallet;

public interface WalletService extends IService<Wallet> {

    /**
     * 为新注册用户开通钱包（余额/冻结/累计收入均为 0）。
     * <p>
     * 必须在注册事务内调用：t_wallet 是下单支付的前置依赖，缺行会导致支付时余额扣减影响行数为 0，
     * 表现为「余额不足」而非真正的缺钱包，很难排查。
     */
    void initWallet(Long userId);
}
