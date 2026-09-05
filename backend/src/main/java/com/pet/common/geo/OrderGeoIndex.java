package com.pet.common.geo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.GeoOrder;
import org.redisson.api.GeoUnit;
import org.redisson.api.RGeo;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 待接单订单的 Redis GEO 索引，供接单大厅按距离检索附近订单。
 * <p>
 * 索引只是<b>加速用的缓存</b>，不是数据源：所有写操作失败都只记 warn 不抛异常，
 * Redis 挂掉时支付、取消、抢单必须照常成功（MySQL 才是订单状态的唯一真相）。
 * 检索侧会拿候选 id 回 MySQL 用 {@code status = 1} 二次过滤，脏 id 无害。
 * <p>
 * 坐标顺序是<b>经度在前</b>（GEOADD lng lat member），与 {@link com.pet.common.util.GeoUtil}
 * 的纬度在前相反，写反不报错、只表现为大厅永远空列表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderGeoIndex {

    /** 待接单订单坐标集合，member 为订单 id 的十进制字符串 */
    public static final String KEY = "geo:order:pending";

    private final RedissonClient redissonClient;

    /**
     * 必须用 StringCodec：Redisson 默认编解码器会把 member 序列化成二进制，
     * redis-cli 里看到的是一串乱码，Phase 4 的检索结果也没法直接 Long.parseLong。
     */
    private RGeo<String> geo() {
        return redissonClient.getGeo(KEY, StringCodec.INSTANCE);
    }

    /** 订单进入待接单状态（支付成功）时写入。 */
    public void add(Long orderId, BigDecimal lng, BigDecimal lat) {
        if (orderId == null || lng == null || lat == null) {
            return;
        }
        try {
            geo().add(lng.doubleValue(), lat.doubleValue(), String.valueOf(orderId));
        } catch (Exception e) {
            log.warn("写入订单 GEO 索引失败，orderId={}: {}", orderId, e.getMessage());
        }
    }

    /** 订单离开待接单状态（被抢单 / 人工指派 / 取消）时移除。 */
    public void remove(Long orderId) {
        if (orderId == null) {
            return;
        }
        try {
            geo().remove(String.valueOf(orderId));
        } catch (Exception e) {
            log.warn("移除订单 GEO 索引失败，orderId={}: {}", orderId, e.getMessage());
        }
    }

    /** 当前索引条数，仅用于懒重建判定与运维排查；Redis 不可用时返回 -1（区别于「索引为空」的 0）。 */
    public int size() {
        try {
            return geo().size();
        } catch (Exception e) {
            log.warn("读取订单 GEO 索引条数失败: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * 按距离升序检索附近的待接单订单。
     * <p>
     * 用 {@code radiusWithDistance} 而不是 {@code search(GeoSearchArgs)}：后者发的是 GEOSEARCH，
     * Redis 6.2 才有，本机的 Redis 5.0.14 会直接回 {@code unknown command}。
     *
     * @return 订单 id → 距离（公里），已按距离升序；<b>Redis 不可用时返回 null</b>，
     *         与「附近确实没有待接单订单」的空 Map 区分开，调用方据此回落到 MySQL 全量扫描
     */
    public Map<Long, Double> searchNearby(double lng, double lat, double radiusKm, int limit) {
        try {
            Map<String, Double> raw = geo().radiusWithDistance(
                    lng, lat, radiusKm, GeoUnit.KILOMETERS, GeoOrder.ASC, limit);
            Map<Long, Double> ordered = new LinkedHashMap<>();
            raw.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(e -> {
                        Long orderId = parseOrderId(e.getKey());
                        if (orderId != null) {
                            ordered.put(orderId, e.getValue());
                        }
                    });
            return ordered;
        } catch (Exception e) {
            log.warn("检索订单 GEO 索引失败，将回落到 MySQL 扫描: {}", e.getMessage());
            return null;
        }
    }

    /** 懒重建前清空整个索引。返回是否真的删掉了 key（key 本来不存在时为 false）。 */
    public boolean clear() {
        try {
            return geo().delete();
        } catch (Exception e) {
            log.warn("清空订单 GEO 索引失败: {}", e.getMessage());
            return false;
        }
    }

    /** 索引里可能残留历史脏 member（例如手工 redis-cli 写入），解析不出 id 就跳过而不是让整个检索失败。 */
    private Long parseOrderId(String member) {
        try {
            return Long.parseLong(member);
        } catch (NumberFormatException e) {
            log.warn("订单 GEO 索引存在无法解析的 member，已跳过: {}", member);
            return null;
        }
    }
}
