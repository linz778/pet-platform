package com.pet.dto;

import com.pet.common.api.PageQuery;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 接单大厅检索条件：以接单员当前坐标为圆心找附近的待接单订单。
 * <p>
 * 坐标必须由前端提供，不能改读 t_sitter_profile.current_lng/lat：本期没有位置上报链路，
 * 那两列只是建档时的固定坐标，拿它当圆心会把「人已经走开了」的旧位置当成当前位置。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HallQuery extends PageQuery {

    @NotNull(message = "请提供当前经度")
    @DecimalMin(value = "-180", message = "经度取值不合法")
    @DecimalMax(value = "180", message = "经度取值不合法")
    private BigDecimal lng;

    @NotNull(message = "请提供当前纬度")
    @DecimalMin(value = "-90", message = "纬度取值不合法")
    @DecimalMax(value = "90", message = "纬度取值不合法")
    private BigDecimal lat;

    /**
     * 检索半径（公里）。上限 50：跨城上门没有意义，放大半径只会让 Redis GEO 返回一大堆无用候选。
     */
    @NotNull(message = "请提供检索半径")
    @DecimalMin(value = "0.5", message = "检索半径不能小于 0.5 公里")
    @DecimalMax(value = "50", message = "检索半径不能超过 50 公里")
    private BigDecimal radiusKm = BigDecimal.TEN;
}
