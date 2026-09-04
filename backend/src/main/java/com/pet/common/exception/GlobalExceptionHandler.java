package com.pet.common.exception;

import com.pet.common.api.Result;
import com.pet.common.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常处理器：把各类异常统一转成标准 Result 响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? ResultCode.VALIDATE_FAILED.getMessage() : fieldError.getDefaultMessage();
        return Result.fail(ResultCode.VALIDATE_FAILED.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? ResultCode.VALIDATE_FAILED.getMessage() : fieldError.getDefaultMessage();
        return Result.fail(ResultCode.VALIDATE_FAILED.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        return Result.fail(ResultCode.VALIDATE_FAILED.getCode(), e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(ResultCode.VALIDATE_FAILED.getCode(), "请求体格式错误，请检查提交的 JSON 是否合法");
    }

    /**
     * query 参数缺失 / 格式不匹配。
     * <p>
     * 不加这两个处理器的话它们会落到兜底的 handleOther，返回 500「服务器内部错误」——
     * 明明只是前端少传了 serviceStart 或时间格式不对，却让人以为后端崩了。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必填请求参数: {}", e.getParameterName());
        return Result.fail(ResultCode.VALIDATE_FAILED.getCode(), "缺少必填参数：" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        // 不回显传入的原始值：query 参数里可能带敏感内容，日志和响应都只给参数名
        log.warn("请求参数格式不正确: {}", e.getName());
        return Result.fail(ResultCode.VALIDATE_FAILED.getCode(), "参数格式不正确：" + e.getName());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突: {}", e.getMessage());
        return Result.fail(ResultCode.VALIDATE_FAILED.getCode(), "数据已存在，请勿重复提交");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.FAILED.getCode(), "服务器内部错误，请稍后重试");
    }
}
