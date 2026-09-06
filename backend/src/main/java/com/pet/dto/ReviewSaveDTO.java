package com.pet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交评价入参。
 * <p>
 * 刻意不含 toUserId：评谁由服务端按订单归属反推（下单用户评接单员、接单员评下单用户），
 * 前端能传被评价人的话，就能给任意用户刷差评。
 */
@Data
public class ReviewSaveDTO {

    @NotNull(message = "缺少订单")
    private Long orderId;

    @NotNull(message = "请选择星级")
    @Min(value = 1, message = "星级最低 1 星")
    @Max(value = 5, message = "星级最高 5 星")
    private Integer rating;

    @Size(max = 500, message = "评价内容不能超过 500 字")
    private String content;

    /** 匿名后对方只看到「匿名用户」，评价人 id 与昵称都不下发 */
    private Boolean anonymous = false;
}
