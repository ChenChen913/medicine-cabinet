package com.family.medicinecabinet.property;

import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.enums.DosageForm;
import com.family.medicinecabinet.entity.enums.MedicineStatus;
import com.family.medicinecabinet.service.SortingService;
import com.family.medicinecabinet.service.impl.SortingServiceImpl;
import net.jqwik.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 排序算法属性测试类
 */
@SpringBootTest
@PropertyDefaults(tries = 100)
public class SortingPropertyTest {
    
    private final SortingService sortingService = new SortingServiceImpl();
    
    // Feature: family-medicine-cabinet, Property 7: 智能排序第一优先级
    @Property
    @Label("排序后的药品应该按使用频率分数降序排列")
    void sortingByFrequencyPriority(@ForAll("medicineList") List<Medicine> medicines) {
        // When: 应用排序规则
        List<Medicine> sorted = sortingService.applySortingRules(medicines);
        
        // Then: 验证频率分数降序
        for (int i = 0; i < sorted.size() - 1; i++) {
            int currentScore = sorted.get(i).getUsageFrequencyScore();
            int nextScore = sorted.get(i + 1).getUsageFrequencyScore();
            assert currentScore >= nextScore : 
                String.format("频率分数应该降序排列: 位置%d的分数%d >= 位置%d的分数%d", 
                    i, currentScore, i+1, nextScore);
        }
    }
    
    // Feature: family-medicine-cabinet, Property 8: 智能排序第二优先级
    @Property
    @Label("具有相同使用频率分数的药品应该按最近购买日期降序排列")
    void sortingByPurchaseDatePriority(@ForAll("sameFrequencyMedicines") List<Medicine> medicines) {
        // When: 应用排序规则
        List<Medicine> sorted = sortingService.applySortingRules(medicines);
        
        // Then: 验证相同频率分数的药品按购买日期降序
        for (int i = 0; i < sorted.size() - 1; i++) {
            Medicine current = sorted.get(i);
            Medicine next = sorted.get(i + 1);
            
            // 如果频率分数相同
            if (current.getUsageFrequencyScore().equals(next.getUsageFrequencyScore())) {
                LocalDate currentDate = current.getLastPurchaseDate();
                LocalDate nextDate = next.getLastPurchaseDate();
                
                // 如果都有购买日期，应该降序
                if (currentDate != null && nextDate != null) {
                    assert !currentDate.isBefore(nextDate) :
                        String.format("相同频率分数时，购买日期应该降序: %s >= %s", 
                            currentDate, nextDate);
                }
                // null 应该排在后面
                if (currentDate != null && nextDate == null) {
                    // 正确：有日期的在前
                } else if (currentDate == null && nextDate != null) {
                    assert false : "null 购买日期应该排在有日期的后面";
                }
            }
        }
    }
    
    /**
     * 提供相同频率分数的药品列表
     */
    @Provide
    Arbitrary<List<Medicine>> sameFrequencyMedicines() {
        int sameScore = 5; // 固定频率分数
        return Arbitraries.integers().between(0, 30).flatMap(daysAgo -> {
            Medicine m = new Medicine();
            m.setId(UUID.randomUUID().toString());
            m.setName("药品" + daysAgo);
            m.setDosageForm(DosageForm.TABLET);
            m.setUnit("粒");
            m.setTotalQuantity(BigDecimal.valueOf(50));
            m.setThreshold(BigDecimal.TEN);
            m.setExpiryDate(LocalDate.now().plusYears(1));
            m.setUsageFrequencyScore(sameScore);
            m.setStatus(MedicineStatus.NORMAL);
            m.setLastPurchaseDate(daysAgo > 0 ? LocalDate.now().minusDays(daysAgo) : null);
            m.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
            return Arbitraries.just(m);
        }).list().ofMinSize(3).ofMaxSize(10);
    }
    
    /**
     * 提供药品列表生成器
     */
    @Provide
    Arbitrary<List<Medicine>> medicineList() {
        return medicine().list().ofMinSize(2).ofMaxSize(20);
    }
    
    // Feature: family-medicine-cabinet, Property 9: 排序完整性不变量
    @Property
    @Label("排序前后药品数量应该相等，且排序后的列表应该包含原列表中的所有药品")
    void sortingCompletenessInvariant(@ForAll("medicineList") List<Medicine> medicines) {
        // When: 应用排序规则
        List<Medicine> sorted = sortingService.applySortingRules(medicines);
        
        // Then: 验证数量相等
        assert sorted.size() == medicines.size() :
            String.format("排序前后数量应该相等: 原始%d, 排序后%d", 
                medicines.size(), sorted.size());
        
        // 验证包含所有元素（通过ID）
        List<String> originalIds = medicines.stream()
            .map(Medicine::getId)
            .sorted()
            .toList();
        List<String> sortedIds = sorted.stream()
            .map(Medicine::getId)
            .sorted()
            .toList();
        
        assert originalIds.equals(sortedIds) :
            "排序后应该包含原列表中的所有药品，无遗漏、无重复";
    }
    
    /**
     * 提供单个药品生成器
     */
    @Provide
    Arbitrary<Medicine> medicine() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
            Arbitraries.integers().between(0, 100),
            Arbitraries.integers().between(0, 10)
        ).as((name, frequencyScore, daysAgo) -> {
            Medicine m = new Medicine();
            m.setId(UUID.randomUUID().toString());
            m.setName(name);
            m.setDosageForm(DosageForm.TABLET);
            m.setUnit("粒");
            m.setTotalQuantity(BigDecimal.valueOf(50));
            m.setThreshold(BigDecimal.TEN);
            m.setExpiryDate(LocalDate.now().plusYears(1));
            m.setUsageFrequencyScore(frequencyScore);
            m.setStatus(MedicineStatus.NORMAL);
            m.setLastPurchaseDate(LocalDate.now().minusDays(daysAgo));
            m.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
            return m;
        });
    }
}
