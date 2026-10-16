package com.family.medicinecabinet.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

/**
 * 药品状态枚举
 */
@Getter
public enum MedicineStatus {
    NORMAL("正常"),
    LOW_STOCK("低库存"),
    EXPIRED("已过期"),
    DEPLETED("已耗尽");

    @EnumValue
    private final String description;

    MedicineStatus(String description) {
        this.description = description;
    }
    
    /**
     * 根据描述查找药品状态枚举
     */
    @JsonCreator
    public static MedicineStatus fromString(String value) {
        return Arrays.stream(MedicineStatus.values())
                .filter(status -> status.getDescription().equals(value))
                .findFirst()
                .orElse(NORMAL);
    }
}