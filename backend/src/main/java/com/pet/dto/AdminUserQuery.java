package com.pet.dto;

import com.pet.common.api.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUserQuery extends PageQuery {

    @Min(0)
    @Max(1)
    private Integer status;

    @Size(max = 50)
    private String keyword;
}
