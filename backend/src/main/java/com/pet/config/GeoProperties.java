package com.pet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 绑定 application.yml 中已存在的 {@code pet-platform.geo} 配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "pet-platform.geo")
public class GeoProperties {

    /**
     * 到达打卡允许的经纬度误差范围（米）。
     * <p>
     * 室内 WiFi 定位误差常达 50-500 米，本地演示若打卡总失败可调到 500-1000。
     */
    private int checkInRadius = 200;
}
