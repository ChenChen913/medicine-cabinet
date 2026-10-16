package com.family.medicinecabinet.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.ShoppingListItem;
import com.family.medicinecabinet.entity.UsageLog;
import com.family.medicinecabinet.entity.enums.MedicineStatus;
import com.family.medicinecabinet.entity.enums.ShoppingReason;
import com.family.medicinecabinet.entity.enums.ShoppingStatus;
import com.family.medicinecabinet.exception.InsufficientStockException;
import com.family.medicinecabinet.exception.InvalidAmountException;
import com.family.medicinecabinet.exception.MedicineNotFoundException;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.mapper.ShoppingListMapper;
import com.family.medicinecabinet.mapper.UsageLogMapper;
import com.family.medicinecabinet.service.InventoryService;
import com.family.medicinecabinet.service.dto.ConsumeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存管理服务实现类
 *
 * <p>库存增减一律通过 {@link MedicineMapper#deductStock} / {@link MedicineMapper#addStock}
 * 的单条 UPDATE 原子完成，并用返回的受影响行数判断成败。禁止
 * "先读快照、在内存里加减、再整实体回写绝对值"的写法：并发下两个请求会基于同一份旧值
 * 互相覆盖，造成丢失更新与超卖。</p>
 */
@Slf4j
@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private MedicineMapper medicineMapper;

    @Autowired
    private UsageLogMapper usageLogMapper;

    @Autowired
    private ShoppingListMapper shoppingListMapper;

    @Override
    public ConsumeResult consumeMedicine(String medicineId, BigDecimal amount,
                                        String userName, String symptom) {
        log.info("消耗药品: medicineId={}, amount={}, userName={}", medicineId, amount, userName);

        // 1. 数量校验
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("消耗量必须大于0");
        }

        ConsumeResult result = new ConsumeResult();

        // 2. 校验药品存在（同时取名称/单位用于提示信息）
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + medicineId);
        }

        // 3. 原子扣减库存：受影响行数为 0 表示 total_quantity < amount，即库存不足
        int affectedRows = medicineMapper.deductStock(medicineId, amount);
        if (affectedRows == 0) {
            throw new InsufficientStockException(
                String.format("库存不足：当前 %s%s，需要 %s%s",
                    medicine.getTotalQuantity(), medicine.getUnit(),
                    amount, medicine.getUnit())
            );
        }

        // 4. 创建用药记录
        UsageLog usageLog = new UsageLog();
        usageLog.setMedicineId(medicineId);
        usageLog.setUsageTime(LocalDateTime.now());
        usageLog.setAmount(amount);
        usageLog.setUserName(userName);
        usageLog.setSymptom(symptom);
        usageLogMapper.insert(usageLog);
        result.addEvent("usage_log_created");

        // 5. 扣减已在数据库中原子完成，重新读取拿扣减后的权威库存
        //    （本事务已持有该行写锁，读到的必然是扣减后的值，不会与并发扣减相互覆盖）
        Medicine latest = medicineMapper.selectById(medicineId);
        BigDecimal newQuantity = latest.getTotalQuantity();

        // 6. 库存状态迁移；状态与频率分数只写各自的列，不写 total_quantity 绝对值
        UpdateWrapper<Medicine> inventoryUpdate = new UpdateWrapper<>();
        inventoryUpdate.eq("id", medicineId);
        MedicineStatus newStatus;
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            // 耗尽逻辑
            newStatus = MedicineStatus.DEPLETED;
            // 使用频率分数用 SQL 自增（usage_frequency_score = usage_frequency_score + 1），
            // 避免"读-改-写"在并发下丢失计数
            inventoryUpdate.setIncrBy("usage_frequency_score", 1);

            // 加入购物清单
            ShoppingListItem item = new ShoppingListItem();
            item.setMedicineId(medicineId);
            item.setReason(ShoppingReason.DEPLETED);
            item.setStatus(ShoppingStatus.PENDING);
            shoppingListMapper.insert(item);

            result.addEvent("medicine_depleted");
            result.addWarning(latest.getName() + " 已用完，已自动加入购物清单");

        } else if (newQuantity.compareTo(latest.getThreshold()) < 0) {
            // 低库存预警
            newStatus = MedicineStatus.LOW_STOCK;
            result.addEvent("low_stock_warning");
            result.addWarning(String.format("%s 库存不足：剩余 %s%s，低于预警值 %s%s",
                latest.getName(), newQuantity, latest.getUnit(),
                latest.getThreshold(), latest.getUnit()));
        } else {
            newStatus = MedicineStatus.NORMAL;
        }

        // 7. 更新状态（耗尽时附带频率分数自增）
        medicineMapper.update(null, inventoryUpdate.set("status", newStatus.name()));

        result.setSuccess(true);
        result.setNewQuantity(newQuantity);
        result.setMedicineName(latest.getName());

        log.info("消耗药品成功: {}, 剩余: {}{}", latest.getName(), newQuantity, latest.getUnit());

        return result;
    }

    @Override
    public boolean checkLowStock(String medicineId) {
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + medicineId);
        }
        return medicine.getTotalQuantity().compareTo(medicine.getThreshold()) < 0;
    }

    @Override
    public void handleDepletedMedicine(String medicineId) {
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + medicineId);
        }

        // 状态与频率分数用单条 UPDATE 完成，不再回写 total_quantity 绝对值
        medicineMapper.update(null, new UpdateWrapper<Medicine>()
            .eq("id", medicineId)
            .set("status", MedicineStatus.DEPLETED.name())
            .setIncrBy("usage_frequency_score", 1));

        // 加入购物清单
        ShoppingListItem item = new ShoppingListItem();
        item.setMedicineId(medicineId);
        item.setReason(ShoppingReason.DEPLETED);
        item.setStatus(ShoppingStatus.PENDING);
        shoppingListMapper.insert(item);

        log.info("处理耗尽药品: {}", medicine.getName());
    }

    @Override
    public void restockMedicine(String medicineId, BigDecimal amount) {
        // 1. 数量校验：null、0、负数一律拒绝，防止"补货"反向扣减库存
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("补货数量必须大于0");
        }

        // 2. 校验药品存在
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + medicineId);
        }

        // 3. 原子累加库存：受影响行数为 0 表示药品不存在
        int affectedRows = medicineMapper.addStock(medicineId, amount);
        if (affectedRows == 0) {
            throw new MedicineNotFoundException("药品不存在: " + medicineId);
        }

        // 4. 累加已在数据库中原子完成，重新读取拿累加后的权威库存
        Medicine latest = medicineMapper.selectById(medicineId);
        BigDecimal newQuantity = latest.getTotalQuantity();

        // 5. 更新状态（不写 total_quantity 绝对值）
        MedicineStatus newStatus = newQuantity.compareTo(latest.getThreshold()) >= 0
            ? MedicineStatus.NORMAL
            : MedicineStatus.LOW_STOCK;
        medicineMapper.update(null, new UpdateWrapper<Medicine>()
            .eq("id", medicineId)
            .set("status", newStatus.name()));

        log.info("补货成功: {}, 新库存: {}{}", latest.getName(), newQuantity, latest.getUnit());
    }
}
