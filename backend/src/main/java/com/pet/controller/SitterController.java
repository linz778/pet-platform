package com.pet.controller;

import com.pet.common.api.PageResult;
import com.pet.common.api.Result;
import com.pet.dto.CheckInDTO;
import com.pet.dto.EvidenceSaveDTO;
import com.pet.dto.HallQuery;
import com.pet.dto.OrderQuery;
import com.pet.dto.SitterAddressSaveDTO;
import com.pet.dto.SitterProfileSaveDTO;
import com.pet.dto.SitterLocationDTO;
import com.pet.dto.TrackSaveDTO;
import com.pet.security.RequireRole;
import com.pet.service.FulfillmentService;
import com.pet.service.OrderService;
import com.pet.service.SitterAddressService;
import com.pet.service.SitterProfileService;
import com.pet.vo.HallOrderVO;
import com.pet.vo.OrderEvidenceVO;
import com.pet.vo.OrderListVO;
import com.pet.vo.SitterAddressVO;
import com.pet.vo.SitterProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

/**
 * 接单员端：资质档案、接单大厅、抢单与履约。
 * <p>
 * 整个类只允许 SITTER 角色。「这一单是不是他接的」属于资源归属，@RequireRole 表达不了，
 * 由 FulfillmentService / OrderService 逐个方法比对 sitter_id。
 * 资质审核（管理端改 audit_status）不在这里，见 Phase 7 的管理端接口。
 */
@Tag(name = "接单员", description = "资质档案、LBS 接单大厅、抢单与履约")
@RestController
@RequestMapping("/sitter")
@RequireRole({"SITTER"})
@RequiredArgsConstructor
public class SitterController {

    private final SitterProfileService sitterProfileService;
    private final SitterAddressService sitterAddressService;
    private final OrderService orderService;
    private final FulfillmentService fulfillmentService;

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

    @Operation(summary = "保存检索位置", description = "浏览器定位不可用时手动设置大厅检索中心；不改变资质审核状态")
    @PostMapping("/location")
    public Result<SitterProfileVO> updateLocation(@Valid @RequestBody SitterLocationDTO dto) {
        return Result.success(sitterProfileService.updateLocation(dto));
    }

    @Operation(summary = "我的地址簿", description = "默认地址排在最前，坐标用于定位失败时检索附近订单")
    @GetMapping("/address")
    public Result<List<SitterAddressVO>> addresses() {
        return Result.success(sitterAddressService.listMine());
    }

    @Operation(summary = "新增地址簿地址")
    @PostMapping("/address")
    public Result<SitterAddressVO> createAddress(@Valid @RequestBody SitterAddressSaveDTO dto) {
        return Result.success(sitterAddressService.create(dto));
    }

    @Operation(summary = "编辑地址簿地址")
    @PutMapping("/address/{id}")
    public Result<SitterAddressVO> updateAddress(@PathVariable Long id,
                                                 @Valid @RequestBody SitterAddressSaveDTO dto) {
        return Result.success(sitterAddressService.update(id, dto));
    }

    @Operation(summary = "设为默认地址")
    @PostMapping("/address/{id}/default")
    public Result<SitterAddressVO> defaultAddress(@PathVariable Long id) {
        return Result.success(sitterAddressService.setDefault(id));
    }

    @Operation(summary = "删除地址簿地址")
    @DeleteMapping("/address/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        sitterAddressService.delete(id);
        return Result.success();
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

    @Operation(summary = "到达定位打卡", description = "已接单 → 服务中；坐标距服务地址超过 pet-platform.geo.check-in-radius 返回 2004")
    @PostMapping("/order/{orderId}/checkin")
    public Result<Void> checkIn(@PathVariable Long orderId, @Valid @RequestBody CheckInDTO dto) {
        fulfillmentService.checkIn(orderId, dto);
        return Result.success();
    }

    @Operation(summary = "上传清单存证", description = "服务中逐项拍照，checkItem 必须属于该服务类别的作业清单；同一项重复提交按重拍覆盖")
    @PostMapping("/order/{orderId}/evidence")
    public Result<OrderEvidenceVO> saveEvidence(@PathVariable Long orderId, @Valid @RequestBody EvidenceSaveDTO dto) {
        return Result.success(fulfillmentService.saveChecklistEvidence(orderId, dto));
    }

    @Operation(summary = "上传散步轨迹", description = "服务中上传轨迹点数组，同一单可多次上传（分段遛）")
    @PostMapping("/order/{orderId}/track")
    public Result<OrderEvidenceVO> saveTrack(@PathVariable Long orderId, @Valid @RequestBody TrackSaveDTO dto) {
        return Result.success(fulfillmentService.saveTrack(orderId, dto));
    }

    @Operation(summary = "完成服务", description = "服务中 → 待验收；作业清单必须逐项都有存证，缺项返回 2008 并列出还差哪几项")
    @PostMapping("/order/{orderId}/finish")
    public Result<Void> finish(@PathVariable Long orderId) {
        fulfillmentService.finish(orderId);
        return Result.success();
    }
}
