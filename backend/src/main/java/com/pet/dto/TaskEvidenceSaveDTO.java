package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 悬赏任务完成证明；一张图片一次请求，前端可连续提交多张。 */
@Data
public class TaskEvidenceSaveDTO {

    @NotBlank(message = "请上传任务证明照片")
    @Size(max = 500, message = "图片地址过长")
    private String imageUrl;

    @Size(max = 255, message = "说明不能超过 255 字")
    private String remark;
}
