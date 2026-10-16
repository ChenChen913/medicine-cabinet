package com.family.medicinecabinet.service;

import com.family.medicinecabinet.entity.enums.ShoppingReason;
import com.family.medicinecabinet.service.dto.ExpiryCheckResult;

/**
 * 过期监控服务接口
 */
public interface ExpiryMonitorService {
    
    /**
     * 每日过期检查（定时任务）
     * 使用 @Scheduled(cron = "0 0 2 * * ?") 每天凌晨2点执行
     * 
     * @return 检查结果
     */
    ExpiryCheckResult runDailyCheck();
    
    /**
     * 标记药品为已过期
     * 
     * @param medicineId 药品ID
     */
    void markExpired(String medicineId);
    
    /**
     * 将药品加入购物清单
     * 
     * @param medicineId 药品ID
     * @param reason 添加原因
     */
    void addToShoppingList(String medicineId, ShoppingReason reason);
}
