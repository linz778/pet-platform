package com.pet.dto;

import com.pet.common.api.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 管理端申诉分页筛选。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArbitrationQuery extends PageQuery {

    @Min(value = 0, message = "申诉状态不正确")
    @Max(value = 2, message = "申诉状态不正确")
    private Integer status;
}
