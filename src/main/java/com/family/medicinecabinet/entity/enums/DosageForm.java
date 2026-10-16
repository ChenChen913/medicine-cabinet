package com.family.medicinecabinet.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

/**
 * 剂型枚举
 */
@Getter
public enum DosageForm {
    
    /**
     * 片剂
     */
    TABLET("tablet", "片剂"),
    
    /**
     * 胶囊
     */
    CAPSULE("capsule", "胶囊"),
    
    /**
     * 口服液
     */
    SYRUP("syrup", "口服液"),
    
    /**
     * 注射剂
     */
    INJECTION("injection", "注射剂"),
    
    /**
     * 外用药膏
     */
    OINTMENT("ointment", "外用药膏"),
    
    /**
     * 其他
     */
    OTHER("other", "其他");
    
    @EnumValue
    private final String code;
    
    @JsonValue
    private final String description;
    
    DosageForm(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据代码或描述查找剂型枚举
     */
    @JsonCreator
    public static DosageForm fromString(String value) {
        return Arrays.stream(DosageForm.values())
                .filter(form -> form.getCode().equalsIgnoreCase(value) || form.getDescription().equals(value))
                .findFirst()
                .orElse(OTHER);
    }
}