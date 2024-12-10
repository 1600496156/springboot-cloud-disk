package com.mhc.springbootclouddisk.common.exception;

import com.mhc.springbootclouddisk.common.response.CloudDiskResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public CloudDiskResult runtimeExceptionHandler(RuntimeException e) {
        log.error("全局异常管理器 - 参数校验捕获异常：{}",e.getMessage());
        return new CloudDiskResult("error",500,e.getMessage(),null);
    }
}
