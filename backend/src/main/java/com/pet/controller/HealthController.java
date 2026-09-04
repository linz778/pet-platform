package com.pet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查：用于快速验证 Web / MySQL / Redis 是否连通。
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate stringRedisTemplate;

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("app", "UP");
        result.put("mysql", checkMysql());
        result.put("redis", checkRedis());
        return result;
    }

    private String checkMysql() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN: " + e.getMessage();
        }
    }

    private String checkRedis() {
        try {
            String pong = stringRedisTemplate.getConnectionFactory().getConnection().ping();
            return pong != null ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN: " + e.getMessage();
        }
    }
}
