package com.family.medicinecabinet.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.ShoppingListItem;
import com.family.medicinecabinet.entity.UsageLog;
import com.family.medicinecabinet.exception.InsufficientStockException;
import com.family.medicinecabinet.exception.InvalidAmountException;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.mapper.ShoppingListMapper;
import com.family.medicinecabinet.mapper.UsageLogMapper;
import com.family.medicinecabinet.service.dto.ConsumeResult;
import com.family.medicinecabinet.support.MockServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 库存服务的定向用例（对应冻结契约 S1 / S2）
 *
 * <p>重点覆盖"补货/消耗的数量校验"与"库存只走原子 UPDATE、绝不回写绝对值"两条不变量。</p>
 */
class InventoryServiceImplTest {

    private MedicineMapper medicineMapper;
    private UsageLogMapper usageLogMapper;
    private ShoppingListMapper shoppingListMapper;
    private InventoryServiceImpl inventoryService;
    private Medicine medicine;

    @BeforeEach
    void setUp() {
        medicineMapper = mock(MedicineMapper.class);
        usageLogMapper = mock(UsageLogMapper.class);
        shoppingListMapper = mock(ShoppingListMapper.class);
        inventoryService = MockServices.inventoryService(medicineMapper, usageLogMapper, shoppingListMapper);

        medicine = MockServices.medicine(BigDecimal.valueOf(100), BigDecimal.TEN);
        when(medicineMapper.selectById(medicine.getId())).thenReturn(medicine);
        when(usageLogMapper.insert(any(UsageLog.class))).thenReturn(1);
        when(shoppingListMapper.insert(any(ShoppingListItem.class))).thenReturn(1);
    }

    @Test
    @DisplayName("补货负数 → 抛 InvalidAmountException，库存不变且不触达库存写入")
    void restockWithNegativeAmountIsRejected() {
        InvalidAmountException exception = assertThrows(InvalidAmountException.class,
            () -> inventoryService.restockMedicine(medicine.getId(), BigDecimal.valueOf(-1)));

        assertEquals(400, exception.getCode());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(medicine.getTotalQuantity()), "库存不得变化");
        verify(medicineMapper, never()).addStock(anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("补货零 → 抛 InvalidAmountException，库存不变且不触达库存写入")
    void restockWithZeroAmountIsRejected() {
        InvalidAmountException exception = assertThrows(InvalidAmountException.class,
            () -> inventoryService.restockMedicine(medicine.getId(), BigDecimal.ZERO));

        assertEquals(400, exception.getCode());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(medicine.getTotalQuantity()), "库存不得变化");
        verify(medicineMapper, never()).addStock(anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("补货 null → 抛 InvalidAmountException，库存不变")
    void restockWithNullAmountIsRejected() {
        InvalidAmountException exception = assertThrows(InvalidAmountException.class,
            () -> inventoryService.restockMedicine(medicine.getId(), null));

        assertEquals(400, exception.getCode());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(medicine.getTotalQuantity()), "库存不得变化");
        verify(medicineMapper, never()).addStock(anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("正常补货 → 走 addStock 原子累加，只更新状态列而不回写 total_quantity")
    @SuppressWarnings("unchecked")
    void restockAddsStockAtomically() {
        MockServices.fakeAddStock(medicineMapper, medicine);

        inventoryService.restockMedicine(medicine.getId(), BigDecimal.valueOf(50));

        assertEquals(0, BigDecimal.valueOf(150).compareTo(medicine.getTotalQuantity()), "库存应累加 50");
        verify(medicineMapper, times(1)).addStock(medicine.getId(), BigDecimal.valueOf(50));

        ArgumentCaptor<Wrapper<Medicine>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(medicineMapper).update(isNull(), captor.capture());
        String sqlSet = ((UpdateWrapper<Medicine>) captor.getValue()).getSqlSet();
        assertTrue(sqlSet.contains("status"), "补货后应更新状态列，实际 SET 子句: " + sqlSet);
        assertFalse(sqlSet.contains("total_quantity"),
            "不得回写 total_quantity 绝对值（会覆盖并发扣减），实际 SET 子句: " + sqlSet);
    }

    @Test
    @DisplayName("消耗药品 → 走 deductStock 原子扣减，且不再有 setTotalQuantity + updateById 的写绝对值路径")
    void consumeDeductsAtomicallyWithoutWritingAbsoluteQuantity() {
        MockServices.fakeDeductStock(medicineMapper, medicine);

        ConsumeResult result = inventoryService.consumeMedicine(
            medicine.getId(), BigDecimal.valueOf(30), "测试用户", "测试症状");

        assertTrue(result.isSuccess());
        verify(medicineMapper, times(1)).deductStock(medicine.getId(), BigDecimal.valueOf(30));
        assertEquals(0, BigDecimal.valueOf(70).compareTo(result.getNewQuantity()), "剩余库存应为 70");
        assertEquals(0, BigDecimal.valueOf(70).compareTo(
            medicineMapper.selectById(medicine.getId()).getTotalQuantity()), "库中库存应为 70");

        // 关键回归断言：updateById 一旦被用来回写含 total_quantity 的整实体，就是并发丢失更新的老写法
        verify(medicineMapper, never()).updateById(
            argThat((Medicine updated) -> updated != null && updated.getTotalQuantity() != null));
    }

    @Test
    @DisplayName("deductStock 返回 0 行（库存不足）→ 抛 InsufficientStockException，且不写用药记录")
    void consumeWithInsufficientStockIsRejected() {
        when(medicineMapper.deductStock(anyString(), any(BigDecimal.class))).thenReturn(0);

        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
            () -> inventoryService.consumeMedicine(
                medicine.getId(), BigDecimal.valueOf(1000), "测试用户", null));

        assertEquals(422, exception.getCode());
        verify(usageLogMapper, never()).insert(any(UsageLog.class));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(medicine.getTotalQuantity()), "库存不得变化");
    }
}
