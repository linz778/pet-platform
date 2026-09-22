package com.pet.integration;

import com.pet.PetPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PetPlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlatformIT {

    @Autowired private TestRestTemplate http;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StringRedisTemplate redis;

    @Test
    void realApiUsesMysqlAndRedisAndEnforcesRoles() {
        Map<?, ?> health = http.getForObject("/health", Map.class);
        assertThat(health.get("mysql")).isEqualTo("UP");
        assertThat(health.get("redis")).isEqualTo("UP");

        String redisKey = "integration-test:" + UUID.randomUUID();
        try {
            redis.opsForValue().set(redisKey, "ok");
            assertThat(redis.opsForValue().get(redisKey)).isEqualTo("ok");
        } finally {
            redis.delete(redisKey);
        }

        String username = "it" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Map<?, ?> registered = http.postForObject("/auth/register", Map.of(
                "username", username, "password", "test123456", "phone", "13900000000", "role", "USER"
        ), Map.class);
        assertThat(registered.get("code")).isEqualTo(200);
        String token = (String) ((Map<?, ?>) registered.get("data")).get("token");
        assertThat(token).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_user WHERE username = ?", Long.class, username))
                .isEqualTo(1L);

        Map<?, ?> unauthenticated = http.getForObject("/user/me", Map.class);
        assertThat(unauthenticated.get("code")).isEqualTo(401);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        Map<?, ?> profile = http.exchange("/user/me", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class).getBody();
        assertThat(profile.get("code")).isEqualTo(200);
        assertThat(((Map<?, ?>) profile.get("data")).get("username")).isEqualTo(username);

        Map<?, ?> forbidden = http.exchange("/admin/dashboard", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class).getBody();
        assertThat(forbidden.get("code")).isEqualTo(403);

        Map<?, ?> catalog = http.exchange("/service-category/list", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class).getBody();
        assertThat(catalog.get("code")).isEqualTo(200);
        assertThat((List<?>) catalog.get("data")).hasSizeGreaterThan(0);
    }
}
