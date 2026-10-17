package com.family.medicinecabinet.entity.enums;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.family.medicinecabinet.entity.Medicine;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * 药品列表的「有效期状态」筛选枚举（仅用于查询筛选，<strong>不落库</strong>）。
 *
 * <p>与 {@link MedicineStatus} 的区别：{@code MedicineStatus.EXPIRED} 是数据库里的状态列，
 * 由每日定时任务写入；本枚举是查询时按 {@code expiry_date} 现算的三态筛选，不依赖状态列是否已被刷新。</p>
 *
 * <p>边界规则（与既有过期监控保持一致：{@code expiry_date < 今天} 才算过期，到期当天仍有效）：</p>
 * <ul>
 *   <li>{@link #EXPIRED}：{@code expiry_date < 今天}</li>
 *   <li>{@link #EXPIRING_SOON}：{@code 今天 <= expiry_date <= 今天 + 30 天}（含两端）</li>
 *   <li>{@link #VALID}：{@code expiry_date > 今天 + 30 天}</li>
 * </ul>
 *
 * <p>三态互斥且覆盖全部合法日期；{@code expiry_date} 为 null 时不属于任何一态
 * （表列本身 NOT NULL，SQL 侧 {@code NULL} 比较也不会命中任何一项）。</p>
 */
public enum ExpiryStatusFilter {

    /**
     * 已过期：有效期早于今天
     */
    EXPIRED("已过期"),

    /**
     * 即将过期：今天起 30 天内到期（含今天与第 30 天）
     */
    EXPIRING_SOON("即将过期"),

    /**
     * 未过期：距离到期超过 30 天
     */
    VALID("未过期");

    /**
     * 「即将过期」窗口天数（闭区间），与 {@code 今天 + N 天} 同义
     */
    public static final int EXPIRING_SOON_DAYS = 30;

    private final String description;

    ExpiryStatusFilter(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 解析筛选参数（与 {@code sortBy} 一致：区分大小写、空串视为非法、不做默认值回落）。
     *
     * @param value 请求参数值
     * @return 匹配到的筛选态
     * @throws IllegalArgumentException 入参为 null、空白或无法识别时抛出
     */
    public static ExpiryStatusFilter fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("不支持的过期状态: " + value + "，可选值: EXPIRED/EXPIRING_SOON/VALID");
        }
        return Arrays.stream(values())
                .filter(filter -> filter.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "不支持的过期状态: " + value + "，可选值: EXPIRED/EXPIRING_SOON/VALID"));
    }

    /**
     * 闭区间下界（含）；返回 null 表示该侧无下界。
     */
    public LocalDate lowerBound(LocalDate today) {
        return switch (this) {
            case EXPIRED -> null;
            case EXPIRING_SOON -> today;
            case VALID -> today.plusDays(EXPIRING_SOON_DAYS + 1L);
        };
    }

    /**
     * 闭区间上界（含）；返回 null 表示该侧无上界。
     */
    public LocalDate upperBound(LocalDate today) {
        return switch (this) {
            case EXPIRED -> today.minusDays(1);
            case EXPIRING_SOON -> today.plusDays(EXPIRING_SOON_DAYS);
            case VALID -> null;
        };
    }

    /**
     * 判断某个有效期日期是否落在本筛选态内（与 {@link #applyTo} 使用同一组边界，二者不会分叉）。
     *
     * @param expiryDate 药品有效期，可为 null
     * @param today      判定基准日
     * @return 命中返回 true；{@code expiryDate} 为 null 恒为 false
     */
    public boolean matches(LocalDate expiryDate, LocalDate today) {
        if (expiryDate == null) {
            return false;
        }
        LocalDate lower = lowerBound(today);
        LocalDate upper = upperBound(today);
        return (lower == null || !expiryDate.isBefore(lower))
                && (upper == null || !expiryDate.isAfter(upper));
    }

    /**
     * 把本筛选态的边界条件追加到查询包装器。
     *
     * @param wrapper 目标查询条件
     * @param today   判定基准日
     */
    public void applyTo(LambdaQueryWrapper<Medicine> wrapper, LocalDate today) {
        LocalDate lower = lowerBound(today);
        LocalDate upper = upperBound(today);
        if (lower != null) {
            wrapper.ge(Medicine::getExpiryDate, lower);
        }
        if (upper != null) {
            wrapper.le(Medicine::getExpiryDate, upper);
        }
    }
}
