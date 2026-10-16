package com.family.medicinecabinet.service;

import com.family.medicinecabinet.entity.ShoppingListItem;
import com.family.medicinecabinet.entity.enums.ShoppingReason;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物清单服务接口
 */
public interface ShoppingListService {
    
    /**
     * 添加药品到购物清单（手动添加）
     * 
     * @param medicineId 药品ID
     * @param reason 添加原因
     * @param customNote 自定义备注
     * @return 购物清单项
     */
    ShoppingListItem addToShoppingList(String medicineId, ShoppingReason reason, String customNote);
    
    /**
     * 标记购物清单项为已购买
     * 
     * @param shoppingItemId 购物清单项ID
     * @param newQuantity 补货数量
     */
    void markAsPurchased(String shoppingItemId, BigDecimal newQuantity);
    
    /**
     * 获取待购买的清单项
     * 
     * @return 待购买清单项列表
     */
    List<ShoppingListItem> getPendingItems();
    
    /**
     * 删除购物清单项
     * 
     * @param shoppingItemId 购物清单项ID
     */
    void deleteItem(String shoppingItemId);
}
