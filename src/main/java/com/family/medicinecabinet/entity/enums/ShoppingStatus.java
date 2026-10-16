package com.family.medicinecabinet.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 购物清单状态枚举
 */
@Getter
public enum ShoppingStatus {
    PENDING("待购买"),
    PURCHASED("已购买");

    @EnumValue
    @JsonValue
    private final String description;

    ShoppingStatus(String description) {
        this.description = description;
    }
}
