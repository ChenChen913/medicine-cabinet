package com.family.medicinecabinet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.family.medicinecabinet.common.Result;
import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.enums.DosageForm;
import com.family.medicinecabinet.entity.enums.MedicineStatus;
import com.family.medicinecabinet.exception.MedicineNotFoundException;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.service.InventoryService;
import com.family.medicinecabinet.service.dto.ConsumeResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 药品管理控制器
 *
 * <p>类级 {@link Validated} 用于开启 {@code @RequestParam} 等非请求体参数上的约束校验；
 * 请求体参数使用 {@link Valid} 触发实体自身声明的约束。</p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/medicines")
public class MedicineController {

    @Autowired
    private MedicineMapper medicineMapper;

    @Autowired
    private InventoryService inventoryService;

    /**
     * 分页查询药品列表
     */
    @GetMapping
    public Result<Page<Medicine>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name
    ) {
        Page<Medicine> page = new Page<>(current, size);
        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();

        if (name != null && !name.isEmpty()) {
            wrapper.like(Medicine::getName, name);
        }

        medicineMapper.selectPage(page, wrapper);
        return Result.success(page);
    }

    /**
     * 根据ID查询药品详情
     */
    @GetMapping("/{id}")
    public Result<Medicine> getById(@PathVariable String id) {
        Medicine medicine = medicineMapper.selectById(id);
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + id);
        }
        return Result.success(medicine);
    }

    /**
     * 创建药品
     *
     * <p>实体已移除字段初始化器，null 兜底必须在这里显式完成。
     * 不捕获异常：底层错误交给 {@code GlobalExceptionHandler} 统一处理，
     * 避免把 SQL/驱动原始报错透传给前端。</p>
     */
    @PostMapping
    public Result<Medicine> create(@Valid @RequestBody Medicine medicine) {
        // 设置默认值，防止空指针异常
        if (medicine.getTotalQuantity() == null) {
            medicine.setTotalQuantity(BigDecimal.ZERO);
        }
        if (medicine.getThreshold() == null) {
            medicine.setThreshold(BigDecimal.ZERO);
        }
        if (medicine.getUsageFrequencyScore() == null) {
            medicine.setUsageFrequencyScore(0);
        }
        if (medicine.getDosageForm() == null) {
            medicine.setDosageForm(DosageForm.OTHER);
        }
        if (medicine.getStatus() == null) {
            medicine.setStatus(MedicineStatus.NORMAL);
        }
        if (medicine.getExpiryDate() == null) {
            medicine.setExpiryDate(LocalDate.now().plusYears(1));
        }

        medicineMapper.insert(medicine);
        return Result.success(medicine);
    }

    /**
     * 更新药品
     *
     * <p>注意：这里<strong>不能</strong>对 {@code status} / {@code usageFrequencyScore} 做 null 兜底。
     * 请求体未携带的字段在实体中为 null，MyBatis-Plus 会把它排除在 UPDATE 之外；
     * 一旦补默认值，就等于用默认值覆盖用户没提交的字段（已过期/低库存状态与排序分数被重置）。</p>
     */
    @PutMapping("/{id}")
    public Result<Medicine> update(@PathVariable String id, @Valid @RequestBody Medicine medicine) {
        medicine.setId(id);
        int affectedRows = medicineMapper.updateById(medicine);
        if (affectedRows == 0) {
            throw new MedicineNotFoundException("药品不存在: " + id);
        }
        return Result.success(medicine);
    }

    /**
     * 删除药品
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id) {
        int affectedRows = medicineMapper.deleteById(id);
        if (affectedRows == 0) {
            throw new MedicineNotFoundException("药品不存在: " + id);
        }
        return Result.success();
    }

    /**
     * 消耗药品（记录用药）
     */
    @PostMapping("/{id}/consume")
    public Result<ConsumeResult> consume(
            @PathVariable String id,
            @RequestParam @Positive(message = "消耗量必须大于0") BigDecimal amount,
            @RequestParam String userName,
            @RequestParam(required = false) String symptom
    ) {
        ConsumeResult result = inventoryService.consumeMedicine(id, amount, userName, symptom);
        return Result.success(result);
    }

    /**
     * 补货
     */
    @PostMapping("/{id}/restock")
    public Result<?> restock(
            @PathVariable String id,
            @RequestParam @Positive(message = "补货数量必须大于0") BigDecimal amount
    ) {
        inventoryService.restockMedicine(id, amount);
        return Result.success();
    }
}
