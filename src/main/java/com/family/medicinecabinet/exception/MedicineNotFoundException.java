package com.family.medicinecabinet.exception;

/**
 * 药品不存在异常
 */
public class MedicineNotFoundException extends MedicineException {
    
    public MedicineNotFoundException(String message) {
        super(404, message);
    }
}
