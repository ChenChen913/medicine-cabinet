package com.family.medicinecabinet.service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 消耗药品结果对象
 */
@Data
public class ConsumeResult {
    
    /**
     * 操作是否成功
     */
    private boolean success;
    
    /**
     * 药品名称
     */
    private String medicineName;
    
    /**
     * 新的库存数量
     */
    private BigDecimal newQuantity;
    
    /**
     * 触发的事件列表
     */
    private List<String> events = new ArrayList<>();
    
    /**
     * 警告信息列表
     */
    private List<String> warnings = new ArrayList<>();
    
    /**
     * 添加事件
     */
    public void addEvent(String event) {
        this.events.add(event);
    }
    
    /**
     * 添加警告
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
