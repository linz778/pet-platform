package com.pet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 接单员提交 / 修改资质入参。
 * <p>
 * 不含 auditStatus、creditLevel、available：审核结论与信誉等级只能由管理端改，
 * 接单员自己能改的话，注册完直接把 auditStatus 传 1 就能跳过审核去抢单。
 */
@Data
public class SitterProfileSaveDTO {

    @NotBlank(message = "请填写真实姓名")
    @Size(max = 50, message = "真实姓名不能超过 50 字")
    private String realName;

    @NotBlank(message = "请填写身份证号")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    @Size(max = 500, message = "身份证照片地址过长")
    private String idCardImg;

    @Size(max = 500, message = "健康证明地址过长")
    private String healthCert;

    @Size(max = 500, message = "资质证书地址过长")
    private String qualification;

    @Min(value = 0, message = "经验年限不能为负")
    @Max(value = 60, message = "经验年限填写不合理")
    private Integer experienceYears = 0;
}
