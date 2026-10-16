# Lombok 编译问题修复总结

## 问题概述

在 Windows + Java 17 + Maven 环境下，项目遇到了多个 Lombok 相关的编译错误。

---

## 已修复的问题

### 1. ✅ SortingService 找不到符号

**问题**：`CategoryServiceImpl.java` 报错 "找不到符号 SortingService"

**根本原因**：
- `SortingService.java` 文件为空（0 字节）
- 文件包含 UTF-8 BOM 字符 (`\ufeff`)

**解决方案**：
- 使用 PowerShell 重新创建文件，使用 UTF-8 无 BOM 编码
- 文件已正确创建，包含完整的接口定义

**文件位置**：`src/main/java/com/family/medicinecabinet/service/SortingService.java`

---

### 2. ✅ InventoryServiceImpl 找不到符号 info

**问题**：`InventoryServiceImpl.java` 报错 "找不到符号 info"

**根本原因**：
- 类使用了 `@Slf4j` 注解，Lombok 会生成一个名为 `log` 的静态 logger 对象
- 代码中创建了局部变量 `UsageLog log = new UsageLog();`
- 局部变量 `log` 覆盖了 Lombok 生成的 `log` 对象
- 导致 `log.info()` 调用失败，因为 `UsageLog` 类没有 `info()` 方法

**解决方案**：
- 将局部变量从 `log` 重命名为 `usageLog`
- 修改所有引用：`log.setXxx()` → `usageLog.setXxx()`

**修改位置**：`src/main/java/com/family/medicinecabinet/service/impl/InventoryServiceImpl.java` 第 52 行

```java
// 修改前（错误）
UsageLog log = new UsageLog();
log.setMedicineId(medicineId);

// 修改后（正确）
UsageLog usageLog = new UsageLog();
usageLog.setMedicineId(medicineId);
```

---

## Lombok 最佳实践

### ⚠️ 避免变量名冲突

当使用 Lombok 注解时，避免使用以下变量名：

| Lombok 注解 | 生成的字段/方法 | 避免使用的变量名 |
|------------|---------------|----------------|
| `@Slf4j` | `private static final Logger log` | `log` |
| `@Log` | `private static final Logger log` | `log` |
| `@Log4j2` | `private static final Logger log` | `log` |
| `@Data` | `equals()`, `hashCode()`, `toString()` | - |
| `@Builder` | `builder()` | `builder` |

### ✅ 推荐的命名方式

```java
@Slf4j
@Service
public class MyService {
    
    // ✅ 正确：使用具体的类型名
    UsageLog usageLog = new UsageLog();
    
    // ✅ 正确：使用描述性名称
    UsageLog newLog = new UsageLog();
    
    // ❌ 错误：与 @Slf4j 生成的 log 冲突
    UsageLog log = new UsageLog();
    
    // ✅ 正确：使用 Lombok 生成的 logger
    log.info("This works!");
}
```

---

## IDEA 编译器配置（重要！）

### 为什么需要配置 IDEA？

Maven 命令行编译器在 Windows + Java 17 环境下对 Lombok 的支持不稳定，导致：
- 注解处理器无法正确运行
- Lombok 生成的代码不可见
- 编译错误：找不到 getter/setter/log 等

### 解决方案：使用 IDEA 内置编译器

1. **File → Settings** (Ctrl + Alt + S)
2. **Build, Execution, Deployment → Build Tools → Maven → Runner**
3. **取消勾选** "Delegate IDE build/run actions to Maven"
4. **Build, Execution, Deployment → Compiler → Annotation Processors**
5. **勾选** "Enable annotation processing"
6. **Build → Rebuild Project**

详细步骤请参考：`IDEA_COMPILER_FIX.md`

---

## 验证修复

### 检查 SortingService

```bash
# 检查文件不是空的
dir src\main\java\com\family\medicinecabinet\service\SortingService.java

# 应该显示文件大小 > 0 字节
```

### 检查 InventoryServiceImpl

在 IDEA 中打开 `InventoryServiceImpl.java`：
- ✅ 第 52 行应该是 `UsageLog usageLog = new UsageLog();`
- ✅ 第 42 行的 `log.info()` 不应该有红色下划线
- ✅ 第 110 行的 `log.info()` 不应该有红色下划线

### 重新构建项目

1. 在 IDEA 中执行：**Build → Rebuild Project**
2. 检查 Build 窗口，应该显示：
   ```
   Build completed successfully in X s
   ```
3. 不应该有任何 "找不到符号" 错误

---

## 如果问题仍然存在

### 步骤 1：清理缓存

```bash
# 在项目根目录执行
rmdir /s /q target
rmdir /s /q .idea
del /q *.iml
```

### 步骤 2：重新导入项目

1. 关闭 IDEA
2. 重新打开 IDEA
3. **File → Open** → 选择 `pom.xml`
4. **Open as Project**
5. 等待 Maven 导入完成

### 步骤 3：重新配置

按照 `IDEA_COMPILER_FIX.md` 中的步骤重新配置编译器设置

---

## 相关文档

- `IDEA_COMPILER_FIX.md` - IDEA 编译器配置详细指南
- `IDEA_SETUP.md` - IDEA 项目设置指南
- `HOW_TO_RUN.md` - 项目运行指南
- `pom.xml` - Maven 配置（包含 Lombok 依赖）

---

## 总结

✅ **SortingService.java** - 文件已重新创建，无 BOM 字符
✅ **InventoryServiceImpl.java** - 变量名冲突已解决
✅ **IDEA 配置** - 使用内置编译器，避免 Maven + Lombok 兼容性问题

**下一步**：在 IDEA 中执行 **Build → Rebuild Project** 验证所有问题已解决。
