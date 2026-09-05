package com.pet.controller;

import com.pet.common.api.PageResult;
import com.pet.common.api.Result;
import com.pet.dto.HallQuery;
import com.pet.dto.OrderQuery;
import com.pet.dto.SitterProfileSaveDTO;
import com.pet.security.RequireRole;
import com.pet.service.OrderService;
import com.pet.service.SitterProfileService;
import com.pet.vo.HallOrderVO;
import com.pet.vo.OrderListVO;
import com.pet.vo.SitterProfileVO;
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

/**
 * 接单员端：资质档案、接单大厅与抢单。
 * <p>
 * 整个类只允许 SITTER 角色。资质审核（管理端改 audit_status）不在这里，见 Phase 7 的管理端接口。
 */
@Tag(name = "接单员", description = "资质档案、LBS 接单大厅与抢单")
@RestController
@RequestMapping("/sitter")
@RequireRole({"SITTER"})
@RequiredArgsConstructor
public class SitterController {

    private final SitterProfileService sitterProfileService;
    private final OrderService orderService;

    @Operation(summary = "我的资质档案", description = "身份证号只返回脱敏值；auditStatus 0待审 1通过 2驳回")
    @GetMapping("/profile")
    public Result<SitterProfileVO> profile() {
        return Result.success(sitterProfileService.getMine());
    }

    @Operation(summary = "提交资质", description = "首次提交或被驳回后重新提交；提交后回到待审状态，已通过审核的档案不允许自助修改")
    @PostMapping("/profile")
    public Result<SitterProfileVO> submitProfile(@Valid @RequestBody SitterProfileSaveDTO dto) {
        return Result.success(sitterProfileService.submit(dto));
    }

    @Operation(summary = "接单大厅", description = "以当前坐标为圆心按距离升序检索附近的待接单订单；不含下单用户身份与备注")
    @GetMapping("/hall/page")
    public Result<PageResult<HallOrderVO>> hall(@Valid HallQuery query) {
        return Result.success(orderService.pageHall(query));
    }

    @Operation(summary = "抢单", description = "分布式锁 + 条件更新双保险，一单只会有一个接单员；资质未过审返回 1005，被抢先返回 2002")
    @PostMapping("/hall/{orderId}/grab")
    public Result<Void> grab(@PathVariable Long orderId) {
        orderService.grab(orderId);
        return Result.success();
    }

    @Operation(summary = "我的接单", description = "抢到的订单分页，带下单用户昵称；status 为空表示全部")
    @GetMapping("/order/page")
    public Result<PageResult<OrderListVO>> myOrders(@Valid OrderQuery query) {
        return Result.success(orderService.pageTaken(query));
    }
}
