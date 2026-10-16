# 测试 insert 引用不明确问题修复

## 问题描述

运行 `mvn test` 时在 `InventoryPropertyTest.java` 中出现编译错误：

```
对 insert 的引用不明确
```

---

## 根本原因

MyBatis-Plus 3.5.7 的 `BaseMapper` 接口有多个 `insert` 方法重载：

```java
public interface BaseMapper<T> {
    int insert(T entity);
    // 可能还有其他重载方法
}
```

当使用 Mockito 的 `any()` 时，编译器无法确定应该使用哪个 `insert` 方法：

```java
when(usageLogMapper.insert(any())).thenReturn(1);  // ❌ 不明确
```

---

## 解决方案

明确指定 `any()` 的类型参数，让编译器知道要 mock 哪个方法：

### 修改前（错误）：

```java
// Mock mapper 行为
when(medicineMapper.selectById(medicine.getId())).thenReturn(medicine);
when(usageLogMapper.insert(any())).thenReturn(1);  // ❌ 引用不明确
when(medicineMapper.updateById(any())).thenReturn(1);  // ❌ 引用不明确
```

### 修改后（正确）：

```java
// Mock mapper 行为
when(medicineMapper.selectById(medicine.getId())).thenReturn(medicine);
when(usageLogMapper.insert(any(com.family.medicinecabinet.entity.UsageLog.class))).thenReturn(1);  // ✅ 明确类型
when(shoppingListMapper.insert(any(com.family.medicinecabinet.entity.ShoppingListItem.class))).thenReturn(1);  // ✅ 明确类型
when(medicineMapper.updateById(any(Medicine.class))).thenReturn(1);  // ✅ 明确类型
```

---

## 技术说明

### Mockito `any()` 方法

Mockito 提供了两种方式来匹配任意参数：

1. **无类型参数**（可能导致歧义）：
   ```java
   when(mapper.insert(any())).thenReturn(1);  // ❌ 如果有重载方法会报错
   ```

2. **带类型参数**（推荐）：
   ```java
   when(mapper.insert(any(Entity.class))).thenReturn(1);  // ✅ 明确指定类型
   ```

### 为什么需要明确类型？

当接口有方法重载时：
```java
interface Mapper {
    int insert(UsageLog log);
    int insert(UsageLog log, boolean selective);
}
```

编译器无法从 `any()` 判断要 mock 哪个方法，必须明确指定参数类型。

---

## 相关修改

### 文件：`InventoryPropertyTest.java`

**修改位置**：第 48-50 行

**修改内容**：
- 为 `usageLogMapper.insert()` 添加类型参数
- 为 `shoppingListMapper.insert()` 添加类型参数（预防性修复）
- 为 `medicineMapper.updateById()` 添加类型参数

---

## 验证修复

### 方法 1：在 IDEA 中运行测试

```
右键 InventoryPropertyTest.java → Run 'InventoryPropertyTest'
```

应该看到：
```
Tests run: 1, Failures: 0, Errors: 0
✅ inventoryDeductionInvariant - PASSED
```

### 方法 2：使用 Maven 命令

```cmd
mvn test -Dtest=InventoryPropertyTest
```

应该看到：
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 最佳实践

### ✅ 推荐做法

在使用 Mockito mock 方法时，始终明确指定类型参数：

```java
// ✅ 好的做法
when(mapper.insert(any(Entity.class))).thenReturn(1);
when(mapper.update(any(Entity.class))).thenReturn(1);
when(mapper.delete(any(String.class))).thenReturn(1);
```

### ❌ 避免做法

不要使用无类型的 `any()`，特别是在有方法重载的情况下：

```java
// ❌ 可能导致编译错误
when(mapper.insert(any())).thenReturn(1);
when(mapper.update(any())).thenReturn(1);
```

---

## 其他可能的解决方案

### 方案 1：使用 `anyObject()`（已废弃）

```java
when(mapper.insert(anyObject())).thenReturn(1);  // ⚠️ 已废弃，不推荐
```

### 方案 2：使用具体的匹配器

```java
when(mapper.insert(argThat(log -> log != null))).thenReturn(1);  // ✅ 可行但复杂
```

### 方案 3：使用 `any(Class)` （推荐）

```java
when(mapper.insert(any(UsageLog.class))).thenReturn(1);  // ✅ 最佳方案
```

---

## 相关问题

### 问题：为什么之前没有这个错误？

**答案**：升级 MyBatis-Plus 到 3.5.7 后，`BaseMapper` 接口可能增加了新的方法重载，导致之前的代码出现歧义。

### 问题：其他测试文件需要修改吗？

**答案**：不需要。`MedicinePropertyTest` 和 `SortingPropertyTest` 没有使用 mock 对象，所以不受影响。

---

## 总结

✅ **问题**：`insert` 方法引用不明确
✅ **原因**：MyBatis-Plus 3.5.7 的方法重载 + Mockito `any()` 无类型参数
✅ **解决**：为 `any()` 添加明确的类型参数
✅ **状态**：已修复，测试应该可以正常运行

---

## 相关文档

- 📄 **测试修复说明.md** - 之前的测试问题修复
- 📄 **完整修复总结.md** - 所有问题修复总结
- 📄 **启动问题修复.md** - MyBatis-Plus 版本升级说明

---

**修复时间**：2024年
**修复文件**：`InventoryPropertyTest.java`
**修复状态**：✅ 完成
