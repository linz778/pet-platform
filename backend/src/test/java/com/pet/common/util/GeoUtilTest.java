package com.pet.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 锁死 {@link GeoUtil#distanceMeters} 的「纬度在前」参数顺序。
 * <p>
 * 这个约定与 Redis GEO、高德地图 Marker 的「经度在前」相反，写反了不会抛异常，
 * 只会表现为接单大厅永远查不到订单、打卡永远提示距离超限，排查成本极高。
 */
class GeoUtilTest {

    /** 1 度经线长度 = 2πR/360，R 取 6371km */
    private static final double ONE_DEGREE_MERIDIAN_METERS = 111_183d;

    @Test
    @DisplayName("纬度差 1 度约 111 公里，与所在经度无关")
    void oneDegreeOfLatitude() {
        assertThat(GeoUtil.distanceMeters(31.0, 121.0, 32.0, 121.0))
                .isCloseTo(ONE_DEGREE_MERIDIAN_METERS, within(1000d));
        assertThat(GeoUtil.distanceMeters(-45.0, 10.0, -44.0, 10.0))
                .isCloseTo(ONE_DEGREE_MERIDIAN_METERS, within(1000d));
    }

    /**
     * 这条用例是参数顺序的守门员：1 度纬线的长度按 cos(纬度) 收缩。
     * 若把签名误改成经度在前，第二个调用会被解释成「纬度差 1 度」而得到 111 公里，断言立刻失败。
     */
    @Test
    @DisplayName("经度差 1 度的距离随纬度按余弦收缩")
    void oneDegreeOfLongitudeShrinksWithLatitude() {
        double atEquator = GeoUtil.distanceMeters(0.0, 100.0, 0.0, 101.0);
        double atSixtyNorth = GeoUtil.distanceMeters(60.0, 100.0, 60.0, 101.0);

        assertThat(atEquator).isCloseTo(ONE_DEGREE_MERIDIAN_METERS, within(1000d));
        assertThat(atSixtyNorth).isCloseTo(ONE_DEGREE_MERIDIAN_METERS * 0.5, within(1000d));
    }

    @Test
    @DisplayName("上海人民广场到北京约 1067 公里")
    void shanghaiToBeijing() {
        double distance = GeoUtil.distanceMeters(31.2304, 121.4737, 39.9042, 116.4074);
        assertThat(distance).isBetween(1_050_000d, 1_090_000d);
    }

    @Test
    @DisplayName("同一点距离为 0，且 A→B 与 B→A 相等")
    void zeroAndSymmetric() {
        assertThat(GeoUtil.distanceMeters(31.2304, 121.4737, 31.2304, 121.4737))
                .isCloseTo(0d, within(0.001d));

        double forward = GeoUtil.distanceMeters(31.2304, 121.4737, 31.1944, 121.3190);
        double backward = GeoUtil.distanceMeters(31.1944, 121.3190, 31.2304, 121.4737);
        assertThat(forward).isCloseTo(backward, within(0.001d));
        // 人民广场 → 虹桥火车站，约 15 公里，正好落在打卡半径校验的典型量级之外
        assertThat(forward).isBetween(13_000d, 17_000d);
    }
}
