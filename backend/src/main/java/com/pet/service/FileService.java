package com.pet.service;

import com.pet.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * 保存图片到本地磁盘。
     *
     * @param file    上传的图片
     * @param bizType 业务分类，决定存储子目录，取值见实现类白名单
     */
    FileVO upload(MultipartFile file, String bizType);
}
