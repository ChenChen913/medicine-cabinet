package com.family.medicinecabinet.exception;

import lombok.Getter;

/**
 * 药品相关异常基类
 */
@Getter
public class MedicineException extends RuntimeException {
    
    private final int code;
    
    public MedicineException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    public MedicineException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
