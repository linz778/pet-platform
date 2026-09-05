package com.pet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 下单入参。
 * <p>
 * 不含 userId（取登录态）、也不含任何金额字段——amount / commission / sitterIncome
 * 一律由服务端按下单时刻的服务类别规则算出，前端传什么都不会被采信。
 */
@Data
public class OrderCreateDTO {

    @NotNull(message = "请选择服务宠物")
    private Long petId;

    @NotNull(message = "请选择服务项目")
    private Long categoryId;

    @NotBlank(message = "服务地址不能为空")
    @Size(max = 255, message = "服务地址不能超过 255 字")
    private String serviceAddress;

    /** 纬度，与经度一起用于接单大厅的附近检索与到达打卡的距离校验 */
    @NotNull(message = "请在地图上标注服务地址")
    @DecimalMin(value = "-90", message = "纬度取值范围是 -90 ~ 90")
    @DecimalMax(value = "90", message = "纬度取值范围是 -90 ~ 90")
    @Digits(integer = 3, fraction = 7, message = "纬度最多 7 位小数")
    private BigDecimal addressLat;

    @NotNull(message = "请在地图上标注服务地址")
    @DecimalMin(value = "-180", message = "经度取值范围是 -180 ~ 180")
    @DecimalMax(value = "180", message = "经度取值范围是 -180 ~ 180")
    @Digits(integer = 3, fraction = 7, message = "经度最多 7 位小数")
    private BigDecimal addressLng;

    /** 是否晚于当前时间由服务端判定并抛 SERVICE_TIME_ILLEGAL，不用 @Future，好让规则集中一处 */
    @NotNull(message = "请选择预约开始时间")
    private LocalDateTime serviceStart;

    private LocalDateTime serviceEnd;

    @Size(max = 500, message = "备注不能超过 500 字")
    private String remark;
}
