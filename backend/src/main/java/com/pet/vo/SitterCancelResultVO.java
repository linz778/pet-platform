package com.pet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 接单员取消订单结果，让前端明确展示本次是否扣分及剩余信誉分。 */
@Data
@AllArgsConstructor
public class SitterCancelResultVO {

    private boolean creditDeducted;

    private int deductedPoints;

    private int creditScore;
}
