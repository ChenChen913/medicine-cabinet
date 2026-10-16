package com.family.medicinecabinet.exception;

import com.family.medicinecabinet.common.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常基类
     *
     * <p>覆盖未单独声明处理器的 {@link MedicineException} 子类/直接实例
     * （例如 409 冲突），避免它们落进兜底分支被改写成 500。</p>
     */
    @ExceptionHandler(MedicineException.class)
    public Result<?> handleMedicineException(MedicineException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

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
     * 处理缺少必需请求参数（如漏传 userName）
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.error(400, "缺少必需参数: " + e.getParameterName());
    }

    /**
     * 处理参数类型不匹配（如 amount=abc）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: name={}, value={}", e.getName(), e.getValue());
        return Result.error(400, "参数类型不正确: " + e.getName());
    }

    /**
     * 处理请求体缺失或 JSON 格式错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.error(400, "请求体缺失或格式错误");
    }

    /**
     * 处理请求体字段校验失败（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", detail);
        return Result.error(400, detail.isEmpty() ? "参数校验失败" : "参数校验失败: " + detail);
    }

    /**
     * 处理方法参数约束校验失败（{@code @Validated} + {@code @RequestParam} 上的约束注解）
     *
     * <p>消息取属性路径的最后一段作为字段名（如 {@code restock.amount} → {@code amount}），
     * 保证客户端能定位到具体入参。</p>
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolation(ConstraintViolationException e) {
        String detail = e.getConstraintViolations().stream()
                .map(violation -> lastPathSegment(violation.getPropertyPath().toString())
                        + " " + violation.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数约束校验失败: {}", detail);
        return Result.error(400, detail.isEmpty() ? "参数校验失败" : "参数校验失败: " + detail);
    }

    /**
     * 处理 Spring MVC 内建方法校验失败（Spring 6.1 起 {@code @RequestParam} 上的约束走这条路径）
     *
     * <p>该异常继承自 {@code ResponseStatusException}，若不单独声明会被下面的兜底分支
     * 改写成 500，因此必须显式处理为 400。</p>
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public Result<?> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        String detail = e.getAllValidationResults().stream()
                .flatMap(result -> {
                    MethodParameter parameter = result.getMethodParameter();
                    String paramName = parameter.getParameterName() != null
                            ? parameter.getParameterName() : "参数";
                    return result.getResolvableErrors().stream()
                            .map(error -> paramName + " " + error.getDefaultMessage());
                })
                .collect(Collectors.joining("; "));
        log.warn("方法参数校验失败: {}", detail);
        return Result.error(400, detail.isEmpty() ? "参数校验失败" : "参数校验失败: " + detail);
    }

    /**
     * 处理表单/查询参数绑定校验失败（{@code @ModelAttribute} + {@code @Valid}）
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String detail = e.getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数绑定校验失败: {}", detail);
        return Result.error(400, detail.isEmpty() ? "参数校验失败" : "参数校验失败: " + detail);
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleGeneral(Exception e) {
        log.error("系统错误", e);
        return Result.error(500, "系统内部错误");
    }

    /**
     * 取属性路径的最后一段（{@code restock.amount} → {@code amount}），用于提取字段名。
     */
    private static String lastPathSegment(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot >= 0 && lastDot < propertyPath.length() - 1
                ? propertyPath.substring(lastDot + 1)
                : propertyPath;
    }
}
