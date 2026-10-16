package com.family.medicinecabinet.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Arrays;

/**
 * 药品状态枚举
 *
 * <p>成员集合与 {@code schema.sql} 的 {@code status ENUM('NORMAL', 'LOW_STOCK', 'EXPIRED', 'DEPLETED')}
 * 严格一致。入库值（{@code @EnumValue}）与出参值（Jackson 默认按枚举名序列化）均为英文大写枚举名；
 * 中文仅作为 {@code description}，用于展示与兼容输入。</p>
 */
@Getter
public enum MedicineStatus {

    /**
     * 正常
     */
    NORMAL("正常"),

    /**
     * 低库存
     */
    LOW_STOCK("低库存"),

    /**
     * 已过期
     */
    EXPIRED("已过期"),

    /**
     * 已耗尽
     */
    DEPLETED("已耗尽");

    /**
     * 数据库存储值：等于枚举名（英文大写），绝不使用中文 description
     */
    @EnumValue
    private final String code;

    /**
     * 中文描述：仅供展示与兼容输入，不作为入库值与出参值
     */
    private final String description;

    MedicineStatus(String description) {
        this.code = name();
        this.description = description;
    }

    /**
     * 将入参归一化为药品状态枚举。
     *
     * <p>同时接受枚举名（大小写不敏感）与中文描述。</p>
     *
     * @param value 入参字符串
     * @return 匹配到的枚举
     * @throws IllegalArgumentException 入参为 null、空白或无法识别时抛出（不做默认值回落）
     */
    @JsonCreator
    public static MedicineStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("无效的药品状态: " + value);
        }
        String input = value.trim();
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(input) || status.description.equals(input))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的药品状态: " + value));
    }
}
