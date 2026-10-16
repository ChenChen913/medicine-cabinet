package com.family.medicinecabinet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类别实体类
 */
@Data
@TableName("categories")
public class Category {
    
    /**
     * 类别ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    /**
     * 类别名称
     */
    private String name;
    
    /**
     * 类别描述
     */
    private String description;
    
    /**
     * 图标
     */
    private String icon;
    
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
