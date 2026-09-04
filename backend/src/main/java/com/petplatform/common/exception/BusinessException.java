package com.petplatform.common.exception;

import com.petplatform.common.api.ResultCode;
import lombok.Getter;

/**
 * 业务异常，由全局异常处理器统一捕获转为标准响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAILED.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
