package com.pet.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SitterAuditDecisionDTO {

    @NotNull(message = "请选择审核结论")
    private Boolean approved;

    @Size(max = 500, message = "审核说明不能超过 500 字")
    private String remark;

    @Min(value = 1, message = "信誉等级最低为 1")
    @Max(value = 5, message = "信誉等级最高为 5")
    private Integer creditLevel = 3;

    @AssertTrue(message = "驳回时必须填写具体原因")
    public boolean isRejectReasonPresent() {
        return Boolean.TRUE.equals(approved) || (remark != null && !remark.isBlank());
    }
}
