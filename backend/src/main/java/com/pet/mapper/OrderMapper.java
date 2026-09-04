package com.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 订单数据层。
 * <p>
 * 所有状态流转一律用<b>带前置状态条件的 UPDATE 并判断影响行数</b>，而不是「先查再改」：
 * 影响行数为 0 就说明订单已被并发操作推进过，调用方必须放弃后续副作用（扣款、结算、GEO 写入）。
 * 这是支付、抢单、验收三处幂等性的唯一防线。
 * <p>
 * 手写 SQL 绕过了 MyBatis-Plus 的逻辑删除拦截器与 MetaObjectHandler，
 * 因此每条语句都必须自带 {@code deleted = 0} 与 {@code update_time = NOW()}。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /** 待支付 → 待接单，同时置支付状态与支付时间。 */
    @Update("UPDATE t_order SET status = 1, pay_status = 1, pay_time = NOW(), update_time = NOW() "
            + "WHERE id = #{id} AND status = 0 AND pay_status = 0 AND deleted = 0")
    int markPaid(@Param("id") Long id);

    /** 已支付 → 已退款（取消已支付订单时紧随 markCancelled 调用）。 */
    @Update("UPDATE t_order SET pay_status = 3, update_time = NOW() "
            + "WHERE id = #{id} AND pay_status = 1 AND deleted = 0")
    int markRefunded(@Param("id") Long id);

    /** 待接单 → 已接单。抢单与管理员人工指派共用，返回 0 表示已被他人抢先。 */
    @Update("UPDATE t_order SET status = 2, sitter_id = #{sitterId}, taken_time = NOW(), update_time = NOW() "
            + "WHERE id = #{id} AND status = 1 AND deleted = 0")
    int markTaken(@Param("id") Long id, @Param("sitterId") Long sitterId);

    /** 已接单 → 服务中（到达定位打卡）。 */
    @Update("UPDATE t_order SET status = 3, checkin_time = NOW(), update_time = NOW() "
            + "WHERE id = #{id} AND status = 2 AND deleted = 0")
    int markCheckedIn(@Param("id") Long id);

    /** 服务中 → 待验收（接单员标记服务完成）。 */
    @Update("UPDATE t_order SET status = 4, finish_time = NOW(), update_time = NOW() "
            + "WHERE id = #{id} AND status = 3 AND deleted = 0")
    int markFinished(@Param("id") Long id);

    /**
     * 待验收 → 已完成并结算。
     * <p>
     * <b>只有返回 1 才允许触发钱包结算</b>：用户连点两次验收、或网络重试导致重复请求时，
     * 第二次会因 status 已变为 5 而返回 0，从而避免给接单员和平台各入账两次。
     */
    @Update("UPDATE t_order SET status = 5, pay_status = 2, accept_time = NOW(), update_time = NOW() "
            + "WHERE id = #{id} AND status = 4 AND pay_status = 1 AND deleted = 0")
    int markAccepted(@Param("id") Long id);

    /** 待支付 / 待接单 → 已取消。已接单之后不允许取消（需走仲裁流程，本期未实现）。 */
    @Update("UPDATE t_order SET status = 6, cancel_time = NOW(), cancel_reason = #{reason}, update_time = NOW() "
            + "WHERE id = #{id} AND status IN (0, 1) AND deleted = 0")
    int markCancelled(@Param("id") Long id, @Param("reason") String reason);

    /** Redis GEO 索引懒重建用：取出全部待接单订单的坐标（仅填充 id/addressLng/addressLat）。 */
    @Select("SELECT id, address_lng, address_lat FROM t_order WHERE status = 1 AND deleted = 0")
    List<Order> selectPendingForGeoRebuild();
}
