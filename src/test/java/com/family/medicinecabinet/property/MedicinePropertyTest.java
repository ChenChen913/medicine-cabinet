package com.family.medicinecabinet.property;

import com.family.medicinecabinet.common.Result;
import com.family.medicinecabinet.controller.MedicineController;
import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.enums.DosageForm;
import com.family.medicinecabinet.exception.InvalidAmountException;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.mapper.ShoppingListMapper;
import com.family.medicinecabinet.mapper.UsageLogMapper;
import com.family.medicinecabinet.service.InventoryService;
import com.family.medicinecabinet.support.MockServices;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.NotEmpty;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 药品属性测试类
 * 使用 jqwik 进行基于属性的测试
 *
 * <p>三个属性都真实调用生产方法（{@code MedicineController.create} /
 * {@code InventoryServiceImpl.restockMedicine}），断言对象是生产代码的行为结果。</p>
 */
@PropertyDefaults(tries = 100)
public class MedicinePropertyTest {

    // Feature: family-medicine-cabinet, Property 1: 药品创建完整性
    @Property
    @Label("对于任意有效的药品输入数据，创建药品后返回的对象应该包含所有必需字段（缺省字段由生产代码兜底）")
    void medicineCreationIntegrity(
            @ForAll @NotBlank String name,
            @ForAll DosageForm dosageForm,
            @ForAll @NotEmpty String unit,
            @ForAll @BigRange(min = "0", max = "1000") BigDecimal quantity
    ) {
        // Given: 只提供业务字段；threshold / status / usageFrequencyScore / expiryDate 故意留空
        MedicineMapper medicineMapper = mock(MedicineMapper.class);
        when(medicineMapper.insert(any(Medicine.class))).thenReturn(1);
        MedicineController controller = MockServices.medicineController(medicineMapper);

        Medicine input = new Medicine();
        input.setName(name);
        input.setDosageForm(dosageForm);
        input.setUnit(unit);
        input.setTotalQuantity(quantity);

        // When: 真实调用生产代码创建药品
        Result<Medicine> result = controller.create(input);

        // Then: 返回对象必需字段完整，且入参字段原样保留
        Medicine created = result.getData();
        assertAll(
            () -> assertEquals(200, result.getCode(), "创建应返回成功码"),
            () -> assertNotNull(created, "返回对象不能为空"),
            () -> assertEquals(name, created.getName(), "名称应与入参一致"),
            () -> assertEquals(dosageForm, created.getDosageForm(), "剂型应与入参一致"),
            () -> assertEquals(unit, created.getUnit(), "单位应与入参一致"),
            () -> assertEquals(0, quantity.compareTo(created.getTotalQuantity()), "总量应与入参一致"),
            () -> assertNotNull(created.getThreshold(), "预警阈值不能为空"),
            () -> assertNotNull(created.getExpiryDate(), "过期日期不能为空"),
            () -> assertNotNull(created.getUsageFrequencyScore(), "使用频率分数不能为空"),
            () -> assertNotNull(created.getStatus(), "状态不能为空")
        );
    }

    // Feature: family-medicine-cabinet, Property 2: 最小单位存储一致性
    @Property
    @Label("对于任意正数补货量，补货后读回的库存应该等于原库存加上补货量（最小单位存储，不丢失精度）")
    void minimalUnitStorageConsistency(
            @ForAll @BigRange(min = "0.01", max = "1000") BigDecimal amount
    ) {
        // Given: 库存为 100 的药品，以及等价模拟数据库原子累加的 Mapper
        MedicineMapper medicineMapper = mock(MedicineMapper.class);
        UsageLogMapper usageLogMapper = mock(UsageLogMapper.class);
        ShoppingListMapper shoppingListMapper = mock(ShoppingListMapper.class);

        Medicine medicine = MockServices.medicine(BigDecimal.valueOf(100), BigDecimal.TEN);
        InventoryService inventoryService = MockServices.inventoryService(
            medicineMapper, usageLogMapper, shoppingListMapper);

        when(medicineMapper.selectById(medicine.getId())).thenReturn(medicine);
        MockServices.fakeAddStock(medicineMapper, medicine);

        // When: 真实调用生产代码补货
        inventoryService.restockMedicine(medicine.getId(), amount);

        // Then: 从 Mapper 读回的库存等于"原库存 + 补货量"
        BigDecimal expected = BigDecimal.valueOf(100).add(amount);
        BigDecimal stored = medicineMapper.selectById(medicine.getId()).getTotalQuantity();
        assertEquals(0, expected.compareTo(stored),
            String.format("读回库存应等于原库存加补货量: 期望 %s, 实际 %s", expected, stored));
        verify(medicineMapper, times(1)).addStock(medicine.getId(), amount);
    }

    // Feature: family-medicine-cabinet, Property 3: 库存数量验证
    @Property
    @Label("对于任意 null、负数或零的补货数量，系统应该拒绝更新操作并抛出 InvalidAmountException")
    void inventoryQuantityValidation(
            @ForAll("invalidQuantity") BigDecimal invalidQuantity
    ) {
        // Given: 库存为 100 的药品
        MedicineMapper medicineMapper = mock(MedicineMapper.class);
        Medicine medicine = MockServices.medicine(BigDecimal.valueOf(100), BigDecimal.TEN);
        when(medicineMapper.selectById(medicine.getId())).thenReturn(medicine);

        InventoryService inventoryService = MockServices.inventoryService(
            medicineMapper, mock(UsageLogMapper.class), mock(ShoppingListMapper.class));

        // When & Then: 真实调用生产代码，无效数量必须被拒绝
        InvalidAmountException exception = assertThrows(InvalidAmountException.class,
            () -> inventoryService.restockMedicine(medicine.getId(), invalidQuantity),
            "无效补货数量应该被拒绝");
        assertEquals(400, exception.getCode(), "无效数量应返回 400");
        assertNotNull(exception.getMessage(), "异常应携带可读信息");

        // 验证原库存未被改变，且拒绝路径不得触达库存写入
        assertEquals(0, BigDecimal.valueOf(100).compareTo(medicine.getTotalQuantity()),
            "被拒绝的补货不得改变库存");
        verify(medicineMapper, never()).addStock(anyString(), any(BigDecimal.class));
    }

    /**
     * 提供无效数量的生成器（负数、零，以及 null）
     */
    @Provide
    Arbitrary<BigDecimal> invalidQuantity() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(-1000), BigDecimal.ZERO)
                .edgeCases(config -> config.add(BigDecimal.ZERO))
                .injectNull(0.1);
    }
}
