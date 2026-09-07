package com.pet.dto;

import com.pet.common.api.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 社区动态分页，type 为空表示全部。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommunityPostQuery extends PageQuery {

    @Min(value = 1, message = "帖子类型不正确")
    @Max(value = 2, message = "帖子类型不正确")
    private Integer type;
}
