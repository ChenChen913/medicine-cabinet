package com.family.medicinecabinet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.family.medicinecabinet.entity.enums.DosageForm;
import com.family.medicinecabinet.entity.enums.MedicineStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 药品实体类
 *
 * <p>字段约束注解与 {@code schema.sql} 中 medicines 表的 NOT NULL 声明一一对应，
 * 使缺必填字段的请求在进入 Controller 前就被拒绝（400），而不是落到 SQL 层报 500。</p>
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
    @NotBlank(message = "药品名称不能为空")
    @Size(max = 255, message = "药品名称长度不能超过255")
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
    @DecimalMin(value = "0", message = "库存总量不能为负数")
    private BigDecimal totalQuantity;

    /**
     * 单位（粒/ml/包/片等）
     */
    @NotBlank(message = "单位不能为空")
    @Size(max = 20, message = "单位长度不能超过20")
    private String unit;

    /**
     * 预警阈值
     */
    @DecimalMin(value = "0", message = "预警阈值不能为负数")
    private BigDecimal threshold;
    
    /**
     * 生产日期
     */
    private LocalDate productionDate;
    
    /**
     * 过期日期
     */
    @NotNull(message = "过期日期不能为空")
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
    private Integer usageFrequencyScore;

    /**
     * 状态
     */
    private MedicineStatus status;
    
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
