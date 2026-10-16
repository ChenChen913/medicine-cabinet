package com.family.medicinecabinet.property;

import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.ShoppingListItem;
import com.family.medicinecabinet.entity.UsageLog;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.mapper.ShoppingListMapper;
import com.family.medicinecabinet.mapper.UsageLogMapper;
import com.family.medicinecabinet.service.InventoryService;
import com.family.medicinecabinet.service.dto.ConsumeResult;
import com.family.medicinecabinet.support.MockServices;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;

import java.math.BigDecimal;

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

        // Given: 创建 mock 对象（deductStock 用 Answer 等价模拟数据库的原子扣减）
        MedicineMapper medicineMapper = mock(MedicineMapper.class);
        UsageLogMapper usageLogMapper = mock(UsageLogMapper.class);
        ShoppingListMapper shoppingListMapper = mock(ShoppingListMapper.class);

        // Given: 创建药品与服务
        Medicine medicine = MockServices.medicine(initialQuantity, BigDecimal.TEN);
        InventoryService inventoryService = MockServices.inventoryService(
            medicineMapper, usageLogMapper, shoppingListMapper);

        // Mock mapper 行为
        when(medicineMapper.selectById(medicine.getId())).thenReturn(medicine);
        MockServices.fakeDeductStock(medicineMapper, medicine);
        when(usageLogMapper.insert(any(UsageLog.class))).thenReturn(1);
        when(shoppingListMapper.insert(any(ShoppingListItem.class))).thenReturn(1);

        // When: 消耗药品（真实调用生产代码）
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

        // Then: 从 Mapper 读回的库存与返回值一致
        BigDecimal stored = medicineMapper.selectById(medicine.getId()).getTotalQuantity();
        assert stored.compareTo(expected) == 0 :
            String.format("数据库中读回的库存应该等于返回值: 期望 %s, 实际 %s", expected, stored);
    }
}
