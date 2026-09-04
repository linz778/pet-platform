package com.pet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 宠物档案新增 / 编辑入参。userId 不在其中——一律取登录态，防止把宠物挂到别人名下。
 */
@Data
public class PetSaveDTO {

    @NotBlank(message = "宠物昵称不能为空")
    @Size(max = 50, message = "宠物昵称不能超过 50 字")
    private String name;

    /** 物种：狗 / 猫 / 其他 */
    @Size(max = 20, message = "物种不能超过 20 字")
    private String species;

    @Size(max = 50, message = "品种不能超过 50 字")
    private String breed;

    /** 0=未知 1=公 2=母 */
    @Min(value = 0, message = "性别取值只能是 0=未知 / 1=公 / 2=母")
    @Max(value = 2, message = "性别取值只能是 0=未知 / 1=公 / 2=母")
    private Integer gender;

    @Min(value = 0, message = "年龄不能为负数")
    @Max(value = 600, message = "年龄（月）超出合理范围")
    private Integer ageMonths;

    @DecimalMin(value = "0", message = "体重不能为负数")
    @Digits(integer = 4, fraction = 2, message = "体重最多 4 位整数、2 位小数")
    private BigDecimal weightKg;

    @Size(max = 255, message = "宠物照片地址过长")
    private String avatar;

    /** 疫苗免疫证明图片，落库时以逗号拼接存进 vaccine_cert(VARCHAR 500) */
    @Size(max = 5, message = "疫苗证明最多上传 5 张")
    private List<String> vaccineCerts;

    @Size(max = 255, message = "性格习性不能超过 255 字")
    private String personality;

    @Size(max = 500, message = "喂养禁忌不能超过 500 字")
    private String feedingTaboo;
}
