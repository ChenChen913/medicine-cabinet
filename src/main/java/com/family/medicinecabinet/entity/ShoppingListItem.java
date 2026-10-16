package com.family.medicinecabinet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.family.medicinecabinet.entity.enums.ShoppingReason;
import com.family.medicinecabinet.entity.enums.ShoppingStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物清单项实体类
 */
@Data
@TableName("shopping_list")
public class ShoppingListItem {
    
    /**
     * 清单项ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    /**
     * 药品ID
     */
    private String medicineId;

    /**
     * 药品名称（非数据库列，由查询语句 LEFT JOIN medicines 回填）
     */
    @TableField(exist = false)
    private String medicineName;

    /**
     * 添加原因
     */
    private ShoppingReason reason;
    
    /**
     * 自定义备注
     */
    private String customNote;
    
    /**
     * 状态
     */
    private ShoppingStatus status = ShoppingStatus.PENDING;
    
    /**
     * 添加时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime addedAt;
    
    /**
     * 购买时间
     */
    private LocalDateTime purchasedAt;
}
