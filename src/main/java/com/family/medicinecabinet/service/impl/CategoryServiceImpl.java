package com.family.medicinecabinet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.family.medicinecabinet.entity.Category;
import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.exception.DuplicateCategoryException;
import com.family.medicinecabinet.exception.MedicineNotFoundException;
import com.family.medicinecabinet.mapper.CategoryMapper;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.service.CategoryService;
import com.family.medicinecabinet.service.SortingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 类别管理服务实现类
 */
@Slf4j
@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {
    
    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private MedicineMapper medicineMapper;
    
    @Autowired
    private SortingService sortingService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public Category createCategory(Category category) {
        log.info("创建类别: {}", category.getName());
        
        // 检查名称唯一性
        Category existing = categoryMapper.selectOne(
            new LambdaQueryWrapper<Category>()
                .eq(Category::getName, category.getName())
        );
        
        if (existing != null) {
            throw new DuplicateCategoryException("类别名称已存在: " + category.getName());
        }
        
        categoryMapper.insert(category);
        log.info("类别创建成功: {}", category.getName());
        
        return category;
    }
    
    @Override
    public void assignMedicineToCategories(String medicineId, List<String> categoryIds) {
        log.info("将药品 {} 分配到 {} 个类别", medicineId, categoryIds.size());
        
        // 验证药品存在
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + medicineId);
        }
        
        // 先删除现有关联
        jdbcTemplate.update(
            "DELETE FROM medicine_categories WHERE medicine_id = ?",
            medicineId
        );
        
        // 添加新关联
        for (String categoryId : categoryIds) {
            jdbcTemplate.update(
                "INSERT INTO medicine_categories (medicine_id, category_id) VALUES (?, ?)",
                medicineId, categoryId
            );
        }
        
        log.info("药品类别分配完成");
    }
    
    @Override
    public List<Medicine> getMedicinesByCategory(String categoryId) {
        // 使用智能排序服务获取排序后的药品列表
        return sortingService.getSortedMedicines(categoryId);
    }
    
    @Override
    public List<Category> getAllCategories() {
        return categoryMapper.selectList(null);
    }
}
