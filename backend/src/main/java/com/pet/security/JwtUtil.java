package com.pet.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成与解析。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties jwtProperties;

    @PostConstruct
    void warnIfDefaultSecret() {
        String secret = jwtProperties.getSecret();
        if (secret != null && secret.contains("change-me-in-prod")) {
            log.warn("JWT 正在使用内置默认密钥，生产环境务必通过环境变量 JWT_SECRET 覆盖！");
        }
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(LoginUser user) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + jwtProperties.getExpireMinutes() * 60_000);
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_ROLE, user.getRole())
                .issuedAt(now)
                .expiration(expire)
                .signWith(key())
                .compact();
    }

    /** 解析 token，失败返回 null。 */
    public LoginUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            LoginUser user = new LoginUser();
            user.setUserId(Long.valueOf(claims.getSubject()));
            user.setUsername(claims.get(CLAIM_USERNAME, String.class));
            user.setRole(claims.get(CLAIM_ROLE, String.class));
            return user;
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public JwtProperties getProperties() {
        return jwtProperties;
    }
}
