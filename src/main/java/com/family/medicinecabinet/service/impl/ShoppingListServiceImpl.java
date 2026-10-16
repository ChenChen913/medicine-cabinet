package com.family.medicinecabinet.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.ShoppingListItem;
import com.family.medicinecabinet.entity.enums.MedicineStatus;
import com.family.medicinecabinet.entity.enums.ShoppingReason;
import com.family.medicinecabinet.entity.enums.ShoppingStatus;
import com.family.medicinecabinet.exception.InvalidAmountException;
import com.family.medicinecabinet.exception.MedicineException;
import com.family.medicinecabinet.exception.MedicineNotFoundException;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.mapper.ShoppingListMapper;
import com.family.medicinecabinet.service.ShoppingListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物清单服务实现类
 */
@Slf4j
@Service
@Transactional
public class ShoppingListServiceImpl implements ShoppingListService {

    @Autowired
    private ShoppingListMapper shoppingListMapper;

    @Autowired
    private MedicineMapper medicineMapper;

    @Override
    public ShoppingListItem addToShoppingList(String medicineId, ShoppingReason reason, String customNote) {
        log.info("添加药品到购物清单: medicineId={}, reason={}", medicineId, reason);

        // 验证药品存在
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + medicineId);
        }

        // 创建购物清单项
        ShoppingListItem item = new ShoppingListItem();
        item.setMedicineId(medicineId);
        item.setReason(reason);
        item.setCustomNote(customNote);
        item.setStatus(ShoppingStatus.PENDING);

        shoppingListMapper.insert(item);

        log.info("成功添加到购物清单: {}", medicine.getName());
        return item;
    }

    @Override
    public void markAsPurchased(String shoppingItemId, BigDecimal newQuantity) {
        log.info("标记购物清单项为已购买: id={}, quantity={}", shoppingItemId, newQuantity);

        // 1. 数量校验：null、0、负数一律拒绝
        if (newQuantity == null || newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("购买数量必须大于0");
        }

        // 2. 获取购物清单项
        ShoppingListItem item = shoppingListMapper.selectById(shoppingItemId);
        if (item == null) {
            throw new MedicineNotFoundException("购物清单项不存在: " + shoppingItemId);
        }

        // 3. 幂等保护：先校验状态。已处理的清单项直接拒绝，
        //    避免重复点击导致库存被重复累加、使用频率分数虚增
        if (ShoppingStatus.PURCHASED.equals(item.getStatus())) {
            throw new MedicineException(409, "该清单项已处理");
        }

        // 4. 获取对应药品
        Medicine medicine = medicineMapper.selectById(item.getMedicineId());
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + item.getMedicineId());
        }

        // 5. 更新购物清单状态
        item.setStatus(ShoppingStatus.PURCHASED);
        item.setPurchasedAt(LocalDateTime.now());
        shoppingListMapper.updateById(item);

        // 6. 库存原子累加：受影响行数为 0 表示药品不存在
        int affectedRows = medicineMapper.addStock(medicine.getId(), newQuantity);
        if (affectedRows == 0) {
            throw new MedicineNotFoundException("药品不存在: " + item.getMedicineId());
        }

        // 7. 累加已在数据库中原子完成，重新读取拿权威库存，
        //    状态 / 最近购买日期 / 频率分数只写各自的列，不回写 total_quantity 绝对值
        Medicine latest = medicineMapper.selectById(medicine.getId());
        BigDecimal totalQuantity = latest.getTotalQuantity();
        MedicineStatus newStatus = totalQuantity.compareTo(latest.getThreshold()) >= 0
            ? MedicineStatus.NORMAL
            : MedicineStatus.LOW_STOCK;

        medicineMapper.update(null, new UpdateWrapper<Medicine>()
            .eq("id", latest.getId())
            .set("status", newStatus.name())
            .set("last_purchase_date", LocalDate.now())
            .setIncrBy("usage_frequency_score", 1));

        log.info("购物清单项已标记为已购买，药品库存已更新: {}, 新库存: {}{}",
            latest.getName(), totalQuantity, latest.getUnit());
    }

    @Override
    public List<ShoppingListItem> getPendingItems() {
        return shoppingListMapper.findPending();
    }

    @Override
    public void deleteItem(String shoppingItemId) {
        log.info("删除购物清单项: {}", shoppingItemId);
        shoppingListMapper.deleteById(shoppingItemId);
    }
}
