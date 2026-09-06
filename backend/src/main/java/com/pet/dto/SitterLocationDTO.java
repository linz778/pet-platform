package com.pet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 接单员手动设置的大厅检索位置；只更新坐标，不影响资质审核状态。 */
@Data
public class SitterLocationDTO {

    @NotNull(message = "请填写经度")
    @DecimalMin(value = "-180", message = "经度取值不合法")
    @DecimalMax(value = "180", message = "经度取值不合法")
    @Digits(integer = 3, fraction = 7, message = "经度最多 7 位小数")
    private BigDecimal lng;

    @NotNull(message = "请填写纬度")
    @DecimalMin(value = "-90", message = "纬度取值不合法")
    @DecimalMax(value = "90", message = "纬度取值不合法")
    @Digits(integer = 2, fraction = 7, message = "纬度最多 7 位小数")
    private BigDecimal lat;
}
