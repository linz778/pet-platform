package com.pet.config;

import com.pet.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final FileProperties fileProperties;

    /**
     * 无需登录即可访问的路径（相对于 context-path /api）。
     * <p>
     * 上传目录 {@code /uploads/**} 不在此列也不需要在此列：静态资源由 ResourceHttpRequestHandler
     * 处理，而 AuthInterceptor 对非 HandlerMethod 的处理器一律直接放行。
     */
    private static final String[] WHITE_LIST = {
            "/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/doc.html",
            "/error",
            "/health"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(WHITE_LIST);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /** 把本地上传目录暴露为静态资源，供前端 img 直接引用。 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(fileProperties.getDir()).toAbsolutePath().normalize();
        try {
            // 必须先建目录：toUri() 只对已存在的目录追加结尾斜杠，缺斜杠会让资源解析失败、图片全部 404
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录: " + root, e);
        }
        String location = root.toUri().toString();
        log.info("上传目录映射 {}/** -> {}", fileProperties.getUrlPrefix(), location);
        registry.addResourceHandler(fileProperties.getUrlPrefix() + "/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
