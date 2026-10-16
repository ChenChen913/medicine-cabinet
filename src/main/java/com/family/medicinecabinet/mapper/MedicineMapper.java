package com.family.medicinecabinet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.family.medicinecabinet.entity.Medicine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 药品 Mapper 接口
 */
@Mapper
public interface MedicineMapper extends BaseMapper<Medicine> {
    
    /**
     * 根据类别ID查询药品列表（带智能排序）
     * 排序规则：
     * 1. 使用频率分数降序
     * 2. 最近购买日期降序
     * 3. 创建时间降序
     */
    @Select("SELECT m.* FROM medicines m " +
            "INNER JOIN medicine_categories mc ON m.id = mc.medicine_id " +
            "WHERE mc.category_id = #{categoryId} " +
            "ORDER BY m.usage_frequency_score DESC, " +
            "m.last_purchase_date DESC, m.created_at DESC")
    List<Medicine> findByCategoryWithSorting(@Param("categoryId") String categoryId);
    
    /**
     * 查询已过期的药品
     */
    List<Medicine> findExpired(@Param("currentDate") LocalDate currentDate);
    
    /**
     * 查询低库存药品
     */
    @Select("SELECT * FROM medicines WHERE total_quantity < threshold AND status != 'EXPIRED'")
    List<Medicine> findLowStock();
}
