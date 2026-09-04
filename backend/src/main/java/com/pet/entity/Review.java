package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pet.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单评价（双向：用户评接单员、接单员评用户）。
 * <p>
 * 唯一键 uk_order_from(order_id, from_user_id) 保证同一人对同一单只能评一次。
 * 订单是否已评价由此表派生，不在 t_order 上冗余标记字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_review")
public class Review extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long fromUserId;

    private Long toUserId;

    /** 星级 1-5 */
    private Integer rating;

    private String content;

    /** 0=实名 1=匿名 */
    private Integer anonymous;
}
