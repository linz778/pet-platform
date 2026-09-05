package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.common.api.PageResult;
import com.pet.dto.OrderCancelDTO;
import com.pet.dto.OrderCreateDTO;
import com.pet.dto.OrderQuery;
import com.pet.entity.Order;
import com.pet.vo.OrderDetailVO;
import com.pet.vo.OrderListVO;

public interface OrderService extends IService<Order> {

    /**
     * 下单。金额由服务端按下单时刻的服务类别规则算出，前端传什么都不会被采信。
     * <p>
     * 落库即为「待支付」，不动钱包、不写 GEO 索引——那两件事都发生在支付成功之后。
     */
    OrderDetailVO create(OrderCreateDTO dto);

    /** 当前登录用户的订单分页，按 id 倒序。status 为空表示全部。 */
    PageResult<OrderListVO> pageMine(OrderQuery query);

    /**
     * 订单详情，下单用户 / 该单接单员 / 管理员三方可见，其余人抛 ORDER_ACCESS_DENIED。
     * <p>
     * 平台抽成与接单员到手金额只对后两者返回，详见 {@link com.pet.vo.OrderDetailVO#getCommission()}。
     */
    OrderDetailVO getDetail(Long orderId);

    /**
     * 支付（模拟）：订单转待接单，下单用户的余额转入冻结（平台担保），并写入 GEO 索引。
     * <p>
     * 重复支付抛 ORDER_ALREADY_PAID，且余额只会被扣一次。
     */
    void pay(Long orderId);

    /**
     * 取消订单。仅待支付 / 待接单可取消；已支付的会全额退回余额并移出 GEO 索引。
     */
    void cancel(Long orderId, OrderCancelDTO dto);
}
