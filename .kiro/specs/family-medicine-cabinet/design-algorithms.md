## 核心业务算法

### 1. 智能排序算法 (Smart Sorting Algorithm)

这是系统的核心算法之一，确保用户能快速找到常用药品。

#### 算法逻辑

```python
def apply_smart_sorting(medicines: List[Medicine]) -> List[Medicine]:
    """
    应用智能排序规则到药品列表
    
    排序优先级：
    1. 使用频率分数 (usage_frequency_score) 降序
    2. 最近购买日期 (last_purchase_date) 降序
    3. 创建时间 (created_at) 降序（兜底）
    """
    def sort_key(medicine: Medicine) -> tuple:
        # 第一优先级：使用频率分数（越高越靠前，所以取负数）
        frequency = -medicine.usage_frequency_score
        
        # 第二优先级：购买日期（越新越靠前）
        # 如果没有购买日期，使用一个很早的日期作为默认值
        purchase_date = medicine.last_purchase_date or date(1900, 1, 1)
        # 转换为时间戳并取负数（越大越靠前）
        purchase_timestamp = -purchase_date.timestamp()
        
        # 第三优先级：创建时间（兜底逻辑）
        created_timestamp = -medicine.created_at.timestamp() if medicine.created_at else 0
        
        return (frequency, purchase_timestamp, created_timestamp)
    
    return sorted(medicines, key=sort_key)
```

#### SQL 实现

```sql
-- 获取某个类别的排序后药品列表
SELECT m.*
FROM medicines m
INNER JOIN medicine_categories mc ON m.id = mc.medicine_id
WHERE mc.category_id = :category_id
  AND m.status != 'expired'
ORDER BY 
  m.usage_frequency_score DESC,  -- 第一优先级
  m.last_purchase_date DESC NULLS LAST,  -- 第二优先级
  m.created_at DESC;  -- 第三优先级（兜底）
```

### 2. 库存消耗与自动补货闭环

这是系统的核心业务流程，实现从消耗到补货的全自动化。

#### 消耗药品流程

```python
async def consume_medicine(
    medicine_id: str,
    amount: Decimal,
    user_name: str,
    symptom: str
) -> Dict[str, Any]:
    """
    消耗药品并触发相关业务逻辑
    
    返回：包含操作结果和触发的事件
    """
    result = {
        'success': False,
        'events': [],
        'warnings': []
    }
    
    # 1. 获取药品信息
    medicine = await medicine_repo.find_by_id(medicine_id)
    if not medicine:
        raise MedicineNotFoundException(f"药品 {medicine_id} 不存在")
    
    # 2. 验证消耗量
    if amount <= 0:
        raise InvalidAmountException("消耗量必须大于0")
    
    if amount > medicine.total_quantity:
        raise InsufficientStockException(
            f"库存不足：当前 {medicine.total_quantity}{medicine.unit}，需要 {amount}{medicine.unit}"
        )
    
    # 3. 创建用药记录
    usage_log = UsageLog(
        id=generate_uuid(),
        medicine_id=medicine_id,
        usage_time=datetime.now(),
        amount=amount,
        user_name=user_name,
        symptom=symptom
    )
    await usage_log_repo.create(usage_log)
    result['events'].append('usage_log_created')
    
    # 4. 扣减库存
    old_quantity = medicine.total_quantity
    new_quantity = old_quantity - amount
    medicine.total_quantity = new_quantity
    
    # 5. 检查库存状态并触发相应逻辑
    
    # 5.1 检查是否耗尽
    if new_quantity == 0:
        # 耗尽逻辑：增加频率分数 + 加入购物清单
        medicine.usage_frequency_score += 1
        medicine.status = MedicineStatus.DEPLETED
        
        # 自动加入购物清单
        shopping_item = ShoppingListItem(
            id=generate_uuid(),
            medicine_id=medicine_id,
            reason=ShoppingReason.DEPLETED,
            status=ShoppingStatus.PENDING,
            added_at=datetime.now()
        )
        await shopping_list_repo.create(shopping_item)
        
        result['events'].append('medicine_depleted')
        result['events'].append('added_to_shopping_list')
        result['warnings'].append(f"{medicine.name} 已用完，已自动加入购物清单")
    
    # 5.2 检查是否低于预警阈值
    elif new_quantity < medicine.threshold:
        medicine.status = MedicineStatus.LOW_STOCK
        result['events'].append('low_stock_warning')
        result['warnings'].append(
            f"{medicine.name} 库存不足：剩余 {new_quantity}{medicine.unit}，"
            f"低于预警值 {medicine.threshold}{medicine.unit}"
        )
    else:
        medicine.status = MedicineStatus.NORMAL
    
    # 6. 更新药品信息
    await medicine_repo.update(medicine_id, {
        'total_quantity': new_quantity,
        'usage_frequency_score': medicine.usage_frequency_score,
        'status': medicine.status
    })
    
    result['success'] = True
    result['new_quantity'] = new_quantity
    result['medicine_name'] = medicine.name
    
    return result
```

### 3. 过期监控定时任务

每日自动检查过期药品并触发补货流程。

```python
async def run_daily_expiry_check() -> Dict[str, Any]:
    """
    每日过期检查定时任务
    
    应该通过 cron 或定时任务调度器每天运行一次
    """
    result = {
        'checked_count': 0,
        'expired_count': 0,
        'expired_medicines': []
    }
    
    current_date = date.today()
    
    # 1. 获取所有未标记为过期的药品
    all_medicines = await medicine_repo.find_all()
    result['checked_count'] = len(all_medicines)
    
    # 2. 检查每个药品的过期日期
    for medicine in all_medicines:
        # 跳过已经标记为过期的药品
        if medicine.status == MedicineStatus.EXPIRED:
            continue
        
        # 检查是否过期
        if medicine.expiry_date < current_date:
            # 3. 标记为过期
            medicine.status = MedicineStatus.EXPIRED
            await medicine_repo.update(medicine.id, {
                'status': MedicineStatus.EXPIRED
            })
            
            # 4. 检查是否已在购物清单中
            existing_item = await shopping_list_repo.find_by_medicine_and_reason(
                medicine.id,
                ShoppingReason.EXPIRED
            )
            
            # 5. 如果不在购物清单中，自动添加
            if not existing_item:
                shopping_item = ShoppingListItem(
                    id=generate_uuid(),
                    medicine_id=medicine.id,
                    reason=ShoppingReason.EXPIRED,
                    status=ShoppingStatus.PENDING,
                    added_at=datetime.now()
                )
                await shopping_list_repo.create(shopping_item)
            
            result['expired_count'] += 1
            result['expired_medicines'].append({
                'id': medicine.id,
                'name': medicine.name,
                'expiry_date': medicine.expiry_date.isoformat()
            })
    
    # 6. 发送通知（如果有过期药品）
    if result['expired_count'] > 0:
        await notification_service.send_expiry_alert(result['expired_medicines'])
    
    return result
```

### 4. 补货完成流程

用户购买药品后更新系统状态。

```python
async def mark_shopping_item_purchased(
    shopping_item_id: str,
    new_quantity: Decimal
) -> Dict[str, Any]:
    """
    标记购物清单项为已购买，并更新药品库存
    """
    # 1. 获取购物清单项
    shopping_item = await shopping_list_repo.find_by_id(shopping_item_id)
    if not shopping_item:
        raise ShoppingItemNotFoundException()
    
    # 2. 获取对应的药品
    medicine = await medicine_repo.find_by_id(shopping_item.medicine_id)
    if not medicine:
        raise MedicineNotFoundException()
    
    # 3. 更新购物清单状态
    shopping_item.status = ShoppingStatus.PURCHASED
    shopping_item.purchased_at = datetime.now()
    await shopping_list_repo.update(shopping_item_id, {
        'status': ShoppingStatus.PURCHASED,
        'purchased_at': shopping_item.purchased_at
    })
    
    # 4. 更新药品库存和频率分数
    medicine.total_quantity += new_quantity
    medicine.usage_frequency_score += 1  # 补货时增加频率
    medicine.last_purchase_date = date.today()
    
    # 5. 更新药品状态
    if medicine.total_quantity >= medicine.threshold:
        medicine.status = MedicineStatus.NORMAL
    else:
        medicine.status = MedicineStatus.LOW_STOCK
    
    await medicine_repo.update(medicine.id, {
        'total_quantity': medicine.total_quantity,
        'usage_frequency_score': medicine.usage_frequency_score,
        'last_purchase_date': medicine.last_purchase_date,
        'status': medicine.status
    })
    
    return {
        'success': True,
        'medicine_name': medicine.name,
        'new_quantity': medicine.total_quantity,
        'new_frequency_score': medicine.usage_frequency_score
    }
```

### 5. 前端数据结构设计

#### 药品卡片数据结构 (列表页)

```typescript
interface MedicineCardData {
  id: string
  name: string
  imageUrl?: string
  totalQuantity: number
  unit: string
  symptomsTreated: string  // 适应症
  dosageInstruction: string  // 服用说明
  status: 'normal' | 'low_stock' | 'expired' | 'depleted'
  storageLocation?: string
  
  // 注意：副作用 (sideEffects) 不包含在卡片数据中
}

// 渲染函数示例
function renderMedicineCard(data: MedicineCardData): string {
  return `
    <div class="medicine-card ${data.status}">
      <img src="${data.imageUrl || '/default-medicine.png'}" />
      <h3>${data.name}</h3>
      <div class="quantity">
        剩余：${data.totalQuantity} ${data.unit}
      </div>
      <div class="symptoms">
        <strong>适应症：</strong>${data.symptomsTreated}
      </div>
      <div class="dosage">
        <strong>用法：</strong>${data.dosageInstruction}
      </div>
      ${data.storageLocation ? `<div class="location">位置：${data.storageLocation}</div>` : ''}
      <button onclick="showDetails('${data.id}')">查看详情</button>
    </div>
  `
}
```

#### 药品详情数据结构 (详情页)

```typescript
interface MedicineDetailData extends MedicineCardData {
  // 详情页额外包含的字段
  sideEffects: string  // 副作用（仅在详情页显示）
  dosageForm: string  // 剂型
  productionDate?: string
  expiryDate: string
  threshold: number
  usageFrequencyScore: number
  categories: string[]
  
  // 用药历史
  recentUsageLogs: UsageLogSummary[]
}

interface UsageLogSummary {
  usageTime: string
  amount: number
  userName: string
  symptom?: string
}

// 详情页渲染函数
function renderMedicineDetail(data: MedicineDetailData): string {
  return `
    <div class="medicine-detail-modal">
      <h2>${data.name}</h2>
      <img src="${data.imageUrl || '/default-medicine.png'}" />
      
      <section class="basic-info">
        <p><strong>剂型：</strong>${data.dosageForm}</p>
        <p><strong>库存：</strong>${data.totalQuantity} ${data.unit}</p>
        <p><strong>预警阈值：</strong>${data.threshold} ${data.unit}</p>
        <p><strong>存放位置：</strong>${data.storageLocation || '未设置'}</p>
      </section>
      
      <section class="dates">
        <p><strong>生产日期：</strong>${data.productionDate || '未知'}</p>
        <p><strong>过期日期：</strong>${data.expiryDate}</p>
      </section>
      
      <section class="medical-info">
        <h3>医疗说明</h3>
        <p><strong>适应症：</strong>${data.symptomsTreated}</p>
        <p><strong>用法用量：</strong>${data.dosageInstruction}</p>
        <p><strong>副作用：</strong>${data.sideEffects}</p>
      </section>
      
      <section class="usage-history">
        <h3>最近用药记录</h3>
        ${renderUsageHistory(data.recentUsageLogs)}
      </section>
    </div>
  `
}
```

#### 购物清单数据结构

```typescript
interface ShoppingListItemData {
  id: string
  medicineId: string
  medicineName: string
  reason: 'expired' | 'depleted' | 'manual'
  customNote?: string
  status: 'pending' | 'purchased'
  addedAt: string
  
  // 关联的药品信息
  medicine: {
    imageUrl?: string
    unit: string
    lastQuantity: number
  }
}

// 购物清单渲染
function renderShoppingList(items: ShoppingListItemData[]): string {
  const pendingItems = items.filter(item => item.status === 'pending')
  
  return `
    <div class="shopping-list">
      <h2>待购清单 (${pendingItems.length})</h2>
      ${pendingItems.map(item => `
        <div class="shopping-item">
          <img src="${item.medicine.imageUrl || '/default-medicine.png'}" />
          <div class="info">
           