package com.family.medicinecabinet.service.impl;

import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.ShoppingListItem;
import com.family.medicinecabinet.entity.UsageLog;
import com.family.medicinecabinet.entity.enums.MedicineStatus;
import com.family.medicinecabinet.entity.enums.ShoppingReason;
import com.family.medicinecabinet.entity.enums.ShoppingStatus;
import com.family.medicinecabinet.exception.InsufficientStockException;
import com.family.medicinecabinet.exception.InvalidAmountException;
import com.family.medicinecabinet.exception.MedicineNotFoundException;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.mapper.ShoppingListMapper;
import com.family.medicinecabinet.mapper.UsageLogMapper;
import com.family.medicinecabinet.service.InventoryService;
import com.family.medicinecabinet.service.dto.ConsumeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存管理服务实现类
 */
@Slf4j
@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {
    
    @Autowired
    private MedicineMapper medicineMapper;
    
    @Autowired
    private UsageLogMapper usageLogMapper;
    
    @Autowired
    private ShoppingListMapper shoppingListMapper;
    
    @Override
    public ConsumeResult consumeMedicine(String medicineId, BigDecimal amount,
                                        String userName, String symptom) {
        log.info("消耗药品: medicineId={}, amount={}, userName={}", medicineId, amount, userName);
        
        ConsumeResult result = new ConsumeResult();
        
        // 1. 获取药品信息
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + medicineId);
        }
        
        // 2. 验证库存
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("消耗量必须大于0");
        }
        if (amount.compareTo(medicine.getTotalQuantity()) > 0) {
            throw new InsufficientStockException(
                String.format("库存不足：当前 %s%s，需要 %s%s",
                    medicine.getTotalQuantity(), medicine.getUnit(),
                    amount, medicine.getUnit())
            );
        }
        
        // 3. 创建用药记录
        UsageLog usageLog = new UsageLog();
        usageLog.setMedicineId(medicineId);
        usageLog.setUsageTime(LocalDateTime.now());
        usageLog.setAmount(amount);
        usageLog.setUserName(userName);
        usageLog.setSymptom(symptom);
        usageLogMapper.insert(usageLog);
        result.addEvent("usage_log_created");
        
        // 4. 扣减库存
        BigDecimal newQuantity = medicine.getTotalQuantity().subtract(amount);
        
        // 5. 检查库存状态
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            // 耗尽逻辑
            medicine.setUsageFrequencyScore(medicine.getUsageFrequencyScore() + 1);
            medicine.setStatus(MedicineStatus.DEPLETED);
            
            // 加入购物清单
            ShoppingListItem item = new ShoppingListItem();
            item.setMedicineId(medicineId);
            item.setReason(ShoppingReason.DEPLETED);
            item.setStatus(ShoppingStatus.PENDING);
            shoppingListMapper.insert(item);
            
            result.addEvent("medicine_depleted");
            result.addWarning(medicine.getName() + " 已用完，已自动加入购物清单");
            
        } else if (newQuantity.compareTo(medicine.getThreshold()) < 0) {
            // 低库存预警
            medicine.setStatus(MedicineStatus.LOW_STOCK);
            result.addEvent("low_stock_warning");
            result.addWarning(String.format("%s 库存不足：剩余 %s%s，低于预警值 %s%s",
                medicine.getName(), newQuantity, medicine.getUnit(),
                medicine.getThreshold(), medicine.getUnit()));
        } else {
            medicine.setStatus(MedicineStatus.NORMAL);
        }
        
        // 6. 更新药品
        medicine.setTotalQuantity(newQuantity);
        medicineMapper.updateById(medicine);
        
        result.setSuccess(true);
        result.setNewQuantity(newQuantity);
        result.setMedicineName(medicine.getName());
        
        log.info("消耗药品成功: {}, 剩余: {}{}", medicine.getName(), newQuantity, medicine.getUnit());
        
        return result;
    }
    
    @Override
    public boolean checkLowStock(String medicineId) {
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + medicineId);
        }
        return medicine.getTotalQuantity().compareTo(medicine.getThreshold()) < 0;
    }
    
    @Override
    public void handleDepletedMedicine(String medicineId) {
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + medicineId);
        }
        
        // 增加频率分数
        medicine.setUsageFrequencyScore(medicine.getUsageFrequencyScore() + 1);
        medicine.setStatus(MedicineStatus.DEPLETED);
        medicineMapper.updateById(medicine);
        
        // 加入购物清单
        ShoppingListItem item = new ShoppingListItem();
        item.setMedicineId(medicineId);
        item.setReason(ShoppingReason.DEPLETED);
        item.setStatus(ShoppingStatus.PENDING);
        shoppingListMapper.insert(item);
        
        log.info("处理耗尽药品: {}, 频率分数: {}", medicine.getName(), medicine.getUsageFrequencyScore());
    }
    
    @Override
    public void restockMedicine(String medicineId, BigDecimal amount) {
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + medicineId);
        }
        
        BigDecimal newQuantity = medicine.getTotalQuantity().add(amount);
        medicine.setTotalQuantity(newQuantity);
        
        // 更新状态
        if (newQuantity.compareTo(medicine.getThreshold()) >= 0) {
            medicine.setStatus(MedicineStatus.NORMAL);
        } else {
            medicine.setStatus(MedicineStatus.LOW_STOCK);
        }
        
        medicineMapper.updateById(medicine);
        
        log.info("补货成功: {}, 新库存: {}{}", medicine.getName(), newQuantity, medicine.getUnit());
    }
}
