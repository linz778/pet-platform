package com.pet.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 轨迹点出参。t_order_evidence.track_json 存的就是这个结构的 JSON 数组。
 */
@Data
public class TrackPointVO {

    private BigDecimal lat;

    private BigDecimal lng;

    private LocalDateTime time;
}
