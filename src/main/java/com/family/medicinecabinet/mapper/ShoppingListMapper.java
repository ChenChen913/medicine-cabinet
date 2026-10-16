package com.family.medicinecabinet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.family.medicinecabinet.entity.ShoppingListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 购物清单 Mapper 接口
 */
@Mapper
public interface ShoppingListMapper extends BaseMapper<ShoppingListItem> {
    
    /**
     * 查询待购买的清单项
     */
    @Select("SELECT * FROM shopping_list WHERE status = 'PENDING' ORDER BY added_at DESC")
    List<ShoppingListItem> findPending();
    
    /**
     * 根据药品ID和原因查询清单项
     */
    ShoppingListItem findByMedicineAndReason(@Param("medicineId") String medicineId,
                                            @Param("reason") String reason);
}
