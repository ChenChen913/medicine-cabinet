package com.family.medicinecabinet.controller;

import com.family.medicinecabinet.common.Result;
import com.family.medicinecabinet.entity.Category;
import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 类别管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/categories")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    
    /**
     * 获取所有类别
     */
    @GetMapping
    public Result<List<Category>> list() {
        List<Category> categories = categoryService.getAllCategories();
        return Result.success(categories);
    }
    
    /**
     * 创建类别
     */
    @PostMapping
    public Result<Category> create(@RequestBody Category category) {
        Category created = categoryService.createCategory(category);
        return Result.success(created);
    }
    
    /**
     * 获取某类别的药品列表（带智能排序）
     */
    @GetMapping("/{id}/medicines")
    public Result<List<Medicine>> getMedicines(@PathVariable String id) {
        List<Medicine> medicines = categoryService.getMedicinesByCategory(id);
        return Result.success(medicines);
    }
    
    /**
     * 将药品分配到类别
     */
    @PostMapping("/assign")
    public Result<?> assignMedicine(
            @RequestParam String medicineId,
            @RequestParam List<String> categoryIds
    ) {
        categoryService.assignMedicineToCategories(medicineId, categoryIds);
        return Result.success();
    }
}
