package com.pet.service.impl;

import com.pet.common.api.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.config.FileProperties;
import com.pet.service.FileService;
import com.pet.vo.FileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 本地磁盘图片存储。
 * <p>
 * 本期不接对象存储（MinIO/OSS），演示规模下本地磁盘足够，且避免引入额外依赖与外部账号。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    /** bizType 白名单。同时充当目录名，因此必须穷举校验，杜绝 ../ 之类的路径穿越。 */
    private static final Set<String> ALLOWED_BIZ_TYPES = Set.of("pet", "evidence", "cert", "avatar", "common");

    /** 扩展名 → 归一化后缀。jpg 与 jpeg 统一存为 jpg，避免同一格式出现两种目录后缀。 */
    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
            "jpg", "jpg",
            "jpeg", "jpg",
            "png", "png",
            "webp", "webp"
    );

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 文件头魔数最多需要读 12 字节（WEBP 的 RIFF....WEBP） */
    private static final int MAGIC_LENGTH = 12;

    private final FileProperties fileProperties;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Override
    public FileVO upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_EMPTY);
        }
        if (file.getSize() > fileProperties.getMaxSize()) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED);
        }

        String type = bizType == null ? "common" : bizType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_BIZ_TYPES.contains(type)) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "不支持的业务分类：" + bizType);
        }

        String extension = resolveExtension(file.getOriginalFilename());
        Path root = Paths.get(fileProperties.getDir()).toAbsolutePath().normalize();
        Path target = root.resolve(type)
                .resolve(LocalDate.now().format(DATE_DIR))
                .resolve(UUID.randomUUID().toString().replace("-", "") + "." + extension);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            // 扩展名可以随意改，落盘后再按文件头魔数复核一次真实格式
            if (!isImage(target)) {
                Files.deleteIfExists(target);
                throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
            }
        } catch (IOException e) {
            log.error("文件上传失败: {}", target, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
        }

        FileVO vo = new FileVO();
        vo.setUrl(contextPath + fileProperties.getUrlPrefix() + "/" + root.relativize(target).toString().replace('\\', '/'));
        vo.setName(target.getFileName().toString());
        vo.setSize(file.getSize());
        return vo;
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
        String ext = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
        String normalized = ALLOWED_EXTENSIONS.get(ext);
        if (normalized == null) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
        return normalized;
    }

    private boolean isImage(Path path) throws IOException {
        byte[] head = new byte[MAGIC_LENGTH];
        int read;
        try (InputStream in = Files.newInputStream(path)) {
            read = in.readNBytes(head, 0, MAGIC_LENGTH);
        }
        if (read < 3) {
            return false;
        }
        // JPEG: FF D8 FF
        if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG: 89 'P' 'N' 'G'
        if (read >= 4 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            return true;
        }
        // WEBP: 'RIFF' .... 'WEBP'
        return read >= MAGIC_LENGTH
                && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
    }
}
