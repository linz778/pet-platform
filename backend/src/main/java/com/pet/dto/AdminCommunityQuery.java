package com.pet.dto;

import com.pet.common.api.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 管理端社区内容筛选。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminCommunityQuery extends PageQuery {

    @Min(1)
    @Max(2)
    private Integer type;

    @Min(0)
    @Max(1)
    private Integer status;

    @Size(max = 50, message = "搜索关键词不能超过 50 字")
    private String keyword;
}
