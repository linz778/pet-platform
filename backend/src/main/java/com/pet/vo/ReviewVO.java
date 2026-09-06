package com.pet.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价。
 * <p>
 * 匿名评价不下发 fromUserId 与 fromNickname——只藏昵称的话，前端拿 id 调一次用户接口就还原了身份。
 * 评价人自己看自己那条（{@code mine = true}）时照常下发，他本来就知道是谁写的。
 */
@Data
public class ReviewVO {

    private Long id;

    private Long orderId;

    private String orderNo;

    private String categoryName;

    /** 评价人，匿名时为 null（non_null 下键消失） */
    private Long fromUserId;

    private String fromNickname;

    private Long toUserId;

    /** 星级 1-5 */
    private Integer rating;

    private String content;

    private Boolean anonymous;

    /** 是否当前登录用户写的那条，前端据此区分「我评的」与「评我的」 */
    private Boolean mine;

    private LocalDateTime createTime;
}
