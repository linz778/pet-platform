package com.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.common.api.PageResult;
import com.pet.dto.HallQuery;
import com.pet.dto.OrderCancelDTO;
import com.pet.dto.OrderCreateDTO;
import com.pet.dto.OrderQuery;
import com.pet.entity.Order;
import com.pet.vo.HallOrderVO;
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

    /**
     * 接单大厅：以接单员当前坐标为圆心检索附近的待接单订单，按距离升序分页。
     * <p>
     * 候选来自 Redis GEO 索引，再回 MySQL 用 {@code status = 1} 二次过滤，
     * 因此索引里的脏数据不会漏到列表里；索引为空时会从 MySQL 懒重建一次。
     * Redis 整个不可用时退回 MySQL 全量扫描 + Haversine 计算，大厅依然可用，只是慢一些。
     */
    PageResult<HallOrderVO> pageHall(HallQuery query);

    /**
     * 抢单：待接单 → 已接单，并把订单移出 GEO 索引。
     * <p>
     * 分布式锁挡住同一瞬间的并发请求，{@code markTaken} 的 {@code status = 1} 条件挡住一切漏网的重复，
     * 两层加起来保证一单只有一个 sitter_id。被抢先抛 ORDER_ALREADY_TAKEN，
     * 订单已取消抛 ORDER_STATUS_ILLEGAL，资质未过审或暂停接单抛 1005 / 1006。
     */
    void grab(Long orderId);

    /** 当前登录接单员抢到的订单分页，按 id 倒序；status 为空表示全部。 */
    PageResult<OrderListVO> pageTaken(OrderQuery query);

    /**
     * 验收：待验收 → 已完成，并在同一事务里结算担保资金（接单员到手 + 平台抽成入账）。
     * <p>
     * 只有下单用户本人能验收。{@code markAccepted} 的 {@code status = 4 AND pay_status = 1} 条件
     * 是结算的唯一幂等防线，返回 0 抛 ORDER_STATUS_ILLEGAL，钱绝不会结算两次。
     */
    void accept(Long orderId);
}
