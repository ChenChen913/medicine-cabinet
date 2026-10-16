package com.family.medicinecabinet.service;

import com.family.medicinecabinet.entity.Medicine;

import java.util.List;

/**
 * 智能排序服务接口
 */
public interface SortingService {
    
    /**
     * 获取某类别的排序后药品列表
     * 
     * @param categoryId 类别ID
     * @return 排序后的药品列表
     */
    List<Medicine> getSortedMedicines(String categoryId);
    
    /**
     * 应用智能排序规则
     * 排序优先级：
     * 1. 使用频率分数降序
     * 2. 最近购买日期降序（null排最后）
     * 3. 创建时间降序
     * 
     * @param medicines 待排序的药品列表
     * @return 排序后的药品列表
     */
    List<Medicine> applySortingRules(List<Medicine> medicines);
}
