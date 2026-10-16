package com.family.medicinecabinet.service.impl;

import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.ShoppingListItem;
import com.family.medicinecabinet.entity.enums.MedicineStatus;
import com.family.medicinecabinet.entity.enums.ShoppingReason;
import com.family.medicinecabinet.entity.enums.ShoppingStatus;
import com.family.medicinecabinet.exception.MedicineNotFoundException;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.mapper.ShoppingListMapper;
import com.family.medicinecabinet.service.ShoppingListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物清单服务实现类
 */
@Slf4j
@Service
@Transactional
public class ShoppingListServiceImpl implements ShoppingListService {
    
    @Autowired
    private ShoppingListMapper shoppingListMapper;
    
    @Autowired
    private MedicineMapper medicineMapper;
    
    @Override
    public ShoppingListItem addToShoppingList(String medicineId, ShoppingReason reason, String customNote) {
        log.info("添加药品到购物清单: medicineId={}, reason={}", medicineId, reason);
        
        // 验证药品存在
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + medicineId);
        }
        
        // 创建购物清单项
        ShoppingListItem item = new ShoppingListItem();
        item.setMedicineId(medicineId);
        item.setReason(reason);
        item.setCustomNote(customNote);
        item.setStatus(ShoppingStatus.PENDING);
        
        shoppingListMapper.insert(item);
        
        log.info("成功添加到购物清单: {}", medicine.getName());
        return item;
    }
    
    @Override
    public void markAsPurchased(String shoppingItemId, BigDecimal newQuantity) {
        log.info("标记购物清单项为已购买: id={}, quantity={}", shoppingItemId, newQuantity);
        
        // 1. 获取购物清单项
        ShoppingListItem item = shoppingListMapper.selectById(shoppingItemId);
        if (item == null) {
            throw new RuntimeException("购物清单项不存在: " + shoppingItemId);
        }
        
        // 2. 获取对应药品
        Medicine medicine = medicineMapper.selectById(item.getMedicineId());
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + item.getMedicineId());
        }
        
        // 3. 更新购物清单状态
        item.setStatus(ShoppingStatus.PURCHASED);
        item.setPurchasedAt(LocalDateTime.now());
        shoppingListMapper.updateById(item);
        
        // 4. 更新药品库存和频率分数
        BigDecimal totalQuantity = medicine.getTotalQuantity().add(newQuantity);
        medicine.setTotalQuantity(totalQuantity);
        medicine.setUsageFrequencyScore(medicine.getUsageFrequencyScore() + 1);
        medicine.setLastPurchaseDate(LocalDate.now());
        
        // 5. 更新药品状态
        if (totalQuantity.compareTo(medicine.getThreshold()) >= 0) {
            medicine.setStatus(MedicineStatus.NORMAL);
        } else {
            medicine.setStatus(MedicineStatus.LOW_STOCK);
        }
        
        medicineMapper.updateById(medicine);
        
        log.info("购物清单项已标记为已购买，药品库存已更新: {}, 新库存: {}{}", 
            medicine.getName(), totalQuantity, medicine.getUnit());
    }
    
    @Override
    public List<ShoppingListItem> getPendingItems() {
        return shoppingListMapper.findPending();
    }
    
    @Override
    public void deleteItem(String shoppingItemId) {
        log.info("删除购物清单项: {}", shoppingItemId);
        shoppingListMapper.deleteById(shoppingItemId);
    }
}
