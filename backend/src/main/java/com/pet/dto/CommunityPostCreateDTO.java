package com.pet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 宠物主人发布晒宠动态或养宠问题。 */
@Data
public class CommunityPostCreateDTO {

    @NotNull(message = "请选择发布类型")
    @Min(value = 1, message = "发布类型不正确")
    @Max(value = 2, message = "发布类型不正确")
    private Integer type;

    /** 可关联自己的宠物档案；不关联时为空。 */
    private Long petId;

    @NotBlank(message = "请填写标题")
    @Size(max = 100, message = "标题不能超过 100 字")
    private String title;

    @NotBlank(message = "请填写正文")
    @Size(max = 2000, message = "正文不能超过 2000 字")
    private String content;

    @Size(max = 5, message = "最多上传 5 张图片")
    private List<String> imageUrls;
}
