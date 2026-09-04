package com.pet.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一 LocalDateTime 的 JSON 序列化格式为 {@code yyyy-MM-dd HH:mm:ss}。
 * <p>
 * application.yml 里的 {@code spring.jackson.date-format} 只作用于 java.util.Date，
 * 对 JSR-310 类型完全无效：默认出参是 ISO-8601 带 T 的 {@code 2026-09-04T12:00:00}，
 * 而入参收到前端 el-date-picker 发出的空格分隔串会直接抛 InvalidFormatException，
 * 前端只能看到一句毫无信息量的「请求体格式错误」。
 * <p>
 * 注意：GET query 参数上的 LocalDateTime 走的是 ConversionService 而非 ObjectMapper，
 * 本配置管不到，需在参数上单独加 {@code @DateTimeFormat(pattern = DATE_TIME_PATTERN)}。
 */
@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        return builder -> builder
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(formatter))
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
    }
}
