package com.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.entity.Arbitration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface ArbitrationMapper extends BaseMapper<Arbitration> {

    /** 待审核 → 已处理；条件更新防止两名管理员重复退款。 */
    @Update("UPDATE t_arbitration SET status = 2, result = #{result}, refund_amount = #{refundAmount}, "
            + "handler_id = #{handlerId}, update_time = NOW() "
            + "WHERE id = #{id} AND status = 0 AND deleted = 0")
    int markDecided(@Param("id") Long id, @Param("handlerId") Long handlerId,
                    @Param("result") String result, @Param("refundAmount") BigDecimal refundAmount);
}
