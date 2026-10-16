package com.family.medicinecabinet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.ShoppingListItem;
import com.family.medicinecabinet.entity.enums.MedicineStatus;
import com.family.medicinecabinet.entity.enums.ShoppingReason;
import com.family.medicinecabinet.entity.enums.ShoppingStatus;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.mapper.ShoppingListMapper;
import com.family.medicinecabinet.service.ExpiryMonitorService;
import com.family.medicinecabinet.service.dto.ExpiryCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 过期监控服务实现类
 */
@Slf4j
@Service
public class ExpiryMonitorServiceImpl implements ExpiryMonitorService {
    
    @Autowired
    private MedicineMapper medicineMapper;
    
    @Autowired
    private ShoppingListMapper shoppingListMapper;
    
    /**
     * 每日凌晨2点执行过期检查
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    @Override
    public ExpiryCheckResult runDailyCheck() {
        log.info("开始执行每日过期检查任务");
        
        ExpiryCheckResult result = new ExpiryCheckResult();
        LocalDate currentDate = LocalDate.now();
        
        // 1. 查询所有未标记为过期的药品
        List<Medicine> allMedicines = medicineMapper.selectList(
            new LambdaQueryWrapper<Medicine>()
                .ne(Medicine::getStatus, MedicineStatus.EXPIRED)
        );
        result.setCheckedCount(allMedicines.size());
        
        // 2. 检查每个药品
        for (Medicine medicine : allMedicines) {
            if (medicine.getExpiryDate().isBefore(currentDate)) {
                // 3. 标记为过期
                medicine.setStatus(MedicineStatus.EXPIRED);
                medicineMapper.updateById(medicine);
                
                // 4. 检查是否已在购物清单中
                ShoppingListItem existing = shoppingListMapper.findByMedicineAndReason(
                    medicine.getId(), ShoppingReason.EXPIRED.name()
                );
                
                // 5. 如果不在购物清单，自动添加
                if (existing == null) {
                    ShoppingListItem item = new ShoppingListItem();
                    item.setMedicineId(medicine.getId());
                    item.setReason(ShoppingReason.EXPIRED);
                    item.setStatus(ShoppingStatus.PENDING);
                    shoppingListMapper.insert(item);
                }
                
                result.addExpiredMedicine(medicine);
            }
        }
        
        log.info("过期检查完成：检查 {} 个药品，发现 {} 个过期",
            result.getCheckedCount(), result.getExpiredCount());
        
        return result;
    }
    
    @Override
    public void markExpired(String medicineId) {
        Medicine medicine = new Medicine();
        medicine.setId(medicineId);
        medicine.setStatus(MedicineStatus.EXPIRED);
        medicineMapper.updateById(medicine);
        log.info("标记药品为已过期: {}", medicineId);
    }
    
    @Override
    public void addToShoppingList(String medicineId, ShoppingReason reason) {
        ShoppingListItem item = new ShoppingListItem();
        item.setMedicineId(medicineId);
        item.setReason(reason);
        item.setStatus(ShoppingStatus.PENDING);
        shoppingListMapper.insert(item);
        log.info("将药品加入购物清单: {}, 原因: {}", medicineId, reason);
    }
}
