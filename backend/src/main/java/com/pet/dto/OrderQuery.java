package com.pet.dto;

import com.pet.common.api.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户端「我的订单」查询条件。status 为空表示全部状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderQuery extends PageQuery {

    @Min(value = 0, message = "订单状态取值不合法")
    @Max(value = 7, message = "订单状态取值不合法")
    private Integer status;
}
