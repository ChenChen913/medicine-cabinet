package com.family.medicinecabinet.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类别实体类
 *
 * <p>字段约束注解与 {@code schema.sql} 中 categories 表的 NOT NULL 声明对应。</p>
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
    @NotBlank(message = "类别名称不能为空")
    @Size(max = 100, message = "类别名称长度不能超过100")
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
