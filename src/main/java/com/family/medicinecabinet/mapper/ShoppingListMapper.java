package com.family.medicinecabinet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.family.medicinecabinet.entity.ShoppingListItem;
import com.family.medicinecabinet.entity.enums.ShoppingStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物清单 Mapper 接口
 */
@Mapper
public interface ShoppingListMapper extends BaseMapper<ShoppingListItem> {

    /**
     * 查询待购买的清单项（含药品名称）
     *
     * <p>状态值取枚举 {@link ShoppingStatus#PENDING} 的名称，不在 SQL 中硬编码字符串状态；
     * SQL 见 mapper/ShoppingListMapper.xml，通过 LEFT JOIN medicines 回填 medicineName。</p>
     */
    default List<ShoppingListItem> findPending() {
        return findPendingByStatus(ShoppingStatus.PENDING.name());
    }

    /**
     * 按状态查询清单项并 LEFT JOIN 回填药品名称
     */
    List<ShoppingListItem> findPendingByStatus(@Param("status") String status);

    /**
     * 根据药品ID和原因查询待购买的清单项（对外签名不变；状态取枚举名由本方法内部传入，
     * SQL 中不再硬编码字符串状态）
     */
    default ShoppingListItem findByMedicineAndReason(@Param("medicineId") String medicineId,
                                                     @Param("reason") String reason) {
        return findByMedicineAndReasonAndStatus(medicineId, reason, ShoppingStatus.PENDING.name());
    }

    /**
     * 按药品ID、原因与状态查询清单项（状态来自参数，SQL 中不硬编码）
     */
    ShoppingListItem findByMedicineAndReasonAndStatus(@Param("medicineId") String medicineId,
                                                      @Param("reason") String reason,
                                                      @Param("status") String status);
}
