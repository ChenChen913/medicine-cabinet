package com.family.medicinecabinet.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.ShoppingListItem;
import com.family.medicinecabinet.entity.enums.ShoppingReason;
import com.family.medicinecabinet.entity.enums.ShoppingStatus;
import com.family.medicinecabinet.exception.InvalidAmountException;
import com.family.medicinecabinet.exception.MedicineException;
import com.family.medicinecabinet.exception.MedicineNotFoundException;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.mapper.ShoppingListMapper;
import com.family.medicinecabinet.support.MockServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 购物清单服务的定向用例（对应冻结契约 S3）
 *
 * <p>重点覆盖"重复标记已购买必须被拒绝且库存不得二次累加"这条幂等不变量。</p>
 */
class ShoppingListServiceImplTest {

    private ShoppingListMapper shoppingListMapper;
    private MedicineMapper medicineMapper;
    private ShoppingListServiceImpl shoppingListService;
    private Medicine medicine;
    private ShoppingListItem item;

    @BeforeEach
    void setUp() {
        shoppingListMapper = mock(ShoppingListMapper.class);
        medicineMapper = mock(MedicineMapper.class);
        shoppingListService = MockServices.shoppingListService(shoppingListMapper, medicineMapper);

        medicine = MockServices.medicine(BigDecimal.valueOf(100), BigDecimal.TEN);
        item = new ShoppingListItem();
        item.setId(UUID.randomUUID().toString());
        item.setMedicineId(medicine.getId());
        item.setReason(ShoppingReason.DEPLETED);
        item.setStatus(ShoppingStatus.PENDING);

        when(shoppingListMapper.selectById(item.getId())).thenReturn(item);
        when(medicineMapper.selectById(medicine.getId())).thenReturn(medicine);
        when(shoppingListMapper.updateById(any(ShoppingListItem.class))).thenReturn(1);
    }

    @Test
    @DisplayName("重复标记已购买的清单项 → 抛 MedicineException(409)，库存不得二次累加")
    void markAsPurchasedIsRejectedWhenAlreadyPurchased() {
        // Given: 清单项已经是已购买状态
        item.setStatus(ShoppingStatus.PURCHASED);

        // When & Then: 再次标记必须被拒绝
        MedicineException exception = assertThrows(MedicineException.class,
            () -> shoppingListService.markAsPurchased(item.getId(), BigDecimal.valueOf(20)));

        assertEquals(409, exception.getCode(), "重复处理应返回 409");
        assertEquals("该清单项已处理", exception.getMessage());

        // 关键回归断言：库存没有被第二次累加，清单项也没有被再次写状态
        verify(medicineMapper, never()).addStock(anyString(), any(BigDecimal.class));
        verify(shoppingListMapper, never()).updateById(any(ShoppingListItem.class));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(medicine.getTotalQuantity()), "库存不得变化");
    }

    @Test
    @DisplayName("购买数量为 0 / null / 负数 → 抛 InvalidAmountException，清单项与库存均不变")
    void markAsPurchasedRejectsNonPositiveQuantity() {
        for (BigDecimal invalid : new BigDecimal[]{null, BigDecimal.ZERO, BigDecimal.valueOf(-5)}) {
            InvalidAmountException exception = assertThrows(InvalidAmountException.class,
                () -> shoppingListService.markAsPurchased(item.getId(), invalid),
                "无效购买数量应被拒绝: " + invalid);
            assertEquals(400, exception.getCode());
        }

        verify(medicineMapper, never()).addStock(anyString(), any(BigDecimal.class));
        verify(shoppingListMapper, never()).updateById(any(ShoppingListItem.class));
        assertEquals(ShoppingStatus.PENDING, item.getStatus(), "被拒绝的请求不得改变清单项状态");
        assertNull(item.getPurchasedAt(), "被拒绝的请求不得写入购买时间");
        assertEquals(0, BigDecimal.valueOf(100).compareTo(medicine.getTotalQuantity()), "库存不得变化");
    }

    @Test
    @DisplayName("正常标记已购买 → 状态置为 PURCHASED 并原子累加库存，不回写 total_quantity 绝对值")
    @SuppressWarnings("unchecked")
    void markAsPurchasedAddsStockOnceAndMarksItemPurchased() {
        MockServices.fakeAddStock(medicineMapper, medicine);

        shoppingListService.markAsPurchased(item.getId(), BigDecimal.valueOf(20));

        // 清单项状态
        assertEquals(ShoppingStatus.PURCHASED, item.getStatus());
        assertNotNull(item.getPurchasedAt(), "购买时间应被写入");
        verify(shoppingListMapper, times(1)).updateById(item);

        // 库存：恰好累加一次，且走原子 addStock
        verify(medicineMapper, times(1)).addStock(medicine.getId(), BigDecimal.valueOf(20));
        assertEquals(0, BigDecimal.valueOf(120).compareTo(medicine.getTotalQuantity()), "库存应为 120");

        // 状态/日期/分数只写各自的列，绝不回写 total_quantity
        ArgumentCaptor<Wrapper<Medicine>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(medicineMapper).update(isNull(), captor.capture());
        String sqlSet = ((UpdateWrapper<Medicine>) captor.getValue()).getSqlSet();
        assertTrue(sqlSet.contains("status"), "应更新状态列，实际 SET 子句: " + sqlSet);
        assertTrue(sqlSet.contains("last_purchase_date"), "应写入最近购买日期，实际 SET 子句: " + sqlSet);
        assertFalse(sqlSet.contains("total_quantity"),
            "不得回写 total_quantity 绝对值（会覆盖并发扣减），实际 SET 子句: " + sqlSet);
    }

    @Test
    @DisplayName("清单项不存在 → 抛 MedicineNotFoundException(404)")
    void markAsPurchasedOnMissingItemThrowsNotFound() {
        when(shoppingListMapper.selectById("not-exist")).thenReturn(null);

        MedicineNotFoundException exception = assertThrows(MedicineNotFoundException.class,
            () -> shoppingListService.markAsPurchased("not-exist", BigDecimal.valueOf(20)));

        assertEquals(404, exception.getCode());
        verify(medicineMapper, never()).addStock(anyString(), any(BigDecimal.class));
    }
}
