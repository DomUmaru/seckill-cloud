package com.seckill.common.exception;


import com.seckill.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理非法参数异常
     * @param e 异常
     * @return Result
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        //log.warn("参数异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 处理兜底异常
     * @param e 异常
     * @return Result
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        //log.error("系统异常", e);
        return Result.error("系统繁忙，请稍后再试");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return Result.error(400,"少传参了");
    }
}
