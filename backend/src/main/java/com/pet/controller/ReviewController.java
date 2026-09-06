package com.pet.controller;

import com.pet.common.api.PageQuery;
import com.pet.common.api.PageResult;
import com.pet.common.api.Result;
import com.pet.dto.ReviewSaveDTO;
import com.pet.security.RequireRole;
import com.pet.service.ReviewService;
import com.pet.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 双向评价。
 * <p>
 * 类上只放 USER 与 SITTER：管理员不是任何一方当事人，替他开写入口只会污染信誉数据。
 * 「这一单是不是他的」属于资源归属，@RequireRole 表达不了，由 ReviewService 逐个方法比对。
 */
@Tag(name = "评价", description = "订单完成后的双向评价")
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
@RequireRole({"USER", "SITTER"})
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "提交评价", description = "评谁由订单归属反推，前端不传被评价人；订单须已完成，同一人对同一单只能评一次（重复返回 2009）")
    @PostMapping
    public Result<ReviewVO> submit(@Valid @RequestBody ReviewSaveDTO dto) {
        return Result.success(reviewService.submit(dto));
    }

    @Operation(summary = "某单的双向评价", description = "下单用户、该单接单员与管理员可见；匿名评价不下发评价人 id 与昵称")
    @GetMapping("/order/{orderId}")
    // 方法级注解会覆盖类上的 USER/SITTER 限定（AuthInterceptor 先读方法、读不到才回落到类）。
    // 管理员处理纠纷时要看到双方各自写了什么，但依然不能替任何一方提交评价
    @RequireRole({"USER", "SITTER", "ADMIN"})
    public Result<List<ReviewVO>> byOrder(@PathVariable Long orderId) {
        return Result.success(reviewService.listByOrder(orderId));
    }

    @Operation(summary = "我收到的评价", description = "别人给我的评价分页，按时间倒序；带订单号与服务类别名")
    @GetMapping("/received/page")
    public Result<PageResult<ReviewVO>> received(@Valid PageQuery query) {
        return Result.success(reviewService.pageReceived(query));
    }
}
