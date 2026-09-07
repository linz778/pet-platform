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

/** 用户发布的自定义悬赏任务；金额由用户填写，平台按隐藏类别的抽成比例拆分。 */
@Data
public class BountyTaskCreateDTO {

    @NotNull(message = "请选择服务宠物")
    private Long petId;

    @NotBlank(message = "请填写任务标题")
    @Size(max = 100, message = "任务标题不能超过 100 字")
    private String title;

    @NotBlank(message = "请填写任务要求")
    @Size(max = 1000, message = "任务要求不能超过 1000 字")
    private String description;

    @NotNull(message = "请填写悬赏金额")
    @DecimalMin(value = "1.00", message = "悬赏金额不能低于 1 元")
    @DecimalMax(value = "10000.00", message = "悬赏金额不能超过 10000 元")
    @Digits(integer = 5, fraction = 2, message = "悬赏金额最多保留两位小数")
    private BigDecimal amount;

    @NotBlank(message = "服务地址不能为空")
    @Size(max = 255, message = "服务地址不能超过 255 字")
    private String serviceAddress;

    @NotNull(message = "请选择服务地址")
    @DecimalMin(value = "-90", message = "纬度取值范围是 -90 ~ 90")
    @DecimalMax(value = "90", message = "纬度取值范围是 -90 ~ 90")
    @Digits(integer = 3, fraction = 7, message = "纬度最多 7 位小数")
    private BigDecimal addressLat;

    @NotNull(message = "请选择服务地址")
    @DecimalMin(value = "-180", message = "经度取值范围是 -180 ~ 180")
    @DecimalMax(value = "180", message = "经度取值范围是 -180 ~ 180")
    @Digits(integer = 3, fraction = 7, message = "经度最多 7 位小数")
    private BigDecimal addressLng;

    @NotNull(message = "请选择任务开始时间")
    private LocalDateTime serviceStart;
}
