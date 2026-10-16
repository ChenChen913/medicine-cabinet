package com.family.medicinecabinet.service.dto;

import com.family.medicinecabinet.entity.Medicine;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 过期检查结果对象
 */
@Data
public class ExpiryCheckResult {
    
    /**
     * 检查的药品数量
     */
    private int checkedCount;
    
    /**
     * 过期的药品数量
     */
    private int expiredCount;
    
    /**
     * 过期的药品列表
     */
    private List<Medicine> expiredMedicines = new ArrayList<>();
    
    /**
     * 添加过期药品
     */
    public void addExpiredMedicine(Medicine medicine) {
        this.expiredMedicines.add(medicine);
        this.expiredCount = this.expiredMedicines.size();
    }
}
