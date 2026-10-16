package com.family.medicinecabinet.exception;

import com.family.medicinecabinet.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理药品不存在异常
     */
    @ExceptionHandler(MedicineNotFoundException.class)
    public Result<?> handleNotFound(MedicineNotFoundException e) {
        log.warn("资源不存在: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理库存不足异常
     */
    @ExceptionHandler(InsufficientStockException.class)
    public Result<?> handleInsufficientStock(InsufficientStockException e) {
        log.warn("库存不足: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理无效数量异常
     */
    @ExceptionHandler(InvalidAmountException.class)
    public Result<?> handleInvalidAmount(InvalidAmountException e) {
        log.warn("数据验证失败: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理类别名称重复异常
     */
    @ExceptionHandler(DuplicateCategoryException.class)
    public Result<?> handleDuplicate(DuplicateCategoryException e) {
        log.warn("数据冲突: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleGeneral(Exception e) {
        log.error("系统错误", e);
        return Result.error(500, "系统内部错误");
    }
}
