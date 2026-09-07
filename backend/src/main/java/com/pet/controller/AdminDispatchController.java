package com.pet.controller;

import com.pet.common.api.PageResult;
import com.pet.common.api.Result;
import com.pet.dto.OrderQuery;
import com.pet.dto.ArbitrationDecisionDTO;
import com.pet.security.RequireRole;
import com.pet.service.OrderService;
import com.pet.vo.OrderListVO;
import com.pet.vo.SitterDispatchVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/admin/dispatch")
@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class AdminDispatchController {

    private final OrderService orderService;

    @GetMapping("/page")
    public Result<PageResult<OrderListVO>> page(@Valid OrderQuery query) {
        return Result.success(orderService.pageAll(query));
    }

    @GetMapping("/{orderId}/sitters")
    public Result<List<SitterDispatchVO>> sitters(@PathVariable Long orderId) {
        return Result.success(orderService.listAssignableSitters(orderId));
    }

    @PostMapping("/{orderId}/assign/{sitterId}")
    public Result<Void> assign(@PathVariable Long orderId, @PathVariable Long sitterId) {
        orderService.assign(orderId, sitterId);
        return Result.success();
    }

    @GetMapping("/bounty/page")
    public Result<PageResult<OrderListVO>> bountyPage(@Valid OrderQuery query) {
        return Result.success(orderService.pageBounties(query));
    }

    @PostMapping("/bounty/{orderId}/review")
    public Result<Void> reviewBounty(@PathVariable Long orderId,
                                     @Valid @RequestBody ArbitrationDecisionDTO dto) {
        orderService.reviewBounty(orderId, dto);
        return Result.success();
    }
}
