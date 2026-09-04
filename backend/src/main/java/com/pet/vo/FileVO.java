package com.pet.vo;

import lombok.Data;

/**
 * 文件上传结果。
 */
@Data
public class FileVO {

    /** 可直接用于 img src 的访问路径，形如 /api/uploads/evidence/20260904/xxx.jpg */
    private String url;

    /** 存储文件名（UUID 重命名后，不含用户上传的原始名，避免路径字符与 XSS） */
    private String name;

    /** 字节数 */
    private Long size;
}
