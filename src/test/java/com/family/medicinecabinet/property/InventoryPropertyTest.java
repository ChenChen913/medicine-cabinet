package com.family.medicinecabinet.property;

import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.enums.DosageForm;
import com.family.medicinecabinet.entity.enums.MedicineStatus;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.mapper.ShoppingListMapper;
import com.family.medicinecabinet.mapper.UsageLogMapper;
import com.family.medicinecabinet.service.InventoryService;
import com.family.medicinecabinet.service.dto.ConsumeResult;
import com.family.medicinecabinet.service.impl.InventoryServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 库存管理属性测试类
 */
@PropertyDefaults(tries = 100)
public class InventoryPropertyTest {
    
    // Feature: family-medicine-cabinet, Property 4: 库存扣减不变量
    @Property
    @Label("对于任意有效的消耗量，记录用药后的库存应该等于原库存减去消耗量")
    void inventoryDeductionInvariant(
            @ForAll @BigRange(min = "10", max = "1000") BigDecimal initialQuantity,
            @ForAll @BigRange(min = "1", max = "100") BigDecimal consumeAmount
    ) {
        // 跳过无效情况
        Assume.that(consumeAmount.compareTo(initialQuantity) <= 0);
        
        // Given: 创建 mock 对象
        MedicineMapper medicineMapper = mock(MedicineMapper.class);
        UsageLogMapper usageLogMapper = mock(UsageLogMapper.class);
        ShoppingListMapper shoppingListMapper = mock(ShoppingListMapper.class);
        
        // Given: 创建药品
        Medicine medicine = createTestMedicine(initialQuantity);
        InventoryService inventoryService = createInventoryService(medicineMapper, usageLogMapper, shoppingListMapper);
        
        // Mock mapper 行为
        when(medicineMapper.selectById(medicine.getId())).thenReturn(medicine);
        when(usageLogMapper.insert(any(com.family.medicinecabinet.entity.UsageLog.class))).thenReturn(1);
        when(shoppingListMapper.insert(any(com.family.medicinecabinet.entity.ShoppingListItem.class))).thenReturn(1);
        when(medicineMapper.updateById(any(Medicine.class))).thenReturn(1);
        
        // When: 消耗药品
        ConsumeResult result = inventoryService.consumeMedicine(
            medicine.getId(), 
            consumeAmount, 
            "测试用户", 
            "测试症状"
        );
        
        // Then: 验证库存扣减正确
        BigDecimal expected = initialQuantity.subtract(consumeAmount);
        assert result.getNewQuantity().compareTo(expected) == 0 :
            String.format("库存扣减不正确: 期望 %s, 实际 %s", expected, result.getNewQuantity());
    }
    
    /**
     * 创建测试药品
     */
    private Medicine createTestMedicine(BigDecimal quantity) {
        Medicine medicine = new Medicine();
        medicine.setId(UUID.randomUUID().toString());
        medicine.setName("测试药品");
        medicine.setDosageForm(DosageForm.TABLET);
        medicine.setUnit("粒");
        medicine.setTotalQuantity(quantity);
        medicine.setThreshold(BigDecimal.TEN);
        medicine.setExpiryDate(LocalDate.now().plusYears(1));
        medicine.setUsageFrequencyScore(0);
        medicine.setStatus(MedicineStatus.NORMAL);
        return medicine;
    }
    
    /**
     * 创建库存服务实例
     */
    private InventoryService createInventoryService(
            MedicineMapper medicineMapper,
            UsageLogMapper usageLogMapper,
            ShoppingListMapper shoppingListMapper) {
        InventoryServiceImpl service = new InventoryServiceImpl();
        // 使用反射设置私有字段
        try {
            java.lang.reflect.Field medicineMapperField = 
                InventoryServiceImpl.class.getDeclaredField("medicineMapper");
            medicineMapperField.setAccessible(true);
            medicineMapperField.set(service, medicineMapper);
            
            java.lang.reflect.Field usageLogMapperField = 
                InventoryServiceImpl.class.getDeclaredField("usageLogMapper");
            usageLogMapperField.setAccessible(true);
            usageLogMapperField.set(service, usageLogMapper);
            
            java.lang.reflect.Field shoppingListMapperField = 
                InventoryServiceImpl.class.getDeclaredField("shoppingListMapper");
            shoppingListMapperField.setAccessible(true);
            shoppingListMapperField.set(service, shoppingListMapper);
        } catch (Exception e) {
            throw new RuntimeException("设置字段失败", e);
        }
        return service;
    }
}
