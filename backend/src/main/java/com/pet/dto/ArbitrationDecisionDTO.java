package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员对申诉作出通过退款或驳回的裁定。 */
@Data
public class ArbitrationDecisionDTO {

    @NotNull(message = "请选择审核结果")
    private Boolean approved;

    @NotBlank(message = "请填写审核说明")
    @Size(max = 500, message = "审核说明不能超过 500 字")
    private String result;
}
