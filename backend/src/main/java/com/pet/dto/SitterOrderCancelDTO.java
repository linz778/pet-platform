package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 接单员取消已接订单的入参；原因必须留存，供雇主与平台查看。 */
@Data
public class SitterOrderCancelDTO {

    @NotBlank(message = "请填写取消原因")
    @Size(max = 255, message = "取消原因不能超过 255 字")
    private String reason;
}
