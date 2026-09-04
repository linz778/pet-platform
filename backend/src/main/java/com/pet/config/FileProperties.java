package com.pet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地文件存储配置（{@code pet-platform.file}）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "pet-platform.file")
public class FileProperties {

    /**
     * 存储根目录。相对路径按<b>进程工作目录</b>解析：从 backend/ 启动 mvn spring-boot:run
     * 落在 backend/uploads/，从仓库根 java -jar 落在 Petplatform/uploads/。
     * 两处 .gitignore 都已覆盖；要固定位置请用 PET_FILE_DIR 指定绝对路径。
     */
    private String dir = "./uploads";

    /** 对外访问前缀，须与 WebMvcConfig 的资源映射路径和鉴权白名单保持一致。 */
    private String urlPrefix = "/uploads";

    /** 单文件字节上限，须 ≤ spring.servlet.multipart.max-file-size。 */
    private long maxSize = 10 * 1024 * 1024L;
}
