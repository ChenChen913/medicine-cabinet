package com.family.medicinecabinet.entity.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 有效期三态筛选的边界与非法输入用例。
 *
 * <p>基准日固定为 2026-10-17，边界点逐一断言：
 * 昨天（已过期）→ 今天（即将过期，到期当天仍有效）→ 今天+30（仍即将过期）→ 今天+31（未过期）。
 * 另含跨月、跨年、闰年边界，以及 null / 非法参数值。</p>
 */
class ExpiryStatusFilterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);

    // ---------- 边界：昨天 / 今天 / 今天+30 / 今天+31 ----------

    @Test
    @DisplayName("有效期 = 今天-1 → 仅命中 EXPIRED")
    void yesterdayIsExpired() {
        assertOnly(ExpiryStatusFilter.EXPIRED, TODAY.minusDays(1), TODAY);
    }

    @Test
    @DisplayName("有效期 = 今天 → 到期当天仍有效，仅命中 EXPIRING_SOON（不判已过期）")
    void todayIsExpiringSoonNotExpired() {
        assertOnly(ExpiryStatusFilter.EXPIRING_SOON, TODAY, TODAY);
    }

    @Test
    @DisplayName("有效期 = 今天+30（窗口上界，含）→ 仅命中 EXPIRING_SOON")
    void thirtyDaysLaterIsExpiringSoon() {
        assertOnly(ExpiryStatusFilter.EXPIRING_SOON, TODAY.plusDays(ExpiryStatusFilter.EXPIRING_SOON_DAYS), TODAY);
    }

    @Test
    @DisplayName("有效期 = 今天+31（刚出窗口）→ 仅命中 VALID")
    void thirtyOneDaysLaterIsValid() {
        assertOnly(ExpiryStatusFilter.VALID, TODAY.plusDays(ExpiryStatusFilter.EXPIRING_SOON_DAYS + 1L), TODAY);
    }

    @Test
    @DisplayName("有效期远在未来 → 仅命中 VALID")
    void farFutureIsValid() {
        assertOnly(ExpiryStatusFilter.VALID, TODAY.plusYears(10), TODAY);
    }

    @Test
    @DisplayName("有效期远在过去 → 仅命中 EXPIRED")
    void farPastIsExpired() {
        assertOnly(ExpiryStatusFilter.EXPIRED, TODAY.minusYears(10), TODAY);
    }

    // ---------- 跨月 / 跨年 / 闰年 ----------

    @Test
    @DisplayName("跨月边界：基准日 2026-01-31，+30 天 = 2026-03-02（2 月仅 28 天）")
    void monthBoundary() {
        LocalDate today = LocalDate.of(2026, 1, 31);
        assertThat(ExpiryStatusFilter.EXPIRING_SOON.upperBound(today)).isEqualTo(LocalDate.of(2026, 3, 2));
        assertOnly(ExpiryStatusFilter.EXPIRED, LocalDate.of(2026, 1, 30), today);
        assertOnly(ExpiryStatusFilter.EXPIRING_SOON, LocalDate.of(2026, 3, 2), today);
        assertOnly(ExpiryStatusFilter.VALID, LocalDate.of(2026, 3, 3), today);
    }

    @Test
    @DisplayName("跨年边界：基准日 2027-12-20，+30 天 = 2028-01-19")
    void yearBoundary() {
        LocalDate today = LocalDate.of(2027, 12, 20);
        assertThat(ExpiryStatusFilter.EXPIRING_SOON.upperBound(today)).isEqualTo(LocalDate.of(2028, 1, 19));
        assertOnly(ExpiryStatusFilter.EXPIRED, LocalDate.of(2027, 12, 19), today);
        assertOnly(ExpiryStatusFilter.EXPIRING_SOON, LocalDate.of(2028, 1, 19), today);
        assertOnly(ExpiryStatusFilter.VALID, LocalDate.of(2028, 1, 20), today);
    }

    @Test
    @DisplayName("闰年边界：基准日 2028-02-28，+30 天 = 2028-03-29（含 2 月 29 日）")
    void leapYearBoundary() {
        LocalDate today = LocalDate.of(2028, 2, 28);
        assertThat(ExpiryStatusFilter.EXPIRING_SOON.upperBound(today)).isEqualTo(LocalDate.of(2028, 3, 29));
        assertOnly(ExpiryStatusFilter.EXPIRED, LocalDate.of(2028, 2, 27), today);
        assertOnly(ExpiryStatusFilter.EXPIRING_SOON, LocalDate.of(2028, 2, 29), today);
        assertOnly(ExpiryStatusFilter.EXPIRING_SOON, LocalDate.of(2028, 3, 29), today);
        assertOnly(ExpiryStatusFilter.VALID, LocalDate.of(2028, 3, 30), today);
    }

    // ---------- 无日期 / 非法参数 ----------

    @Test
    @DisplayName("有效期为空 → 三态均不命中（表列 NOT NULL，SQL 中 NULL 比较亦不命中）")
    void nullExpiryMatchesNothing() {
        for (ExpiryStatusFilter filter : ExpiryStatusFilter.values()) {
            assertThat(filter.matches(null, TODAY)).as(filter.name()).isFalse();
        }
    }

    @Test
    @DisplayName("fromString 接受三个大写枚举名")
    void fromStringAcceptsEnumNames() {
        assertThat(ExpiryStatusFilter.fromString("EXPIRED")).isEqualTo(ExpiryStatusFilter.EXPIRED);
        assertThat(ExpiryStatusFilter.fromString("EXPIRING_SOON")).isEqualTo(ExpiryStatusFilter.EXPIRING_SOON);
        assertThat(ExpiryStatusFilter.fromString("VALID")).isEqualTo(ExpiryStatusFilter.VALID);
    }

    @Test
    @DisplayName("fromString 拒绝 null / 空串 / 空白 / 大小写不符 / 未知值，并给出可选值")
    void fromStringRejectsInvalidInput() {
        for (String invalid : Arrays.asList(null, "", "   ", "expired", "Expiring_Soon", "FOO", "已过期")) {
            assertThatThrownBy(() -> ExpiryStatusFilter.fromString(invalid))
                    .as(String.valueOf(invalid))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("EXPIRED/EXPIRING_SOON/VALID");
        }
    }

    // ---------- 属性测试见 property/ExpiryFilterPropertyTest ----------

    /**
     * 断言某个有效期只命中预期的那一态（另外两态都不命中），即三态互斥。
     */
    private static void assertOnly(ExpiryStatusFilter expected, LocalDate expiryDate, LocalDate today) {
        for (ExpiryStatusFilter filter : ExpiryStatusFilter.values()) {
            assertThat(filter.matches(expiryDate, today))
                    .as("%s vs %s (today=%s)", filter.name(), expiryDate, today)
                    .isEqualTo(filter == expected);
        }
    }
}
