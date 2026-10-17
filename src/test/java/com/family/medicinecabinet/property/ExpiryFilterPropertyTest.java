package com.family.medicinecabinet.property;

import com.family.medicinecabinet.entity.enums.ExpiryStatusFilter;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.PropertyDefaults;
import net.jqwik.api.Provide;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 有效期三态筛选的属性测试（与 {@code SortingPropertyTest} 同目录同风格）。
 *
 * <p>不依赖 Spring 上下文：被测对象是纯日期边界计算。</p>
 */
@PropertyDefaults(tries = 500)
public class ExpiryFilterPropertyTest {

    // Feature: medicine-cabinet, 新增功能：按有效期筛选
    @Property
    @Label("任意基准日与任意有效期：已过期/即将过期/未过期三态中恰好一态命中（互斥且穷尽）")
    void exactlyOneFilterMatches(@ForAll("dates") LocalDate today, @ForAll("dates") LocalDate expiryDate) {
        List<ExpiryStatusFilter> hits = Arrays.stream(ExpiryStatusFilter.values())
                .filter(filter -> filter.matches(expiryDate, today))
                .toList();
        assert hits.size() == 1 :
                String.format("today=%s, expiryDate=%s 命中 %d 个筛选态：%s", today, expiryDate, hits.size(), hits);
    }

    @Property
    @Label("已过期严格等于『有效期早于今天』；未过期严格等于『有效期晚于今天+30』")
    void boundariesAreConsistent(@ForAll("dates") LocalDate today, @ForAll("dates") LocalDate expiryDate) {
        assert ExpiryStatusFilter.EXPIRED.matches(expiryDate, today) == expiryDate.isBefore(today)
                : String.format("EXPIRED 边界不符: today=%s, expiryDate=%s", today, expiryDate);

        boolean valid = ExpiryStatusFilter.VALID.matches(expiryDate, today);
        boolean afterWindow = expiryDate.isAfter(today.plusDays(ExpiryStatusFilter.EXPIRING_SOON_DAYS));
        assert valid == afterWindow
                : String.format("VALID 边界不符: today=%s, expiryDate=%s", today, expiryDate);
    }

    @Provide
    Arbitrary<LocalDate> dates() {
        // 限定在有限年限内，避免 LocalDate.MAX/MIN 附近 plusDays 溢出干扰属性
        return Arbitraries.longs()
                .between(LocalDate.of(1900, 1, 1).toEpochDay(), LocalDate.of(2100, 12, 31).toEpochDay())
                .map(LocalDate::ofEpochDay);
    }
}
