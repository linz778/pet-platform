package com.pet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 点按爪印后的最新状态与计数。 */
@Data
@AllArgsConstructor
public class CommunityLikeVO {
    private boolean liked;
    private int likeCount;
}
