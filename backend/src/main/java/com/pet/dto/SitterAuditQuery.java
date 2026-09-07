package com.pet.dto;

import com.pet.common.api.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SitterAuditQuery extends PageQuery {

    @Min(value = 0, message = "审核状态不正确")
    @Max(value = 2, message = "审核状态不正确")
    private Integer status;

    @Size(max = 50, message = "搜索关键词不能超过 50 字")
    private String keyword;
}
