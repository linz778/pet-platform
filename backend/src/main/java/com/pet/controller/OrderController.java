package com.pet.controller;

import com.pet.common.api.PageResult;
import com.pet.common.api.Result;
import com.pet.dto.OrderCancelDTO;
import com.pet.dto.OrderCreateDTO;
import com.pet.dto.OrderQuery;
import com.pet.security.RequireRole;
import com.pet.service.FulfillmentService;
import com.pet.service.OrderService;
import com.pet.vo.OrderDetailVO;
import com.pet.vo.OrderEvidenceVO;
import com.pet.vo.OrderListVO;
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
 * 订单（下单用户侧）。
 * <p>
 * 类上的 @RequireRole 只挡住角色，挡不住资源归属：同一个 USER 角色下，
 * 谁下的单、谁能付、谁能取消，由 OrderService 逐个方法校验 userId。
 */
@Tag(name = "订单", description = "宠物主人下单、支付、取消与验收")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@RequireRole({"USER"})
public class OrderController {

    private final OrderService orderService;
    private final FulfillmentService fulfillmentService;

    @Operation(summary = "下单", description = "金额由服务端按下单时刻的服务类别规则算出，落库为待支付")
    @PostMapping
    public Result<OrderDetailVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.create(dto));
    }

    @Operation(summary = "我的订单分页", description = "status 为空表示全部：0待支付 1待接单 2已接单 3服务中 4待验收 5已完成 6已取消")
    @GetMapping("/my/page")
    public Result<PageResult<OrderListVO>> myPage(@Valid OrderQuery query) {
        return Result.success(orderService.pageMine(query));
    }

    @Operation(summary = "订单详情", description = "下单用户、该单接单员与管理员可见；平台抽成不返回给下单用户")
    @GetMapping("/{id}")
    // 方法级注解会覆盖类上的 USER 限定（AuthInterceptor 先读方法、读不到才回落到类）。
    // 详情是三角色共用的读接口：接单员履约页与管理员调度页都要看同一份订单，
    // 而「谁能看这一单」属于资源归属，@RequireRole 表达不了，由 getDetail 逐个校验。
    @RequireRole({"USER", "SITTER", "ADMIN"})
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(orderService.getDetail(id));
    }

    @Operation(summary = "履约存证", description = "打卡定位、清单照片与散步轨迹，按上传先后返回；下单用户、该单接单员与管理员可见")
    @GetMapping("/{id}/evidence")
    // 同 detail：方法级注解覆盖类上的 USER 限定，验收前用户要能看到接单员拍的每一张照片
    @RequireRole({"USER", "SITTER", "ADMIN"})
    public Result<List<OrderEvidenceVO>> evidence(@PathVariable Long id) {
        return Result.success(fulfillmentService.listEvidence(id));
    }

    @Operation(summary = "支付订单", description = "模拟支付：余额转入冻结（平台担保），订单转待接单并进入附近订单索引")
    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.success();
    }

    @Operation(summary = "取消订单", description = "仅待支付 / 待接单可取消；已支付的会全额退回余额")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody(required = false) OrderCancelDTO dto) {
        orderService.cancel(id, dto);
        return Result.success();
    }

    @Operation(summary = "验收订单", description = "待验收 → 已完成，担保资金同时结算：接单员到手入其余额，平台抽成入平台账户；重复验收返回 2003")
    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        orderService.accept(id);
        return Result.success();
    }
}
