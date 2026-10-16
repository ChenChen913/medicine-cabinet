package com.family.medicinecabinet.exception;

/**
 * 无效数量异常
 */
public class InvalidAmountException extends MedicineException {
    
    public InvalidAmountException(String message) {
        super(400, message);
    }
}
