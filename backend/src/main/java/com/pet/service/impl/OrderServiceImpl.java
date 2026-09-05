package com.pet.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.api.PageResult;
import com.pet.common.api.ResultCode;
import com.pet.common.enums.OrderStatus;
import com.pet.common.enums.PayStatus;
import com.pet.common.exception.BusinessException;
import com.pet.common.geo.OrderGeoIndex;
import com.pet.common.lock.DistributedLock;
import com.pet.common.util.GeoUtil;
import com.pet.dto.HallQuery;
import com.pet.dto.OrderCancelDTO;
import com.pet.dto.OrderCreateDTO;
import com.pet.dto.OrderQuery;
import com.pet.entity.Order;
import com.pet.entity.Pet;
import com.pet.entity.ServiceCategory;
import com.pet.entity.User;
import com.pet.mapper.OrderMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.UserMapper;
import com.pet.security.UserContext;
import com.pet.service.OrderService;
import com.pet.service.ServiceCategoryService;
import com.pet.service.PetService;
import com.pet.service.SitterProfileService;
import com.pet.service.WalletService;
import com.pet.vo.HallOrderVO;
import com.pet.vo.OrderDetailVO;
import com.pet.vo.OrderListVO;
import com.pet.vo.PricePreviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 抢单锁：等待 3 秒（让并发请求排队而不是直接失败），持锁 10 秒兜底防死锁 */
    private static final String GRAB_LOCK_PREFIX = "order:grab:";
    private static final long GRAB_LOCK_WAIT_SECONDS = 3;
    private static final long GRAB_LOCK_LEASE_SECONDS = 10;

    /** GEO 索引重建锁，避免 clear() 与并发支付写入的 add() 交错把订单永久丢掉 */
    private static final String GEO_REBUILD_LOCK_KEY = "lock:geo:order:rebuild";

    /**
     * 一次 GEO 检索最多取回的候选数，与 {@code PageQuery} 的 size 上限一致。
     * <p>
     * GEO 只能按距离排序返回，做不了 offset 也做不了只算 count，所以分页必须在内存里切；
     * 待接单订单超过这个数时 total 会偏小，本期可接受（真实平台该换成 ES 的 geo_distance 查询）。
     */
    private static final int MAX_HALL_CANDIDATES = 500;

    private final PetService petService;
    private final PetMapper petMapper;
    private final UserMapper userMapper;
    private final ServiceCategoryService serviceCategoryService;
    private final WalletService walletService;
    private final SitterProfileService sitterProfileService;
    private final OrderGeoIndex geoIndex;
    private final DistributedLock lock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO create(OrderCreateDTO dto) {
        Long userId = UserContext.userId();
        // 校验宠物归属：不校验就能拿别人的 petId 下单，让陌生接单员上门到别人家
        petService.requireMine(dto.getPetId());
        validateServiceTime(dto.getServiceStart(), dto.getServiceEnd());
        // 计价一律走 previewPrice，全项目只有这一份算法，保证「预览多少就付多少」；
        // 顺带挡掉对已下架或不存在的类别下单
        PricePreviewVO price = serviceCategoryService.previewPrice(dto.getCategoryId(), dto.getServiceStart());

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPetId(dto.getPetId());
        order.setCategoryId(dto.getCategoryId());
        order.setServiceAddress(dto.getServiceAddress());
        order.setAddressLat(dto.getAddressLat());
        order.setAddressLng(dto.getAddressLng());
        order.setServiceStart(dto.getServiceStart());
        order.setServiceEnd(dto.getServiceEnd());
        order.setAmount(price.getAmount());
        order.setCommission(price.getCommission());
        order.setSitterIncome(price.getSitterIncome());
        order.setStatus(OrderStatus.UNPAID.getCode());
        order.setPayStatus(PayStatus.UNPAID.getCode());
        order.setRemark(dto.getRemark());
        save(order);
        return getDetail(order.getId());
    }

    @Override
    public PageResult<OrderListVO> pageMine(OrderQuery query) {
        Page<Order> page = page(query.toPage(), Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, UserContext.userId())
                .eq(query.getStatus() != null, Order::getStatus, query.getStatus())
                .orderByDesc(Order::getId));
        // 不用 PageResult.of(page, mapper)：拼宠物名与类别名需要整页的 id 集合去批量查，逐行转换会 N+1
        List<OrderListVO> records = toListVOs(page.getRecords());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public OrderDetailVO getDetail(Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        Long viewerId = UserContext.userId();
        boolean isOwner = order.getUserId().equals(viewerId);
        boolean isSitter = viewerId.equals(order.getSitterId());
        boolean isAdmin = UserContext.isAdmin();
        if (!isOwner && !isSitter && !isAdmin) {
            throw new BusinessException(ResultCode.ORDER_ACCESS_DENIED);
        }

        List<Order> one = List.of(order);
        Map<Long, ServiceCategory> categories = loadCategories(one);
        Map<Long, Pet> pets = loadPets(one);

        OrderDetailVO vo = new OrderDetailVO();
        fillCommon(vo, order, categories, pets);
        vo.setAddressLat(order.getAddressLat());
        vo.setAddressLng(order.getAddressLng());
        vo.setRemark(order.getRemark());
        vo.setPayTime(order.getPayTime());
        vo.setTakenTime(order.getTakenTime());
        vo.setCheckinTime(order.getCheckinTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setAcceptTime(order.getAcceptTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());

        Pet pet = pets.get(order.getPetId());
        if (pet != null) {
            vo.setPetSpecies(pet.getSpecies());
            vo.setPetBreed(pet.getBreed());
            vo.setPetAvatar(pet.getAvatar());
        }

        // 平台分成只对管理员与该单接单员赋值，对下单用户保持 null —— non_null 下这个键会彻底消失。
        // 这不是漏赋值，别「顺手补全」，理由见 OrderDetailVO#commission 的注释。
        if (isAdmin || isSitter) {
            vo.setCommission(order.getCommission());
            vo.setSitterIncome(order.getSitterIncome());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId) {
        Long userId = UserContext.userId();
        Order order = requireOwned(orderId, userId);

        // markPaid 带 status = 0 AND pay_status = 0 条件，只有影响行数为 1 才允许动钱包。
        // 写成「先查再改」的话，连点两次支付会给同一笔订单冻结两次。
        if (baseMapper.markPaid(orderId) == 0) {
            boolean alreadyPaid = order.getPayStatus() != null && order.getPayStatus() != PayStatus.UNPAID.getCode();
            throw new BusinessException(alreadyPaid ? ResultCode.ORDER_ALREADY_PAID : ResultCode.ORDER_STATUS_ILLEGAL);
        }
        // 余额不足时这里抛异常，上面的 markPaid 会随事务一起回滚，订单退回待支付
        walletService.payOrder(orderId, userId, order.getAmount());

        // GEO 索引写在事务提交之前，若提交失败会留下一条脏数据。可接受：检索侧会拿候选 id
        // 回 MySQL 用 status = 1 二次过滤，脏 id 顶多让某次查询白跑一趟，不会漏单也不会错单。
        geoIndex.add(orderId, order.getAddressLng(), order.getAddressLat());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, OrderCancelDTO dto) {
        Long userId = UserContext.userId();
        Order order = requireOwned(orderId, userId);
        String reason = StrUtil.blankToDefault(dto == null ? null : dto.getReason(), "用户主动取消");

        // 条件更新限定 status IN (0, 1)：已接单之后的取消要走仲裁流程，本期不实现
        if (baseMapper.markCancelled(orderId, reason) == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
        }
        if (order.getPayStatus() != null && order.getPayStatus() == PayStatus.PAID.getCode()) {
            // markRefunded 带 pay_status = 1 条件，是退款的第二道幂等防线：
            // 即使两个取消请求同时越过了 markCancelled，也只有一个能把钱退回去
            if (baseMapper.markRefunded(orderId) == 1) {
                walletService.refundOrder(orderId, userId, order.getAmount());
            }
        }
        geoIndex.remove(orderId);
    }

    @Override
    public PageResult<HallOrderVO> pageHall(HallQuery query) {
        double lng = query.getLng().doubleValue();
        double lat = query.getLat().doubleValue();
        double radiusKm = query.getRadiusKm().doubleValue();

        Map<Long, Double> distances = nearbyByGeo(lng, lat, radiusKm);
        List<Order> pending;
        if (distances == null) {
            pending = nearbyByMysql(lat, lng, radiusKm);
            distances = distanceMap(pending, lat, lng);
        } else {
            pending = pendingInGeoOrder(distances);
        }

        List<Order> slice = pending.stream()
                .skip((query.getPage() - 1) * query.getSize())
                .limit(query.getSize())
                .toList();
        return new PageResult<>(toHallVOs(slice, distances), pending.size(), query.getPage(), query.getSize());
    }

    /**
     * 抢单。<b>刻意不加 @Transactional</b>：整个方法只有一次写库（markTaken 的条件 UPDATE，
     * 单语句本身就是原子的），GEO 移除又是事务外的 Redis 操作。加上事务反而会让「先开事务再抢锁」，
     * 锁在提交前就释放了，白白拉长行锁的持有时间。
     */
    @Override
    public void grab(Long orderId) {
        Long sitterId = UserContext.userId();
        // 资质校验放在锁外：它只读不写，没必要占着订单锁去查一次档案
        sitterProfileService.requireGrabable(sitterId);

        // tryLockAndRun 返回 null 表示没抢到锁。doGrab 成功一定返回 TRUE、失败一定抛异常，
        // 所以 null 只可能来自「锁被别人持有」，不会被业务返回值混淆。
        Boolean grabbed = lock.tryLockAndRun(GRAB_LOCK_PREFIX + orderId,
                GRAB_LOCK_WAIT_SECONDS, GRAB_LOCK_LEASE_SECONDS, () -> doGrab(orderId, sitterId));
        if (grabbed == null) {
            throw new BusinessException(ResultCode.ORDER_ALREADY_TAKEN);
        }
    }

    @Override
    public PageResult<OrderListVO> pageTaken(OrderQuery query) {
        Page<Order> page = page(query.toPage(), Wrappers.<Order>lambdaQuery()
                .eq(Order::getSitterId, UserContext.userId())
                .eq(query.getStatus() != null, Order::getStatus, query.getStatus())
                .orderByDesc(Order::getId));
        List<OrderListVO> records = toListVOs(page.getRecords(), loadNicknames(page.getRecords()), true);
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 订单必须是当前登录用户下的单，否则连「订单不存在」都不必伪装——直接回无权操作。 */
    private Order requireOwned(Long orderId, Long userId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_ACCESS_DENIED);
        }
        return order;
    }

    private void validateServiceTime(LocalDateTime start, LocalDateTime end) {
        if (!start.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.SERVICE_TIME_ILLEGAL);
        }
        if (end != null && !end.isAfter(start)) {
            throw new BusinessException(ResultCode.SERVICE_TIME_ILLEGAL.getCode(), "预约结束时间必须晚于开始时间");
        }
    }

    /** PO + 秒级时间戳 + 6 位随机数，共 22 字符；uk_order_no 兜底，撞号概率可忽略。 */
    private String generateOrderNo() {
        return "PO" + LocalDateTime.now().format(ORDER_NO_TIME) + RandomUtil.randomNumbers(6);
    }

    private List<OrderListVO> toListVOs(List<Order> orders) {
        return toListVOs(orders, Map.of(), false);
    }

    /**
     * @param ownerNicknames 下单用户 id → 昵称
     * @param sitterView     是否接单员视角。只有这个视角才填下单用户昵称与到手金额：
     *                       用户自己的列表里前者就是他本人（噪音），后者是平台分成（泄密）
     */
    private List<OrderListVO> toListVOs(List<Order> orders, Map<Long, String> ownerNicknames, boolean sitterView) {
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, ServiceCategory> categories = loadCategories(orders);
        Map<Long, Pet> pets = loadPets(orders);
        return orders.stream().map(o -> {
            OrderListVO vo = new OrderListVO();
            fillCommon(vo, o, categories, pets);
            if (sitterView) {
                vo.setOwnerNickname(ownerNicknames.get(o.getUserId()));
                vo.setSitterIncome(o.getSitterIncome());
            }
            return vo;
        }).toList();
    }

    /** 列表与详情共用的公共字段填充，详情 VO 继承列表 VO 所以能直接传进来。 */
    private void fillCommon(OrderListVO vo, Order o, Map<Long, ServiceCategory> categories, Map<Long, Pet> pets) {
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setCategoryId(o.getCategoryId());
        vo.setPetId(o.getPetId());
        vo.setServiceAddress(o.getServiceAddress());
        vo.setServiceStart(o.getServiceStart());
        vo.setServiceEnd(o.getServiceEnd());
        vo.setAmount(o.getAmount());
        vo.setStatus(o.getStatus());
        vo.setStatusText(OrderStatus.descOf(o.getStatus()));
        vo.setPayStatus(o.getPayStatus());
        vo.setPayStatusText(PayStatus.descOf(o.getPayStatus()));
        vo.setCreateTime(o.getCreateTime());

        ServiceCategory category = categories.get(o.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
            vo.setUnit(category.getUnit());
        }
        Pet pet = pets.get(o.getPetId());
        if (pet != null) {
            vo.setPetName(pet.getName());
            vo.setPetDeleted(pet.getDeleted() != null && pet.getDeleted() == 1);
        }
    }

    /** 锁内执行的抢单动作。抢到返回 TRUE，其余情况一律抛异常——不返回 false，好让 null 只表示「没抢到锁」。 */
    private Boolean doGrab(Long orderId, Long sitterId) {
        if (getById(orderId) == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (baseMapper.markTaken(orderId, sitterId) == 0) {
            // 影响行数 0 说明订单已离开「待接单」。sitter_id 非空 = 被别人抢走或管理员已指派，
            // 为空 = 主人取消了。两种情况提示语完全不同，混成一条会让接单员以为网络卡了而反复重试。
            Order latest = getById(orderId);
            boolean takenByOther = latest != null && latest.getSitterId() != null;
            throw new BusinessException(takenByOther
                    ? ResultCode.ORDER_ALREADY_TAKEN : ResultCode.ORDER_STATUS_ILLEGAL);
        }
        // 必须放在条件更新成功之后：反过来的话，抢单失败也会把别人还能抢的单从大厅抹掉
        geoIndex.remove(orderId);
        return Boolean.TRUE;
    }

    /** GEO 检索半径内的候选订单 id → 距离（公里），升序；返回 null 表示 Redis 不可用。 */
    private Map<Long, Double> nearbyByGeo(double lng, double lat, double radiusKm) {
        Map<Long, Double> hit = geoIndex.searchNearby(lng, lat, radiusKm, MAX_HALL_CANDIDATES);
        // 索引为空有两种可能：附近确实没单，或者 Redis 被清空 / 刚启动从没建过。
        // 用 MySQL 的待接单数区分，没单就不重建——否则平台空闲时每次进大厅都要白扫一遍表。
        if (hit != null && hit.isEmpty() && geoIndex.size() == 0 && countPending() > 0) {
            rebuildGeoIndex();
            hit = geoIndex.searchNearby(lng, lat, radiusKm, MAX_HALL_CANDIDATES);
        }
        return hit;
    }

    private long countPending() {
        return count(Wrappers.<Order>lambdaQuery().eq(Order::getStatus, OrderStatus.PENDING.getCode()));
    }

    /**
     * 从 MySQL 全量重建 GEO 索引。
     * <p>
     * 加锁是因为 clear() 与并发支付写入的 add() 交错会永久丢掉那几条订单——它们已经支付成功，
     * 却再也不会被任何一次写入重新加进索引，只能等下一次重建。没抢到锁说明别的请求正在重建，
     * 本次直接放弃：这一屏是空列表，用户刷新一次就有了，不值得为此把请求挂住等锁。
     */
    private void rebuildGeoIndex() {
        Integer rebuilt = lock.tryLockAndRun(GEO_REBUILD_LOCK_KEY, 0, 30, () -> {
            List<Order> pending = baseMapper.selectPendingForGeoRebuild();
            geoIndex.clear();
            pending.forEach(o -> geoIndex.add(o.getId(), o.getAddressLng(), o.getAddressLat()));
            return pending.size();
        });
        log.info("重建订单 GEO 索引：{}", rebuilt == null ? "另一请求正在进行，本次跳过" : rebuilt + " 条");
    }

    /** 拿 GEO 候选 id 回 MySQL 用 status = 1 二次过滤，脏 id（事务回滚残留、索引未清干净）到此为止。 */
    private List<Order> pendingInGeoOrder(Map<Long, Double> distances) {
        if (distances.isEmpty()) {
            return List.of();
        }
        List<Order> orders = new ArrayList<>(list(Wrappers.<Order>lambdaQuery()
                .eq(Order::getStatus, OrderStatus.PENDING.getCode())
                .in(Order::getId, distances.keySet())));
        // IN 查询不保证顺序，不重排的话「离我最近」会退化成「id 最小」
        orders.sort(Comparator.comparingDouble(o -> distances.getOrDefault(o.getId(), Double.MAX_VALUE)));
        return orders;
    }

    /**
     * Redis 不可用时的降级路径：扫全部待接单订单，用 Haversine 现算距离，过滤半径后升序返回。
     * <p>
     * 比 GEO 慢一个量级，但大厅不能因为缓存挂了就让接单员看不到单——那时订单还在正常产生，
     * 只是没人能抢，钱全冻在担保里。
     */
    private List<Order> nearbyByMysql(double lat, double lng, double radiusKm) {
        double radiusMeters = radiusKm * 1000;
        return list(Wrappers.<Order>lambdaQuery().eq(Order::getStatus, OrderStatus.PENDING.getCode())).stream()
                .filter(o -> o.getAddressLat() != null && o.getAddressLng() != null)
                .map(o -> Map.entry(o, GeoUtil.distanceMeters(lat, lng,
                        o.getAddressLat().doubleValue(), o.getAddressLng().doubleValue())))
                .filter(e -> e.getValue() <= radiusMeters)
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    /** 降级路径下补一份距离表（公里），让 VO 组装与 GEO 路径共用同一段代码。 */
    private Map<Long, Double> distanceMap(List<Order> orders, double lat, double lng) {
        Map<Long, Double> distances = new HashMap<>();
        for (Order o : orders) {
            if (o.getAddressLat() != null && o.getAddressLng() != null) {
                distances.put(o.getId(), GeoUtil.distanceMeters(lat, lng,
                        o.getAddressLat().doubleValue(), o.getAddressLng().doubleValue()) / 1000);
            }
        }
        return distances;
    }

    /** 大厅列表项。不含下单用户身份与备注，理由见 {@link HallOrderVO} 的类注释。 */
    private List<HallOrderVO> toHallVOs(List<Order> orders, Map<Long, Double> distancesKm) {
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, ServiceCategory> categories = loadCategories(orders);
        Map<Long, Pet> pets = loadPets(orders);
        return orders.stream().map(o -> {
            HallOrderVO vo = new HallOrderVO();
            vo.setId(o.getId());
            vo.setOrderNo(o.getOrderNo());
            vo.setCategoryId(o.getCategoryId());
            vo.setServiceAddress(o.getServiceAddress());
            vo.setAddressLng(o.getAddressLng());
            vo.setAddressLat(o.getAddressLat());
            vo.setServiceStart(o.getServiceStart());
            vo.setServiceEnd(o.getServiceEnd());
            vo.setAmount(o.getAmount());
            vo.setSitterIncome(o.getSitterIncome());
            Double km = distancesKm.get(o.getId());
            if (km != null) {
                vo.setDistanceKm(BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP));
            }
            ServiceCategory category = categories.get(o.getCategoryId());
            if (category != null) {
                vo.setCategoryCode(category.getCode());
                vo.setCategoryName(category.getName());
                vo.setUnit(category.getUnit());
            }
            Pet pet = pets.get(o.getPetId());
            if (pet != null) {
                vo.setPetName(pet.getName());
                vo.setPetSpecies(pet.getSpecies());
            }
            return vo;
        }).toList();
    }

    /** 批量取下单用户昵称；昵称为空时退回用户名，免得列表里出现一片空白。 */
    private Map<Long, String> loadNicknames(List<Order> orders) {
        Set<Long> ids = orders.stream().map(Order::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(Wrappers.<User>lambdaQuery().in(User::getId, ids)).stream()
                .collect(Collectors.toMap(User::getId, u -> StrUtil.blankToDefault(u.getNickname(), u.getUsername())));
    }

    private Map<Long, ServiceCategory> loadCategories(List<Order> orders) {
        Set<Long> ids = orders.stream().map(Order::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        // 空集合必须短路：MyBatis-Plus 的 in() 收到空集合会生成 IN () 导致 SQL 语法错误
        if (ids.isEmpty()) {
            return Map.of();
        }
        return serviceCategoryService.listByIds(ids).stream()
                .collect(Collectors.toMap(ServiceCategory::getId, Function.identity()));
    }

    private Map<Long, Pet> loadPets(List<Order> orders) {
        Set<Long> ids = orders.stream().map(Order::getPetId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return petMapper.selectSnapshots(ids).stream()
                .collect(Collectors.toMap(Pet::getId, Function.identity()));
    }
}
