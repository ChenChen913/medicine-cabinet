package com.family.medicinecabinet.exception;

/**
 * 类别名称重复异常
 */
public class DuplicateCategoryException extends MedicineException {
    
    public DuplicateCategoryException(String message) {
        super(409, message);
    }
}
