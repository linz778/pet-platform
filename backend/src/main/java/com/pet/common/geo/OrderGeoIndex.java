package com.pet.common.geo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RGeo;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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

    /** 当前索引条数，仅用于懒重建判定与运维排查。 */
    public int size() {
        try {
            return geo().size();
        } catch (Exception e) {
            log.warn("读取订单 GEO 索引条数失败: {}", e.getMessage());
            return -1;
        }
    }
}
