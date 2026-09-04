package com.petplatform.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "pet-platform.jwt")
public class JwtProperties {

    /** 签名密钥 */
    private String secret;
    /** 过期时间（分钟） */
    private long expireMinutes = 720;
    /** 请求头名称 */
    private String header = "Authorization";
    /** token 前缀 */
    private String prefix = "Bearer ";
}
