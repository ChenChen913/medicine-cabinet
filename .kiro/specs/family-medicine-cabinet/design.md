# 设计文档

## 概述

家庭智慧药箱管理系统采用关系型数据库设计，以最小颗粒度存储药品库存，通过智能算法实现药品排序、自动预警和补货闭环。系统分为数据层、业务逻辑层和展示层三个主要部分。

### 技术栈

**后端：**
- Java 17+
- Spring Boot 3.x
- MyBatis-Plus（数据访问层）
- MySQL 8.0
- Spring Scheduler（定时任务）
- Lombok（简化代码）

**前端：**
- Vue 3 + TypeScript
- **Ant Design Vue Pro**（企业级中后台前端解决方案）
  - 开箱即用的高质量 Vue 组件
  - 完整的设计规范和最佳实践
  - 丰富的页面模板和业务组件
- Vite（构建工具）
- Pinia（状态管理）

核心设计理念：
- **最小单位存储**：所有库存以最小单位（粒/ml/包）计量
- **智能排序**：基于使用频率和购买日期的多级排序算法
- **自动化闭环**：从消耗到补货的全自动化流程
- **分离关注点**：卡片展示简要信息，详情页展示完整信息
- **企业级UI**：设计稿原计划使用 Ant Design Pro（**实际未按此实现**——落地为 ant-design-vue，非 Pro，见「前端设计」章节开头说明与 `frontend/`）

## 架构

系统采用前后端分离的三层架构：

```
┌──────────────────────────────────────────────────┐
│         前端层 (Vue 3 + Ant Design Pro)          │
│  - 药品管理页面（ProTable + ProCard）            │
│  - 药品详情抽屉（ProDescriptions）               │
│  - 购物清单页面（ProList）                       │
│  - 数据统计看板（ProCard + Charts）              │
└──────────────────────────────────────────────────┘
                        ↓ REST API
┌──────────────────────────────────────────────────┐
│         后端层 (Spring Boot)                     │
│                                                  │
│  Controller 层：                                 │
│  - MedicineController                           │
│  - UsageLogController                           │
│  - ShoppingListController                       │
│                                                  │
│  Service 层：                                    │
│  - InventoryService (库存管理)                  │
│  - SortingService (智能排序)                    │
│  - ExpiryMonitorService (过期监控)              │
│                                                  │
│  Mapper 层 (MyBatis-Plus)：                     │
│  - MedicineMapper                               │
│  - UsageLogMapper                               │
│  - ShoppingListMapper                           │
└──────────────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────┐
│              数据层 (MySQL 8.0)                  │
│  - medicines (药品表)                            │
│  - categories (类别表)                           │
│  - usage_logs (用药记录表)                       │
│  - shopping_list (购物清单表)                    │
└──────────────────────────────────────────────────┘
```

## 组件和接口

### 1. 数据访问层组件 (Mapper)

#### MedicineMapper
负责药品数据的CRUD操作（使用 MyBatis-Plus）。

```java
@Mapper
public interface MedicineMapper extends BaseMapper<Medicine> {
    /**
     * 根据类别ID查询药品列表（带智能排序）
     */
    @Select("SELECT m.* FROM medicines m " +
            "INNER JOIN medicine_categories mc ON m.id = mc.medicine_id " +
            "WHERE mc.category_id = #{categoryId} " +
            "ORDER BY m.usage_frequency_score DESC, " +
            "m.last_purchase_date DESC, m.created_at DESC")
    List<Medicine> findByCategoryWithSorting(@Param("categoryId") String categoryId);
    
    /**
     * 查询已过期的药品
     */
    List<Medicine> findExpired(@Param("currentDate") LocalDate currentDate);
    
    /**
     * 查询低库存药品
     */
    @Select("SELECT * FROM medicines WHERE total_quantity < threshold AND status != 'EXPIRED'")
    List<Medicine> findLowStock();
}
```

#### UsageLogMapper
负责用药记录的存储和查询。

```java
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
```

#### ShoppingListMapper
负责购物清单的管理。

```java
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
```

### 2. 业务逻辑层组件 (Service)

#### InventoryService
核心库存管理服务。

```java
@Service
@Transactional
public interface InventoryService {
    /**
     * 消耗药品
     * @return 操作结果，包含触发的事件和警告
     */
    ConsumeResult consumeMedicine(String medicineId, BigDecimal amount, 
                                 String userName, String symptom);
    
    /**
     * 检查是否低库存
     */
    boolean checkLowStock(String medicineId);
    
    /**
     * 处理耗尽的药品（增加频率分数，加入购物清单）
     */
    void handleDepletedMedicine(String medicineId);
    
    /**
     * 补货（更新库存和频率分数）
     */
    void restockMedicine(String medicineId, BigDecimal amount);
}
```

#### SortingService
智能排序服务。

```java
@Service
public interface SortingService {
    /**
     * 获取某类别的排序后药品列表
     */
    List<Medicine> getSortedMedicines(String categoryId);
    
    /**
     * 应用智能排序规则
     */
    List<Medicine> applySortingRules(List<Medicine> medicines);
}
```

#### ExpiryMonitorService
过期监控服务（定时任务）。

```java
@Service
public interface ExpiryMonitorService {
    /**
     * 每日过期检查（定时任务）
     * 使用 @Scheduled(cron = "0 0 2 * * ?") 每天凌晨2点执行
     */
    ExpiryCheckResult runDailyCheck();
    
    /**
     * 标记药品为已过期
     */
    void markExpired(String medicineId);
    
    /**
     * 将过期药品加入购物清单
     */
    void addToShoppingList(String medicineId, ShoppingReason reason);
}
```

## 数据模型

### ER 图描述

**实体关系：**
- `Medicine` (药品) 1:N `UsageLog` (用药记录)
- `Medicine` (药品) N:M `Category` (类别) 通过 `MedicineCategory` 关联表
- `Medicine` (药品) 1:N `ShoppingListItem` (购物清单项)

### 数据库表结构

#### 1. medicines (药品主表)

```sql
CREATE TABLE medicines (
  -- 主键
  id VARCHAR(36) PRIMARY KEY,
  
  -- 基础信息
  name VARCHAR(255) NOT NULL,
  image_url VARCHAR(500),
  dosage_form ENUM('TABLET', 'CAPSULE', 'GRANULE', 'LIQUID', 'EXTERNAL', 'INJECTION', 'OTHER') NOT NULL,
  storage_location VARCHAR(255),
  
  -- 库存相关
  total_quantity DECIMAL(10, 2) NOT NULL DEFAULT 0,
  unit VARCHAR(20) NOT NULL, -- 粒/ml/包/片等
  threshold DECIMAL(10, 2) NOT NULL DEFAULT 10,
  
  -- 时间相关
  production_date DATE,
  expiry_date DATE NOT NULL,
  last_purchase_date DATE,
  
  -- 医疗说明
  symptoms_treated TEXT, -- 适应症
  dosage_instruction VARCHAR(500), -- 服用说明
  side_effects TEXT, -- 副作用（详情页显示）
  
  -- 算法字段
  usage_frequency_score INT NOT NULL DEFAULT 0,
  
  -- 状态
  status ENUM('NORMAL', 'LOW_STOCK', 'EXPIRED', 'DEPLETED') NOT NULL DEFAULT 'NORMAL',
  
  -- 审计字段
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_expiry_date (expiry_date),
  INDEX idx_status (status),
  INDEX idx_usage_frequency (usage_frequency_score DESC)
);
```

#### 2. categories (类别表)

```sql
CREATE TABLE categories (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  description TEXT,
  icon VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 3. medicine_categories (药品-类别关联表)

```sql
CREATE TABLE medicine_categories (
  medicine_id VARCHAR(36) NOT NULL,
  category_id VARCHAR(36) NOT NULL,
  PRIMARY KEY (medicine_id, category_id),
  FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE,
  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);
```

#### 4. usage_logs (用药记录表)

```sql
CREATE TABLE usage_logs (
  id VARCHAR(36) PRIMARY KEY,
  medicine_id VARCHAR(36) NOT NULL,
  usage_time TIMESTAMP NOT NULL,
  amount DECIMAL(10, 2) NOT NULL,
  user_name VARCHAR(100) NOT NULL,
  symptom VARCHAR(255),
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE,
  INDEX idx_medicine_id (medicine_id),
  INDEX idx_usage_time (usage_time DESC),
  INDEX idx_user_name (user_name)
);
```

#### 5. shopping_list (购物清单表)

```sql
CREATE TABLE shopping_list (
  id VARCHAR(36) PRIMARY KEY,
  medicine_id VARCHAR(36) NOT NULL,
  reason ENUM('EXPIRED', 'DEPLETED', 'MANUAL') NOT NULL,
  custom_note TEXT,
  status ENUM('PENDING', 'PURCHASED') NOT NULL DEFAULT 'PENDING',
  added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  purchased_at TIMESTAMP NULL,
  
  FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE,
  INDEX idx_status (status),
  INDEX idx_added_at (added_at DESC)
);
```

### Java 实体类模型

```java
@Data
@TableName("medicines")
public class Medicine {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String name;
    private String imageUrl;
    
    @TableField("dosage_form")
    private DosageForm dosageForm;
    
    private String storageLocation;
    
    @TableField("total_quantity")
    private BigDecimal totalQuantity;
    
    private String unit;
    private BigDecimal threshold;
    
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private LocalDate lastPurchaseDate;
    
    private String symptomsTreated;
    private String dosageInstruction;
    private String sideEffects;
    
    @TableField("usage_frequency_score")
    private Integer usageFrequencyScore = 0;
    
    private MedicineStatus status = MedicineStatus.NORMAL;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

@Data
@TableName("categories")
public class Category {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String name;
    private String description;
    private String icon;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

@Data
@TableName("usage_logs")
public class UsageLog {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String medicineId;
    private LocalDateTime usageTime;
    private BigDecimal amount;
    private String userName;
    private String symptom;
    private String notes;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

@Data
@TableName("shopping_list")
public class ShoppingListItem {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String medicineId;
    private ShoppingReason reason;
    private String customNote;
    private ShoppingStatus status = ShoppingStatus.PENDING;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime addedAt;
    
    private LocalDateTime purchasedAt;
}

// 枚举类型
public enum DosageForm {
    TABLET, CAPSULE, GRANULE, LIQUID, EXTERNAL, INJECTION, OTHER
}

public enum MedicineStatus {
    NORMAL, LOW_STOCK, EXPIRED, DEPLETED
}

public enum ShoppingReason {
    EXPIRED, DEPLETED, MANUAL
}

public enum ShoppingStatus {
    PENDING, PURCHASED
}
```

## 正确性属性

*属性是一个特征或行为，应该在系统的所有有效执行中保持为真——本质上是关于系统应该做什么的形式化陈述。属性作为人类可读规范和机器可验证正确性保证之间的桥梁。*

### 属性反思与去重

经过分析，以下属性存在逻辑冗余，需要合并：
- 属性1.1、1.3、1.5 可以合并为"药品创建完整性"属性
- 属性5.1和5.2 可以合并为"卡片渲染正确性"属性
- 属性5.3和5.4 可以合并为"详情页渲染完整性"属性
- 属性10.1、10.2、10.3 可以合并为"序列化round-trip"属性

### 核心属性列表

**属性 1: 药品创建完整性**
*对于任意*有效的药品输入数据（包含名称、剂型、单位、数量），创建药品后查询该药品，应该返回包含所有必需字段（名称、剂型、单位、总量、购买日期）的完整对象。
**验证需求: 1.1, 1.3, 1.5**

**属性 2: 最小单位存储一致性**
*对于任意*药品和任意正数数量，以最小单位存储后读取，返回的数量应该与存储时的数量相等。
**验证需求: 1.2**

**属性 3: 库存数量验证**
*对于任意*负数、零或非数字的库存输入，系统应该拒绝更新操作，并保持原有库存不变。
**验证需求: 1.4**

**属性 4: 库存扣减不变量**
*对于任意*药品和任意有效的消耗量（0 < 消耗量 ≤ 当前库存），记录用药后的库存应该等于原库存减去消耗量。
**验证需求: 2.2**

**属性 5: 低库存预警触发**
*对于任意*药品，当库存扣减后的剩余量低于预警阈值时，系统应该生成低库存提醒。
**验证需求: 2.3, 2.4**

**属性 6: 耗尽自动补货**
*对于任意*药品，当库存归零时，系统应该将使用频率分数递增1，并自动将该药品添加到购物清单（原因为"耗尽"）。
**验证需求: 2.5**

**属性 7: 智能排序第一优先级**
*对于任意*药品列表，排序后的结果应该首先按使用频率分数降序排列，即对于列表中任意相邻的两个药品A和B，如果A排在B前面，则A的频率分数 ≥ B的频率分数。
**验证需求: 3.1**

**属性 8: 智能排序第二优先级**
*对于任意*具有相同使用频率分数的药品子集，这些药品应该按最近购买日期降序排列（新购买的在前）。
**验证需求: 3.2**

**属性 9: 排序完整性不变量**
*对于任意*药品列表，排序前后的药品数量应该相等，且排序后的列表应该包含原列表中的所有药品（无遗漏、无重复）。
**验证需求: 3.4**

**属性 10: 过期自动标记**
*对于任意*过期日期早于当前日期的药品，执行过期检查后，该药品的状态应该被标记为"已过期"。
**验证需求: 4.2**

**属性 11: 过期自动加入购物清单**
*对于任意*被标记为已过期的药品，该药品应该自动出现在购物清单中，且添加原因为"过期"。
**验证需求: 4.3**

**属性 12: 卡片渲染正确性**
*对于任意*药品，渲染的卡片应该包含药品名称、剩余数量、单位、适应症和服用说明，但不应该包含副作用信息。
**验证需求: 5.1, 5.2**

**属性 13: 详情页渲染完整性**
*对于任意*药品，渲染的详情页应该包含所有医疗说明字段（副作用、生产日期、过期日期、存放位置）。
**验证需求: 5.3, 5.4**

**属性 14: 数据更新一致性**
*对于任意*药品和任意有效的更新数据，更新后立即查询该药品，返回的数据应该反映最新的更新值。
**验证需求: 5.5**

**属性 15: 购物清单标记已买更新频率**
*对于任意*购物清单项，当标记为"已买"时，对应药品的使用频率分数应该递增1。
**验证需求: 6.4**

**属性 16: 类别名称唯一性**
*对于任意*两个不同的类别，它们的名称应该不相同；尝试创建同名类别应该被系统拒绝。
**验证需求: 7.4**

**属性 17: 药品多类别关联**
*对于任意*药品和任意多个类别，该药品应该能够同时关联到所有这些类别，且查询任一类别都能找到该药品。
**验证需求: 7.5**

**属性 18: 类别筛选正确性**
*对于任意*类别，按该类别筛选后的药品列表应该只包含属于该类别的药品，不包含其他类别的药品。
**验证需求: 7.2, 7.3**

**属性 19: 序列化round-trip**
*对于任意*有效的药品对象，序列化为JSON后再反序列化，应该得到与原对象等价的对象（所有字段值相等）。
**验证需求: 10.1, 10.2, 10.3**

**属性 20: 数据操作失败保持完整性**
*对于任意*会导致失败的数据操作（如违反约束），操作失败后数据库状态应该与操作前保持一致（无部分更新）。
**验证需求: 10.5**

## 核心业务算法

### 1. 智能排序算法

智能排序是系统的核心功能，确保用户能快速找到常用药品。

#### 排序规则

1. **第一优先级**：使用频率分数 (usage_frequency_score) 降序
2. **第二优先级**：最近购买日期 (last_purchase_date) 降序
3. **第三优先级**：创建时间 (created_at) 降序（兜底）

#### Java 实现

```java
@Service
public class SortingServiceImpl implements SortingService {
    
    @Override
    public List<Medicine> applySortingRules(List<Medicine> medicines) {
        return medicines.stream()
            .sorted(Comparator
                // 第一优先级：使用频率分数降序
                .comparing(Medicine::getUsageFrequencyScore, Comparator.reverseOrder())
                // 第二优先级：购买日期降序（null 排最后）
                .thenComparing(Medicine::getLastPurchaseDate, 
                    Comparator.nullsLast(Comparator.reverseOrder()))
                // 第三优先级：创建时间降序
                .thenComparing(Medicine::getCreatedAt, Comparator.reverseOrder())
            )
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Medicine> getSortedMedicines(String categoryId) {
        // 直接使用 Mapper 的排序查询
        return medicineMapper.findByCategoryWithSorting(categoryId);
    }
}
```

#### SQL 实现

```sql
SELECT m.*
FROM medicines m
INNER JOIN medicine_categories mc ON m.id = mc.medicine_id
WHERE mc.category_id = :category_id
ORDER BY 
  m.usage_frequency_score DESC,
  m.last_purchase_date DESC NULLS LAST,
  m.created_at DESC;
```

### 2. 库存消耗流程

```java
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
        ConsumeResult result = new ConsumeResult();
        
        // 1. 获取药品信息
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在: " + medicineId);
        }
        
        // 2. 验证库存
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("消耗量必须大于0");
        }
        if (amount.compareTo(medicine.getTotalQuantity()) > 0) {
            throw new InsufficientStockException(
                String.format("库存不足：当前 %s%s，需要 %s%s",
                    medicine.getTotalQuantity(), medicine.getUnit(),
                    amount, medicine.getUnit())
            );
        }
        
        // 3. 创建用药记录
        UsageLog usageLog = new UsageLog();
        usageLog.setMedicineId(medicineId);
        usageLog.setUsageTime(LocalDateTime.now());
        usageLog.setAmount(amount);
        usageLog.setUserName(userName);
        usageLog.setSymptom(symptom);
        usageLogMapper.insert(usageLog);
        result.addEvent("usage_log_created");
        
        // 4. 扣减库存
        BigDecimal newQuantity = medicine.getTotalQuantity().subtract(amount);
        
        // 5. 检查库存状态
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            // 耗尽逻辑
            medicine.setUsageFrequencyScore(medicine.getUsageFrequencyScore() + 1);
            medicine.setStatus(MedicineStatus.DEPLETED);
            
            // 加入购物清单
            ShoppingListItem item = new ShoppingListItem();
            item.setMedicineId(medicineId);
            item.setReason(ShoppingReason.DEPLETED);
            item.setStatus(ShoppingStatus.PENDING);
            shoppingListMapper.insert(item);
            
            result.addEvent("medicine_depleted");
            result.addWarning(medicine.getName() + " 已用完，已自动加入购物清单");
            
        } else if (newQuantity.compareTo(medicine.getThreshold()) < 0) {
            // 低库存预警
            medicine.setStatus(MedicineStatus.LOW_STOCK);
            result.addEvent("low_stock_warning");
            result.addWarning(String.format("%s 库存不足：剩余 %s%s，低于预警值 %s%s",
                medicine.getName(), newQuantity, medicine.getUnit(),
                medicine.getThreshold(), medicine.getUnit()));
        } else {
            medicine.setStatus(MedicineStatus.NORMAL);
        }
        
        // 6. 更新药品
        medicine.setTotalQuantity(newQuantity);
        medicineMapper.updateById(medicine);
        
        result.setSuccess(true);
        result.setNewQuantity(newQuantity);
        result.setMedicineName(medicine.getName());
        
        return result;
    }
}
```

### 3. 过期监控定时任务

```java
@Service
@Slf4j
public class ExpiryMonitorServiceImpl implements ExpiryMonitorService {
    
    @Autowired
    private MedicineMapper medicineMapper;
    
    @Autowired
    private ShoppingListMapper shoppingListMapper;
    
    /**
     * 每日凌晨2点执行过期检查
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    @Override
    public ExpiryCheckResult runDailyCheck() {
        log.info("开始执行每日过期检查任务");
        
        ExpiryCheckResult result = new ExpiryCheckResult();
        LocalDate currentDate = LocalDate.now();
        
        // 1. 查询所有未标记为过期的药品
        List<Medicine> allMedicines = medicineMapper.selectList(
            new LambdaQueryWrapper<Medicine>()
                .ne(Medicine::getStatus, MedicineStatus.EXPIRED)
        );
        result.setCheckedCount(allMedicines.size());
        
        // 2. 检查每个药品
        for (Medicine medicine : allMedicines) {
            if (medicine.getExpiryDate().isBefore(currentDate)) {
                // 3. 标记为过期
                medicine.setStatus(MedicineStatus.EXPIRED);
                medicineMapper.updateById(medicine);
                
                // 4. 检查是否已在购物清单中
                ShoppingListItem existing = shoppingListMapper.findByMedicineAndReason(
                    medicine.getId(), ShoppingReason.EXPIRED.name()
                );
                
                // 5. 如果不在购物清单，自动添加
                if (existing == null) {
                    ShoppingListItem item = new ShoppingListItem();
                    item.setMedicineId(medicine.getId());
                    item.setReason(ShoppingReason.EXPIRED);
                    item.setStatus(ShoppingStatus.PENDING);
                    shoppingListMapper.insert(item);
                }
                
                result.addExpiredMedicine(medicine);
            }
        }
        
        log.info("过期检查完成：检查 {} 个药品，发现 {} 个过期",
            result.getCheckedCount(), result.getExpiredCount());
        
        return result;
    }
    
    @Override
    public void markExpired(String medicineId) {
        Medicine medicine = new Medicine();
        medicine.setId(medicineId);
        medicine.setStatus(MedicineStatus.EXPIRED);
        medicineMapper.updateById(medicine);
    }
    
    @Override
    public void addToShoppingList(String medicineId, ShoppingReason reason) {
        ShoppingListItem item = new ShoppingListItem();
        item.setMedicineId(medicineId);
        item.setReason(reason);
        item.setStatus(ShoppingStatus.PENDING);
        shoppingListMapper.insert(item);
    }
}
```

### 4. 补货完成流程

```java
@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {
    
    @Override
    public RestockResult markShoppingItemPurchased(String shoppingItemId, 
                                                   BigDecimal newQuantity) {
        // 1. 获取购物清单项
        ShoppingListItem item = shoppingListMapper.selectById(shoppingItemId);
        if (item == null) {
            throw new ShoppingItemNotFoundException("购物清单项不存在");
        }
        
        // 2. 获取对应药品
        Medicine medicine = medicineMapper.selectById(item.getMedicineId());
        if (medicine == null) {
            throw new MedicineNotFoundException("药品不存在");
        }
        
        // 3. 更新购物清单状态
        item.setStatus(ShoppingStatus.PURCHASED);
        item.setPurchasedAt(LocalDateTime.now());
        shoppingListMapper.updateById(item);
        
        // 4. 更新药品库存和频率分数
        BigDecimal totalQuantity = medicine.getTotalQuantity().add(newQuantity);
        medicine.setTotalQuantity(totalQuantity);
        medicine.setUsageFrequencyScore(medicine.getUsageFrequencyScore() + 1);
        medicine.setLastPurchaseDate(LocalDate.now());
        
        // 5. 更新药品状态
        if (totalQuantity.compareTo(medicine.getThreshold()) >= 0) {
            medicine.setStatus(MedicineStatus.NORMAL);
        } else {
            medicine.setStatus(MedicineStatus.LOW_STOCK);
        }
        
        medicineMapper.updateById(medicine);
        
        RestockResult result = new RestockResult();
        result.setSuccess(true);
        result.setMedicineName(medicine.getName());
        result.setNewQuantity(totalQuantity);
        result.setNewFrequencyScore(medicine.getUsageFrequencyScore());
        
        return result;
    }
}
```

## 前端设计 (Ant Design Pro)

> **⚠️ 实现说明（事实修正）**：本章为设计稿。**前端实际未按此实现**——未使用 Ant Design Pro，实际基于 `ant-design-vue`（非 Pro，见 `frontend/package.json`）；本章中的 ProTable / ProDescriptions / ProList / ProCard 及图表库（Charts）均未落地。**实际实现以 `frontend/` 为准**（页面见 `frontend/src/views/`）。

### 技术选型说明

使用 **Ant Design Vue Pro** 作为前端框架，提供：
- 开箱即用的高质量组件
- 专业的企业级UI设计
- 完整的页面模板和布局
- 响应式设计支持

### 页面结构

#### 1. 药品管理页面 (使用 ProTable)

```vue
<template>
  <pro-table
    :columns="columns"
    :request="loadMedicines"
    :search="{ labelWidth: 'auto' }"
    row-key="id"
  >
    <!-- 工具栏 -->
    <template #toolBarRender>
      <a-button type="primary" @click="handleAdd">
        <plus-outlined /> 添加药品
      </a-button>
    </template>
    
    <!-- 状态标签渲染 -->
    <template #status="{ record }">
      <a-tag :color="getStatusColor(record.status)">
        {{ getStatusText(record.status) }}
      </a-tag>
    </template>
    
    <!-- 操作列 -->
    <template #action="{ record }">
      <a @click="handleConsume(record)">记录用药</a>
      <a-divider type="vertical" />
      <a @click="showDetail(record)">详情</a>
      <a-divider type="vertical" />
      <a @click="handleEdit(record)">编辑</a>
    </template>
  </pro-table>
</template>

<script setup lang="ts">
import { ProTable } from '@ant-design-vue/pro-components'

// 列定义
const columns = [
  {
    title: '药品图片',
    dataIndex: 'imageUrl',
    valueType: 'image',
    hideInSearch: true,
    width: 80
  },
  {
    title: '药品名称',
    dataIndex: 'name',
    copyable: true,
    ellipsis: true
  },
  {
    title: '剩余数量',
    dataIndex: 'totalQuantity',
    valueType: 'digit',
    hideInSearch: true,
    render: (_, record) => `${record.totalQuantity} ${record.unit}`
  },
  {
    title: '适应症',
    dataIndex: 'symptomsTreated',
    ellipsis: true,
    width: 200
  },
  {
    title: '服用说明',
    dataIndex: 'dosageInstruction',
    ellipsis: true,
    hideInSearch: true
  },
  {
    title: '存放位置',
    dataIndex: 'storageLocation',
    filters: true,
    valueType: 'select'
  },
  {
    title: '状态',
    dataIndex: 'status',
    valueType: 'select',
    valueEnum: {
      normal: { text: '正常', status: 'Success' },
      low_stock: { text: '低库存', status: 'Warning' },
      expired: { text: '已过期', status: 'Error' },
      depleted: { text: '已耗尽', status: 'Default' }
    }
  },
  {
    title: '操作',
    valueType: 'option',
    width: 200
  }
]
</script>
```

#### 2. 药品详情抽屉 (使用 ProDescriptions)

```vue
<template>
  <a-drawer
    v-model:visible="visible"
    title="药品详情"
    width="720"
    :body-style="{ paddingBottom: '80px' }"
  >
    <pro-descriptions
      :column="2"
      :data-source="medicineDetail"
    >
      <!-- 基础信息 -->
      <pro-descriptions-item label="药品名称" :span="2">
        {{ medicineDetail.name }}
      </pro-descriptions-item>
      
      <pro-descriptions-item label="剂型">
        {{ medicineDetail.dosageForm }}
      </pro-descriptions-item>
      
      <pro-descriptions-item label="库存">
        <a-statistic
          :value="medicineDetail.totalQuantity"
          :suffix="medicineDetail.unit"
          :value-style="getQuantityStyle(medicineDetail)"
        />
      </pro-descriptions-item>
      
      <pro-descriptions-item label="预警阈值">
        {{ medicineDetail.threshold }} {{ medicineDetail.unit }}
      </pro-descriptions-item>
      
      <pro-descriptions-item label="存放位置">
        <a-tag color="blue">{{ medicineDetail.storageLocation }}</a-tag>
      </pro-descriptions-item>
      
      <!-- 时间信息 -->
      <pro-descriptions-item label="生产日期">
        {{ medicineDetail.productionDate }}
      </pro-descriptions-item>
      
      <pro-descriptions-item label="过期日期">
        <a-tag :color="isExpired(medicineDetail.expiryDate) ? 'red' : 'green'">
          {{ medicineDetail.expiryDate }}
        </a-tag>
      </pro-descriptions-item>
      
      <!-- 医疗说明 -->
      <pro-descriptions-item label="适应症" :span="2">
        {{ medicineDetail.symptomsTreated }}
      </pro-descriptions-item>
      
      <pro-descriptions-item label="用法用量" :span="2">
        {{ medicineDetail.dosageInstruction }}
      </pro-descriptions-item>
      
      <!-- 副作用（仅详情页显示） -->
      <pro-descriptions-item label="副作用" :span="2">
        <a-alert
          type="warning"
          :message="medicineDetail.sideEffects"
          show-icon
        />
      </pro-descriptions-item>
      
      <!-- 使用统计 -->
      <pro-descriptions-item label="使用频率">
        <a-progress
          :percent="getFrequencyPercent(medicineDetail.usageFrequencyScore)"
          :steps="10"
        />
      </pro-descriptions-item>
    </pro-descriptions>
    
    <!-- 最近用药记录 -->
    <a-divider>最近用药记录</a-divider>
    <a-timeline>
      <a-timeline-item
        v-for="log in medicineDetail.recentUsageLogs"
        :key="log.id"
      >
        <p>{{ log.usageTime }}</p>
        <p>{{ log.userName }} 服用 {{ log.amount }} {{ medicineDetail.unit }}</p>
        <p v-if="log.symptom">症状：{{ log.symptom }}</p>
      </a-timeline-item>
    </a-timeline>
  </a-drawer>
</template>
```

#### 3. 购物清单页面 (使用 ProList)

```vue
<template>
  <pro-list
    :data-source="shoppingList"
    :grid="{ gutter: 16, column: 2 }"
    row-key="id"
  >
    <template #renderItem="{ item }">
      <pro-card
        :title="item.medicineName"
        :hoverable="true"
        :actions="[
          { key: 'mark', text: '标记已买', onClick: () => markPurchased(item) },
          { key: 'delete', text: '删除', danger: true, onClick: () => deleteItem(item) }
        ]"
      >
        <template #cover>
          <img :src="item.medicine.imageUrl || '/default-medicine.png'" />
        </template>
        
        <a-descriptions :column="1" size="small">
          <a-descriptions-item label="添加原因">
            <a-tag :color="getReasonColor(item.reason)">
              {{ getReasonText(item.reason) }}
            </a-tag>
          </a-descriptions-item>
          
          <a-descriptions-item label="上次数量">
            {{ item.medicine.lastQuantity }} {{ item.medicine.unit }}
          </a-descriptions-item>
          
          <a-descriptions-item label="添加时间">
            {{ formatDate(item.addedAt) }}
          </a-descriptions-item>
          
          <a-descriptions-item v-if="item.customNote" label="备注">
            {{ item.customNote }}
          </a-descriptions-item>
        </a-descriptions>
      </pro-card>
    </template>
  </pro-list>
</template>
```

#### 4. 数据统计看板 (使用 ProCard + Charts)

```vue
<template>
  <div class="dashboard">
    <a-row :gutter="16">
      <!-- 统计卡片 -->
      <a-col :span="6">
        <pro-card>
          <a-statistic
            title="药品总数"
            :value="statistics.totalMedicines"
            suffix="种"
          >
            <template #prefix>
              <medicine-box-outlined />
            </template>
          </a-statistic>
        </pro-card>
      </a-col>
      
      <a-col :span="6">
        <pro-card>
          <a-statistic
            title="低库存预警"
            :value="statistics.lowStockCount"
            :value-style="{ color: '#faad14' }"
          >
            <template #prefix>
              <warning-outlined />
            </template>
          </a-statistic>
        </pro-card>
      </a-col>
      
      <a-col :span="6">
        <pro-card>
          <a-statistic
            title="已过期"
            :value="statistics.expiredCount"
            :value-style="{ color: '#cf1322' }"
          >
            <template #prefix>
              <close-circle-outlined />
            </template>
          </a-statistic>
        </pro-card>
      </a-col>
      
      <a-col :span="6">
        <pro-card>
          <a-statistic
            title="待购买"
            :value="statistics.pendingPurchase"
          >
            <template #prefix>
              <shopping-cart-outlined />
            </template>
          </a-statistic>
        </pro-card>
      </a-col>
    </a-row>
    
    <!-- 图表区域 -->
    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="12">
        <pro-card title="常用药品排行">
          <bar-chart :data="topMedicines" />
        </pro-card>
      </a-col>
      
      <a-col :span="12">
        <pro-card title="用药趋势">
          <line-chart :data="usageTrend" />
        </pro-card>
      </a-col>
    </a-row>
  </div>
</template>
```

### TypeScript 接口定义

```typescript
// API 响应类型
interface MedicineVO {
  id: string
  name: string
  imageUrl?: string
  dosageForm: string
  unit: string
  totalQuantity: number
  threshold: number
  symptomsTreated: string
  dosageInstruction: string
  status: 'NORMAL' | 'LOW_STOCK' | 'EXPIRED' | 'DEPLETED'
  storageLocation?: string
  expiryDate: string
  usageFrequencyScore: number
  // 注意：列表页不返回 sideEffects
}

interface MedicineDetailVO extends MedicineVO {
  sideEffects: string  // 仅详情页返回
  productionDate?: string
  lastPurchaseDate?: string
  categories: CategoryVO[]
  recentUsageLogs: UsageLogVO[]
}

interface ShoppingListItemVO {
  id: string
  medicineId: string
  medicineName: string
  reason: 'EXPIRED' | 'DEPLETED' | 'MANUAL'
  customNote?: string
  status: 'PENDING' | 'PURCHASED'
  addedAt: string
  medicine: {
    imageUrl?: string
    unit: string
    lastQuantity: number
  }
}
```

## 错误处理

### 异常类型

```java
// 基础异常类
public class MedicineException extends RuntimeException {
    private final int code;
    
    public MedicineException(int code, String message) {
        super(message);
        this.code = code;
    }
}

// 具体异常类型
public class MedicineNotFoundException extends MedicineException {
    public MedicineNotFoundException(String message) {
        super(404, message);
    }
}

public class InsufficientStockException extends MedicineException {
    public InsufficientStockException(String message) {
        super(422, message);
    }
}

public class InvalidAmountException extends MedicineException {
    public InvalidAmountException(String message) {
        super(400, message);
    }
}

public class DuplicateCategoryException extends MedicineException {
    public DuplicateCategoryException(String message) {
        super(409, message);
    }
}
```

### 全局异常处理器

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MedicineNotFoundException.class)
    public Result<?> handleNotFound(MedicineNotFoundException e) {
        log.warn("资源不存在: {}", e.getMessage());
        return Result.error(404, e.getMessage());
    }
    
    @ExceptionHandler(InsufficientStockException.class)
    public Result<?> handleInsufficientStock(InsufficientStockException e) {
        log.warn("库存不足: {}", e.getMessage());
        return Result.error(422, e.getMessage());
    }
    
    @ExceptionHandler(InvalidAmountException.class)
    public Result<?> handleInvalidAmount(InvalidAmountException e) {
        log.warn("数据验证失败: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }
    
    @ExceptionHandler(DuplicateCategoryException.class)
    public Result<?> handleDuplicate(DuplicateCategoryException e) {
        log.warn("数据冲突: {}", e.getMessage());
        return Result.error(409, e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public Result<?> handleGeneral(Exception e) {
        log.error("系统错误", e);
        return Result.error(500, "系统内部错误");
    }
}
```

### 错误处理策略

1. **数据验证错误**：返回 400 Bad Request，包含详细错误信息
2. **资源不存在**：返回 404 Not Found
3. **业务逻辑错误**：返回 422 Unprocessable Entity
4. **数据冲突**：返回 409 Conflict
5. **数据库错误**：记录日志，返回 500 Internal Server Error
6. **所有操作使用 @Transactional**：确保数据一致性

## 测试策略

### 单元测试 (JUnit 5 + Mockito)

针对核心业务逻辑编写单元测试：

```java
@SpringBootTest
class InventoryServiceTest {
    
    @Autowired
    private InventoryService inventoryService;
    
    @MockBean
    private MedicineMapper medicineMapper;
    
    @Test
    @DisplayName("测试库存扣减逻辑")
    void testConsumeMedicine() {
        // Given
        Medicine medicine = createTestMedicine();
        when(medicineMapper.selectById(anyString())).thenReturn(medicine);
        
        // When
        ConsumeResult result = inventoryService.consumeMedicine(
            medicine.getId(), 
            BigDecimal.valueOf(5), 
            "张三", 
            "头痛"
        );
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(BigDecimal.valueOf(15), result.getNewQuantity());
    }
}
```

### 基于属性的测试 (jqwik)

使用 **jqwik** (Java 的属性测试框架) 进行属性测试：

**配置要求**：
- 每个属性测试至少运行 100 次迭代
- 每个测试必须标注对应的设计文档属性编号
- 标注格式：`// Feature: family-medicine-cabinet, Property X: [属性描述]`

**Maven 依赖**：
```xml
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.7.4</version>
    <scope>test</scope>
</dependency>
```

**测试覆盖**：
- 属性 1-20 的所有正确性属性
- 重点测试：排序不变量、库存扣减不变量、序列化 round-trip

**示例**：

```java
@PropertyDefaults(tries = 100)
class MedicinePropertyTest {
    
    // Feature: family-medicine-cabinet, Property 4: 库存扣减不变量
    @Property
    @Label("对于任意有效的消耗量，库存应该正确扣减")
    void inventoryDeductionInvariant(
        @ForAll @BigRange(min = "1", max = "1000") BigDecimal initialQuantity,
        @ForAll @BigRange(min = "0.1", max = "100") BigDecimal consumeAmount
    ) {
        // 跳过无效情况
        Assume.that(consumeAmount.compareTo(initialQuantity) <= 0);
        
        // Given
        Medicine medicine = createMedicine(initialQuantity);
        
        // When
        inventoryService.consumeMedicine(medicine.getId(), consumeAmount, "测试", "测试");
        
        // Then
        Medicine updated = medicineMapper.selectById(medicine.getId());
        BigDecimal expected = initialQuantity.subtract(consumeAmount);
        assertEquals(0, expected.compareTo(updated.getTotalQuantity()));
    }
    
    // Feature: family-medicine-cabinet, Property 7: 智能排序第一优先级
    @Property
    @Label("排序后的药品应该按使用频率分数降序排列")
    void sortingByFrequencyInvariant(@ForAll("medicineList") List<Medicine> medicines) {
        // When
        List<Medicine> sorted = sortingService.applySortingRules(medicines);
        
        // Then
        for (int i = 0; i < sorted.size() - 1; i++) {
            int currentScore = sorted.get(i).getUsageFrequencyScore();
            int nextScore = sorted.get(i + 1).getUsageFrequencyScore();
            assertTrue(currentScore >= nextScore, 
                "频率分数应该降序排列");
        }
    }
    
    // Feature: family-medicine-cabinet, Property 9: 排序完整性不变量
    @Property
    @Label("排序前后药品数量应该相等")
    void sortingCompletenessInvariant(@ForAll("medicineList") List<Medicine> medicines) {
        // When
        List<Medicine> sorted = sortingService.applySortingRules(medicines);
        
        // Then
        assertEquals(medicines.size(), sorted.size());
        assertTrue(sorted.containsAll(medicines));
    }
    
    // Feature: family-medicine-cabinet, Property 19: 序列化round-trip
    @Property
    @Label("序列化后反序列化应该得到等价对象")
    void serializationRoundTrip(@ForAll("medicine") Medicine medicine) {
        // When
        String json = objectMapper.writeValueAsString(medicine);
        Medicine deserialized = objectMapper.readValue(json, Medicine.class);
        
        // Then
        assertEquals(medicine.getId(), deserialized.getId());
        assertEquals(medicine.getName(), deserialized.getName());
        assertEquals(0, medicine.getTotalQuantity().compareTo(deserialized.getTotalQuantity()));
        // ... 验证所有字段
    }
    
    // 数据生成器
    @Provide
    Arbitrary<Medicine> medicine() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100),
            Arbitraries.of(DosageForm.class),
            Arbitraries.of("粒", "ml", "包", "片"),
            Arbitraries.bigDecimals().between(BigDecimal.ZERO, BigDecimal.valueOf(1000))
        ).as((name, form, unit, quantity) -> {
            Medicine m = new Medicine();
            m.setId(UUID.randomUUID().toString());
            m.setName(name);
            m.setDosageForm(form);
            m.setUnit(unit);
            m.setTotalQuantity(quantity);
            m.setThreshold(BigDecimal.TEN);
            m.setExpiryDate(LocalDate.now().plusYears(1));
            return m;
        });
    }
    
    @Provide
    Arbitrary<List<Medicine>> medicineList() {
        return medicine().list().ofMinSize(1).ofMaxSize(20);
    }
}
```

### 集成测试 (Spring Boot Test)

测试完整的业务流程：

```java
@SpringBootTest
@Transactional
class MedicineIntegrationTest {
    
    @Test
    @DisplayName("完整流程：添加药品 -> 消耗 -> 耗尽 -> 加入购物清单")
    void testCompleteFlow() {
        // 1. 添加药品
        Medicine medicine = createAndSaveMedicine();
        
        // 2. 消耗至耗尽
        inventoryService.consumeMedicine(
            medicine.getId(),
            medicine.getTotalQuantity(),
            "测试用户",
            "测试症状"
        );
        
        // 3. 验证加入购物清单
        List<ShoppingListItem> items = shoppingListMapper.findPending();
        assertTrue(items.stream()
            .anyMatch(item -> item.getMedicineId().equals(medicine.getId())));
        
        // 4. 验证频率分数增加
        Medicine updated = medicineMapper.selectById(medicine.getId());
        assertEquals(medicine.getUsageFrequencyScore() + 1, 
            updated.getUsageFrequencyScore());
    }
}
```

