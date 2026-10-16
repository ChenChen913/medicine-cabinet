package com.family.medicinecabinet.support;

import com.family.medicinecabinet.controller.MedicineController;
import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.enums.DosageForm;
import com.family.medicinecabinet.entity.enums.MedicineStatus;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.mapper.ShoppingListMapper;
import com.family.medicinecabinet.mapper.UsageLogMapper;
import com.family.medicinecabinet.service.impl.InventoryServiceImpl;
import com.family.medicinecabinet.service.impl.ShoppingListServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 测试夹具：用 mock Mapper + 反射注入字段的方式构造被测的 Service / Controller 实例。
 *
 * <p>{@link #fakeAddStock} / {@link #fakeDeductStock} 用 Mockito Answer 等价模拟数据库里
 * {@code total_quantity = total_quantity ± ?} 的原子语义（库存不足时返回 0 行）。
 * 这样属性测试与定向用例断言的是"生产代码调用 Mapper 之后的状态"，
 * 而不是断言测试自己刚 set 进去的局部变量。</p>
 */
public final class MockServices {

    private MockServices() {
    }

    public static InventoryServiceImpl inventoryService(MedicineMapper medicineMapper,
                                                       UsageLogMapper usageLogMapper,
                                                       ShoppingListMapper shoppingListMapper) {
        InventoryServiceImpl service = new InventoryServiceImpl();
        setField(service, "medicineMapper", medicineMapper);
        setField(service, "usageLogMapper", usageLogMapper);
        setField(service, "shoppingListMapper", shoppingListMapper);
        return service;
    }

    public static ShoppingListServiceImpl shoppingListService(ShoppingListMapper shoppingListMapper,
                                                             MedicineMapper medicineMapper) {
        ShoppingListServiceImpl service = new ShoppingListServiceImpl();
        setField(service, "shoppingListMapper", shoppingListMapper);
        setField(service, "medicineMapper", medicineMapper);
        return service;
    }

    public static MedicineController medicineController(MedicineMapper medicineMapper) {
        MedicineController controller = new MedicineController();
        setField(controller, "medicineMapper", medicineMapper);
        return controller;
    }

    /**
     * 构造一个字段完整的测试药品。
     */
    public static Medicine medicine(BigDecimal totalQuantity, BigDecimal threshold) {
        Medicine medicine = new Medicine();
        medicine.setId(UUID.randomUUID().toString());
        medicine.setName("测试药品");
        medicine.setDosageForm(DosageForm.TABLET);
        medicine.setUnit("粒");
        medicine.setTotalQuantity(totalQuantity);
        medicine.setThreshold(threshold);
        medicine.setExpiryDate(LocalDate.now().plusYears(1));
        medicine.setUsageFrequencyScore(0);
        medicine.setStatus(MedicineStatus.NORMAL);
        return medicine;
    }

    /**
     * 模拟数据库的原子累加：{@code UPDATE medicines SET total_quantity = total_quantity + ? WHERE id = ?}
     */
    public static void fakeAddStock(MedicineMapper medicineMapper, Medicine medicine) {
        when(medicineMapper.addStock(anyString(), any(BigDecimal.class))).thenAnswer(invocation -> {
            BigDecimal amount = invocation.getArgument(1);
            medicine.setTotalQuantity(medicine.getTotalQuantity().add(amount));
            return 1;
        });
    }

    /**
     * 模拟数据库的原子扣减：
     * {@code UPDATE medicines SET total_quantity = total_quantity - ? WHERE id = ? AND total_quantity >= ?}
     * —— 库存不足时返回 0 行。
     */
    public static void fakeDeductStock(MedicineMapper medicineMapper, Medicine medicine) {
        when(medicineMapper.deductStock(anyString(), any(BigDecimal.class))).thenAnswer(invocation -> {
            BigDecimal amount = invocation.getArgument(1);
            if (medicine.getTotalQuantity().compareTo(amount) < 0) {
                return 0;
            }
            medicine.setTotalQuantity(medicine.getTotalQuantity().subtract(amount));
            return 1;
        });
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("反射注入字段失败: " + fieldName, e);
        }
    }
}
