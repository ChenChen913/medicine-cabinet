package com.family.medicinecabinet.service;

import com.family.medicinecabinet.service.dto.ConsumeResult;

import java.math.BigDecimal;

/**
 * 库存管理服务接口
 */
public interface InventoryService {
    
    /**
     * 消耗药品
     * 
     * @param medicineId 药品ID
     * @param amount 消耗量
     * @param userName 服用人
     * @param symptom 对应症状
     * @return 操作结果，包含触发的事件和警告
     */
    ConsumeResult consumeMedicine(String medicineId, BigDecimal amount, 
                                 String userName, String symptom);
    
    /**
     * 检查是否低库存
     * 
     * @param medicineId 药品ID
     * @return 是否低库存
     */
    boolean checkLowStock(String medicineId);
    
    /**
     * 处理耗尽的药品（增加频率分数，加入购物清单）
     * 
     * @param medicineId 药品ID
     */
    void handleDepletedMedicine(String medicineId);
    
    /**
     * 补货（更新库存和频率分数）
     * 
     * @param medicineId 药品ID
     * @param amount 补货数量
     */
    void restockMedicine(String medicineId, BigDecimal amount);
}
