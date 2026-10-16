package com.family.medicinecabinet.exception;

/**
 * 库存不足异常
 */
public class InsufficientStockException extends MedicineException {
    
    public InsufficientStockException(String message) {
        super(422, message);
    }
}
