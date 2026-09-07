package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 社区评论；接单员在问题帖下发布时前端展示为专业答复。 */
@Data
public class CommunityCommentCreateDTO {

    @NotBlank(message = "请输入回复内容")
    @Size(max = 1000, message = "回复不能超过 1000 字")
    private String content;
}
