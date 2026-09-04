package com.pet.controller;

import com.pet.common.api.Result;
import com.pet.service.FileService;
import com.pet.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件", description = "图片上传")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传图片", description = "支持 jpg/jpeg/png/webp，单文件上限见 pet-platform.file.max-size")
    @PostMapping("/upload")
    public Result<FileVO> upload(@RequestPart("file") MultipartFile file,
                                @RequestParam(defaultValue = "common") String bizType) {
        return Result.success(fileService.upload(file, bizType));
    }
}
