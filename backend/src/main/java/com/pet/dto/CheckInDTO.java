package com.pet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 到达定位打卡。
 * <p>
 * 坐标由前端现场采集（浏览器定位，取不到时由接单员确认的兜底坐标），
 * 服务端拿它与订单的服务地址算 Haversine 距离，超出
 * {@code pet-platform.geo.check-in-radius} 就拒绝打卡（2004）。
 */
@Data
public class CheckInDTO {

    @NotNull(message = "请提供打卡纬度")
    @DecimalMin(value = "-90", message = "纬度取值不合法")
    @DecimalMax(value = "90", message = "纬度取值不合法")
    private BigDecimal lat;

    @NotNull(message = "请提供打卡经度")
    @DecimalMin(value = "-180", message = "经度取值不合法")
    @DecimalMax(value = "180", message = "经度取值不合法")
    private BigDecimal lng;
}
