package com.family.medicinecabinet.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 购物清单添加原因枚举
 */
@Getter
public enum ShoppingReason {
    EXPIRED("过期"),
    DEPLETED("耗尽"),
    MANUAL("手动添加");

    @EnumValue
    @JsonValue
    private final String description;

    ShoppingReason(String description) {
        this.description = description;
    }
}
