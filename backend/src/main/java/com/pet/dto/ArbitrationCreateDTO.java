package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 用户发起服务申诉的原因与图片证据。 */
@Data
public class ArbitrationCreateDTO {

    @NotBlank(message = "请填写不满意原因或接单员操作问题")
    @Size(max = 500, message = "申诉原因不能超过 500 字")
    private String reason;

    @NotEmpty(message = "请至少上传 1 张证据照片")
    @Size(max = 5, message = "证据照片最多上传 5 张")
    private List<@NotBlank(message = "证据照片地址不能为空") String> evidenceUrls;
}
