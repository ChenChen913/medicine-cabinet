package com.family.medicinecabinet.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Arrays;

/**
 * 剂型枚举
 *
 * <p>成员集合与 {@code schema.sql} 的 {@code dosage_form ENUM} 严格一致（7 个）。
 * 入库值（{@code @EnumValue}）与出参值（Jackson 默认按枚举名序列化）均为英文大写枚举名；
 * 中文仅作为 {@code description}，用于展示与兼容输入。</p>
 */
@Getter
public enum DosageForm {

    /**
     * 片剂
     */
    TABLET("片剂"),

    /**
     * 胶囊
     */
    CAPSULE("胶囊"),

    /**
     * 颗粒
     */
    GRANULE("颗粒"),

    /**
     * 液体（口服液）
     */
    LIQUID("液体"),

    /**
     * 外用
     */
    EXTERNAL("外用"),

    /**
     * 注射剂
     */
    INJECTION("注射剂"),

    /**
     * 其他
     */
    OTHER("其他");

    /**
     * 数据库存储值：等于枚举名（英文大写），绝不使用中文 description
     */
    @EnumValue
    private final String code;

    /**
     * 中文描述：仅供展示与兼容输入，不作为入库值与出参值
     */
    private final String description;

    DosageForm(String description) {
        this.code = name();
        this.description = description;
    }

    /**
     * 将入参归一化为剂型枚举。
     *
     * <p>同时接受：枚举名（大小写不敏感）、中文描述，以及历史遗留值
     * （{@code syrup}/{@code 口服液} → LIQUID，{@code ointment}/{@code 外用药膏} → EXTERNAL，
     * 这两个成员已不在权威集合内）。</p>
     *
     * @param value 入参字符串
     * @return 匹配到的枚举
     * @throws IllegalArgumentException 入参为 null、空白或无法识别时抛出（不做默认值回落）
     */
    @JsonCreator
    public static DosageForm fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("无效的剂型: " + value);
        }
        String input = value.trim();
        // 历史遗留值兼容映射（权威集合中已无 SYRUP / OINTMENT）
        if ("syrup".equalsIgnoreCase(input) || "口服液".equals(input)) {
            return LIQUID;
        }
        if ("ointment".equalsIgnoreCase(input) || "外用药膏".equals(input)) {
            return EXTERNAL;
        }
        return Arrays.stream(values())
                .filter(form -> form.name().equalsIgnoreCase(input) || form.description.equals(input))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的剂型: " + value));
    }
}
