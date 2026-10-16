package com.family.medicinecabinet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用药记录实体类
 */
@Data
@TableName("usage_logs")
public class UsageLog {
    
    /**
     * 记录ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    /**
     * 药品ID
     */
    private String medicineId;
    
    /**
     * 服用时间
     */
    private LocalDateTime usageTime;
    
    /**
     * 服用量
     */
    private BigDecimal amount;
    
    /**
     * 服用人
     */
    private String userName;
    
    /**
     * 对应症状
     */
    private String symptom;
    
    /**
     * 备注
     */
    private String notes;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
