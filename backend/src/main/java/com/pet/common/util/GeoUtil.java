package com.pet.common.util;

/**
 * 地理距离工具。
 */
public final class GeoUtil {

    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private GeoUtil() {
    }

    /**
     * Haversine 公式计算两点间地表距离（米）。
     * <p>
     * ⚠️ 参数顺序是<b>纬度在前</b>，与 Redis GEO（GEOADD/GEORADIUS）和高德地图 Marker
     * 的「经度在前」约定相反。写反不会抛异常，只会表现为大厅永远查不到订单、
     * 打卡永远提示距离超限，因此 GeoUtilTest 专门锁死了这一约定。
     *
     * @param lat1 起点纬度
     * @param lng1 起点经度
     * @param lat2 终点纬度
     * @param lng2 终点经度
     */
    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double deltaLat = radLat2 - radLat1;
        double deltaLng = Math.toRadians(lng2 - lng1);

        double h = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(h));
    }
}
