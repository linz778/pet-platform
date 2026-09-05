package com.pet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 散步轨迹上传（户外散步类服务）。
 * <p>
 * 一次上传一条轨迹记录；同一单可以有多条（分段遛），所以服务端只做插入不做覆盖。
 * 轨迹不参与「服务完成」的清单校验，缺了也能 finish——不是每个类别都有轨迹。
 */
@Data
public class TrackSaveDTO {

    /** 上限 2000 点：按 5 秒一个点算能覆盖近 3 小时，再长只会把 TEXT 列撑大 */
    @Valid
    @NotEmpty(message = "轨迹点不能为空")
    @Size(max = 2000, message = "轨迹点过多，请分段上传")
    private List<TrackPointDTO> points;

    @Size(max = 255, message = "备注不能超过 255 字")
    private String remark;
}
