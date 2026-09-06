package com.pet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 新增或编辑用户服务地址；坐标必须来自地图候选或地图选点。 */
@Data
public class UserAddressSaveDTO {

    @NotBlank(message = "请选择地址标签")
    @Size(max = 20, message = "地址标签不能超过 20 字")
    private String label;

    @NotBlank(message = "请选择省份")
    @Size(max = 50, message = "省份名称过长")
    private String province;

    @NotBlank(message = "请选择城市")
    @Size(max = 50, message = "城市名称过长")
    private String city;

    @NotBlank(message = "请选择地区")
    @Size(max = 50, message = "地区名称过长")
    private String district;

    @NotBlank(message = "请填写详细地址")
    @Size(max = 255, message = "详细地址不能超过 255 字")
    private String detailAddress;

    @NotNull(message = "请从候选地点中选择详细地址")
    @DecimalMin(value = "-180", message = "地址经度不合法")
    @DecimalMax(value = "180", message = "地址经度不合法")
    @Digits(integer = 3, fraction = 7, message = "地址经度最多 7 位小数")
    private BigDecimal lng;

    @NotNull(message = "请从候选地点中选择详细地址")
    @DecimalMin(value = "-90", message = "地址纬度不合法")
    @DecimalMax(value = "90", message = "地址纬度不合法")
    @Digits(integer = 2, fraction = 7, message = "地址纬度最多 7 位小数")
    private BigDecimal lat;

    private Boolean defaultAddress = false;
}
