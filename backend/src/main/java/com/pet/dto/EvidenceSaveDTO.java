package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 作业清单单项存证：一项一张照片。
 * <p>
 * checkItem 必须来自该服务类别的 checklist_template（如「换粮」「铲砂」），
 * 由服务端比对，避免接单员自造一个「已完成」项糊弄验收。
 */
@Data
public class EvidenceSaveDTO {

    @NotBlank(message = "请选择清单项")
    @Size(max = 50, message = "清单项名称过长")
    private String checkItem;

    /** 先调 /file/upload 拿到的图片地址 */
    @NotBlank(message = "请上传存证照片")
    @Size(max = 500, message = "图片地址过长")
    private String imageUrl;

    /** 拍照时的定位，可空：室内拍清单照片常常取不到坐标 */
    private BigDecimal lat;

    private BigDecimal lng;

    @Size(max = 255, message = "备注不能超过 255 字")
    private String remark;
}
