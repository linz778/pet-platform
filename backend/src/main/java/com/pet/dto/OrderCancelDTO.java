package com.pet.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 取消订单入参。原因可空，服务端会补一个默认值，避免 cancel_reason 留白看不出是谁、为什么取消的。
 */
@Data
public class OrderCancelDTO {

    @Size(max = 255, message = "取消原因不能超过 255 字")
    private String reason;
}
