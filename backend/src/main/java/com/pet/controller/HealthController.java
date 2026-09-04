package com.pet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
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
@Slf4j
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

    // 该端点免登录公开：失败只返回 DOWN，异常详情写日志，避免泄漏主机/驱动等内部信息。
    private String checkMysql() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            log.warn("MySQL 健康检查失败: {}", e.getMessage());
            return "DOWN";
        }
    }

    private String checkRedis() {
        RedisConnectionFactory factory = stringRedisTemplate.getConnectionFactory();
        if (factory == null) {
            return "DOWN";
        }
        try (RedisConnection conn = factory.getConnection()) {
            return conn.ping() != null ? "UP" : "DOWN";
        } catch (Exception e) {
            log.warn("Redis 健康检查失败: {}", e.getMessage());
            return "DOWN";
        }
    }
}
