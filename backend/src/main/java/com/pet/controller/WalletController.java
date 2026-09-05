package com.pet.controller;

import com.pet.common.api.PageQuery;
import com.pet.common.api.PageResult;
import com.pet.common.api.Result;
import com.pet.dto.RechargeDTO;
import com.pet.dto.WithdrawDTO;
import com.pet.service.WalletService;
import com.pet.vo.WalletTransactionVO;
import com.pet.vo.WalletVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 钱包。本期是模拟资金流：充值即时到账、支付即冻结担保、验收后拆分结算、提现即时成功。
 * <p>
 * 不加 @RequireRole：用户要充值付款，接单员要看收益和提现，三种角色都用得到。
 */
@Tag(name = "钱包", description = "余额、担保冻结、流水与提现")
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "我的钱包", description = "含可用余额、担保中的冻结金额与累计收入")
    @GetMapping("/me")
    public Result<WalletVO> me() {
        return Result.success(walletService.getMine());
    }

    @Operation(summary = "我的流水", description = "type 为空表示全部：1充值 2支付 3佣金入账 4提现 5退款 6平台佣金")
    @GetMapping("/transaction/page")
    public Result<PageResult<WalletTransactionVO>> transactions(@RequestParam(required = false) Integer type,
                                                               @Valid PageQuery query) {
        return Result.success(walletService.pageMyTransactions(type, query));
    }

    @Operation(summary = "充值", description = "模拟支付通道，金额区间 1 - 10000，即时到账")
    @PostMapping("/recharge")
    public Result<WalletVO> recharge(@Valid @RequestBody RechargeDTO dto) {
        walletService.recharge(dto.getAmount());
        return Result.success(walletService.getMine());
    }

    @Operation(summary = "提现", description = "模拟银行通道，即时成功；扣可用余额，不减累计收入")
    @PostMapping("/withdraw")
    public Result<WalletVO> withdraw(@Valid @RequestBody WithdrawDTO dto) {
        walletService.withdraw(dto.getAmount());
        return Result.success(walletService.getMine());
    }
}
