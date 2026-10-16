package com.family.medicinecabinet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.family.medicinecabinet.entity.UsageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用药记录 Mapper 接口
 */
@Mapper
public interface UsageLogMapper extends BaseMapper<UsageLog> {
    
    /**
     * 根据药品ID查询用药记录
     */
    List<UsageLog> findByMedicineId(@Param("medicineId") String medicineId);
    
    /**
     * 根据时间范围查询用药记录
     */
    List<UsageLog> findByDateRange(@Param("start") LocalDateTime start, 
                                   @Param("end") LocalDateTime end);
    
    /**
     * 根据用户查询用药记录
     */
    List<UsageLog> findByUser(@Param("userName") String userName);
}
