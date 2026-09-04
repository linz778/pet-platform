package com.petplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI petPlatformOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("宠物日常上门服务系统平台 API")
                .description("用户端 / 接单员端 / 管理端 接口文档")
                .version("v0.0.1")
                .contact(new Contact().name("PetPlatform Team")));
    }
}
