package com.family.medicinecabinet.service;

import com.family.medicinecabinet.entity.Category;
import com.family.medicinecabinet.entity.Medicine;

import java.util.List;

/**
 * 类别管理服务接口
 */
public interface CategoryService {
    
    /**
     * 创建类别（检查名称唯一性）
     * 
     * @param category 类别对象
     * @return 创建的类别
     */
    Category createCategory(Category category);
    
    /**
     * 将药品分配到多个类别
     * 
     * @param medicineId 药品ID
     * @param categoryIds 类别ID列表
     */
    void assignMedicineToCategories(String medicineId, List<String> categoryIds);
    
    /**
     * 获取某类别的药品列表（带智能排序）
     * 
     * @param categoryId 类别ID
     * @return 药品列表
     */
    List<Medicine> getMedicinesByCategory(String categoryId);
    
    /**
     * 获取所有类别
     * 
     * @return 类别列表
     */
    List<Category> getAllCategories();
}
