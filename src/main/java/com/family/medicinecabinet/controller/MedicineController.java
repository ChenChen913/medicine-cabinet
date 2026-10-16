package com.family.medicinecabinet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.family.medicinecabinet.common.Result;
import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.enums.DosageForm;
import com.family.medicinecabinet.entity.enums.MedicineStatus;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.service.InventoryService;
import com.family.medicinecabinet.service.dto.ConsumeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 药品管理控制器
 */
@Slf4j
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
        return Result.success(medicine);
    }
    
    /**
     * 创建药品
     */
    @PostMapping
    public Result<Medicine> create(@RequestBody Medicine medicine) {
        try {
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
        } catch (Exception e) {
            log.error("创建药品失败", e);
            return Result.error("创建药品失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新药品
     */
    @PutMapping("/{id}")
    public Result<Medicine> update(@PathVariable String id, @RequestBody Medicine medicine) {
        medicine.setId(id);
        medicineMapper.updateById(medicine);
        return Result.success(medicine);
    }
    
    /**
     * 删除药品
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id) {
        medicineMapper.deleteById(id);
        return Result.success();
    }
    
    /**
     * 消耗药品（记录用药）
     */
    @PostMapping("/{id}/consume")
    public Result<ConsumeResult> consume(
            @PathVariable String id,
            @RequestParam BigDecimal amount,
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
            @RequestParam BigDecimal amount
    ) {
        inventoryService.restockMedicine(id, amount);
        return Result.success();
    }
}