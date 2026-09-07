package com.pet.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 接单员为自己服务过的宠物记录照护习性。 */
@Data
public class SitterPetNoteSaveDTO {

    @NotNull(message = "请选择服务过的宠物")
    private Long petId;

    @Size(max = 500, message = "性格习性不能超过 500 字")
    private String habits;

    @Size(max = 500, message = "饮食与禁忌不能超过 500 字")
    private String feedingNotes;

    @Size(max = 1000, message = "服务心得不能超过 1000 字")
    private String careNotes;

    @AssertTrue(message = "请至少填写一项照护记录")
    public boolean isContentPresent() {
        return hasText(habits) || hasText(feedingNotes) || hasText(careNotes);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
