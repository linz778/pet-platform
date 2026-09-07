package com.pet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServiceRuleUpdateDTO {

    @NotNull(message = "请填写基础单价")
    @DecimalMin(value = "0.01", message = "基础单价必须大于 0")
    @DecimalMax(value = "99999.99", message = "基础单价不能超过 99999.99")
    @Digits(integer = 5, fraction = 2, message = "基础单价最多保留两位小数")
    private BigDecimal basePrice;

    @NotNull(message = "请填写周末溢价倍数")
    @DecimalMin(value = "1.000", message = "周末溢价倍数不能小于 1")
    @DecimalMax(value = "5.000", message = "周末溢价倍数不能超过 5")
    @Digits(integer = 1, fraction = 2, message = "周末溢价倍数最多保留两位小数")
    private BigDecimal holidayRate;

    @NotNull(message = "请填写平台抽成比例")
    @DecimalMin(value = "0.000", message = "平台抽成比例不能小于 0")
    @DecimalMax(value = "1.000", message = "平台抽成比例不能超过 100%")
    @Digits(integer = 1, fraction = 3, message = "平台抽成比例最多保留三位小数")
    private BigDecimal commissionRate;

    @NotNull(message = "作业清单不能为空")
    @Size(max = 20, message = "作业清单最多 20 项")
    private List<@NotBlank(message = "作业清单项不能为空") @Size(max = 30, message = "单项最多 30 个字") String> checklist;

    @NotNull(message = "请选择上下架状态")
    @Min(value = 0, message = "上下架状态不合法")
    @Max(value = 1, message = "上下架状态不合法")
    private Integer status;
}
