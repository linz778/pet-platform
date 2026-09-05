package com.pet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 散步轨迹上的一个采样点。
 * <p>
 * 与 {@link com.pet.vo.TrackPointVO} 字段同名，是刻意保留的两份：
 * 入参这份带校验注解，出参那份不带，合并成一个类会让「校验规则」跟着响应体一起对外暴露。
 */
@Data
public class TrackPointDTO {

    @NotNull(message = "轨迹点缺少纬度")
    @DecimalMin(value = "-90", message = "纬度取值不合法")
    @DecimalMax(value = "90", message = "纬度取值不合法")
    private BigDecimal lat;

    @NotNull(message = "轨迹点缺少经度")
    @DecimalMin(value = "-180", message = "经度取值不合法")
    @DecimalMax(value = "180", message = "经度取值不合法")
    private BigDecimal lng;

    /** 采样时刻，前端 watchPosition 回调里取；缺省由服务端补当前时间 */
    private LocalDateTime time;
}
