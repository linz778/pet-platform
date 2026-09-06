package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.common.api.PageQuery;
import com.pet.common.api.PageResult;
import com.pet.dto.ReviewSaveDTO;
import com.pet.entity.Review;
import com.pet.vo.ReviewVO;

import java.util.List;

/**
 * 订单完成后的双向评价：下单用户评接单员，接单员评下单用户。
 * <p>
 * 独立于 {@link OrderService}：评价是订单结算之后才发生的事，不参与任何状态流转，
 * 也不冗余「是否已评价」标记到 t_order——那个状态由本表的 uk_order_from 派生。
 */
public interface ReviewService extends IService<Review> {

    /**
     * 提交评价，返回落库后的评价。
     * <p>
     * 不叫 save：IService 已经有一个 save(Review)，同名重载会让「存实体」和「提交入参」在调用点分不清。
     * <p>
     * 只有该单的下单用户或接单员能评，被评价人由订单归属反推；订单必须已完成（验收结算后），
     * 同一人对同一单只能评一次，重复提交抛 REVIEW_ALREADY_EXISTS。
     */
    ReviewVO submit(ReviewSaveDTO dto);

    /** 某单的双向评价，按提交先后。下单用户、该单接单员与管理员可见，其余人抛 ORDER_ACCESS_DENIED。 */
    List<ReviewVO> listByOrder(Long orderId);

    /** 我收到的评价分页，按时间倒序。 */
    PageResult<ReviewVO> pageReceived(PageQuery query);
}
