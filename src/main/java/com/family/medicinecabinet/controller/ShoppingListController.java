package com.family.medicinecabinet.controller;

import com.family.medicinecabinet.common.Result;
import com.family.medicinecabinet.entity.ShoppingListItem;
import com.family.medicinecabinet.entity.enums.ShoppingReason;
import com.family.medicinecabinet.service.ShoppingListService;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物清单控制器
 *
 * <p>类级 {@link Validated} 用于开启 {@code @RequestParam} 上的约束校验。</p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/shopping-list")
public class ShoppingListController {
    
    @Autowired
    private ShoppingListService shoppingListService;
    
    /**
     * 获取待购买清单
     */
    @GetMapping("/pending")
    public Result<List<ShoppingListItem>> getPending() {
        List<ShoppingListItem> items = shoppingListService.getPendingItems();
        return Result.success(items);
    }
    
    /**
     * 添加到购物清单
     */
    @PostMapping
    public Result<ShoppingListItem> add(
            @RequestParam String medicineId,
            @RequestParam ShoppingReason reason,
            @RequestParam(required = false) String customNote
    ) {
        ShoppingListItem item = shoppingListService.addToShoppingList(medicineId, reason, customNote);
        return Result.success(item);
    }
    
    /**
     * 标记为已购买
     */
    @PostMapping("/{id}/purchased")
    public Result<?> markPurchased(
            @PathVariable String id,
            @RequestParam @Positive(message = "购买数量必须大于0") BigDecimal quantity
    ) {
        shoppingListService.markAsPurchased(id, quantity);
        return Result.success();
    }
    
    /**
     * 删除清单项
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id) {
        shoppingListService.deleteItem(id);
        return Result.success();
    }
}
