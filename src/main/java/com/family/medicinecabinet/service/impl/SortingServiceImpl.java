package com.family.medicinecabinet.service.impl;

import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.service.SortingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能排序服务实现类
 */
@Slf4j
@Service
public class SortingServiceImpl implements SortingService {
    
    @Autowired
    private MedicineMapper medicineMapper;
    
    @Override
    public List<Medicine> getSortedMedicines(String categoryId) {
        log.debug("获取类别 {} 的排序后药品列表", categoryId);
        // 直接使用 Mapper 的排序查询（SQL 层面排序，性能更好）
        return medicineMapper.findByCategoryWithSorting(categoryId);
    }
    
    @Override
    public List<Medicine> applySortingRules(List<Medicine> medicines) {
        log.debug("应用智能排序规则，药品数量: {}", medicines.size());
        
        return medicines.stream()
            .sorted(Comparator
                // 第一优先级：使用频率分数降序
                .comparing(Medicine::getUsageFrequencyScore, Comparator.reverseOrder())
                // 第二优先级：购买日期降序（null 排最后）
                .thenComparing(Medicine::getLastPurchaseDate, 
                    Comparator.nullsLast(Comparator.reverseOrder()))
                // 第三优先级：创建时间降序
                .thenComparing(Medicine::getCreatedAt, 
                    Comparator.nullsLast(Comparator.reverseOrder()))
            )
            .collect(Collectors.toList());
    }
}
