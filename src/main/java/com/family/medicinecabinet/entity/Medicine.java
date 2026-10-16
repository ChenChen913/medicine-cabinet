package com.family.medicinecabinet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.family.medicinecabinet.entity.enums.DosageForm;
import com.family.medicinecabinet.entity.enums.MedicineStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 药品实体类
 */
@Data
@TableName("medicines")
public class Medicine {
    
    /**
     * 药品ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    /**
     * 药品名称
     */
    private String name;
    
    /**
     * 药品图片URL
     */
    private String imageUrl;
    
    /**
     * 剂型
     */
    @TableField("dosage_form")
    private DosageForm dosageForm;
    
    /**
     * 存放位置
     */
    private String storageLocation;
    
    /**
     * 当前剩余总量
     */
    @TableField("total_quantity")
    private BigDecimal totalQuantity;
    
    /**
     * 单位（粒/ml/包/片等）
     */
    private String unit;
    
    /**
     * 预警阈值
     */
    private BigDecimal threshold;
    
    /**
     * 生产日期
     */
    private LocalDate productionDate;
    
    /**
     * 过期日期
     */
    private LocalDate expiryDate;
    
    /**
     * 最近购买日期
     */
    private LocalDate lastPurchaseDate;
    
    /**
     * 适应症
     */
    private String symptomsTreated;
    
    /**
     * 服用说明
     */
    private String dosageInstruction;
    
    /**
     * 副作用（详情页显示）
     */
    private String sideEffects;
    
    /**
     * 使用频率分数
     */
    @TableField("usage_frequency_score")
    private Integer usageFrequencyScore = 0;
    
    /**
     * 状态
     */
    private MedicineStatus status = MedicineStatus.NORMAL;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
