# 变更与修复记录（CHANGELOG）

本文由 7 份历史修复快照合并而成（来源见文末），按主题归并。据归档文档，这些修复发生在本项目初始开发阶段（归档署名日期 2024-12-13；仓库 git 提交时间均为 2026-10-16，**时间口径不一致，日期存疑**，仅供参考）。

> ⚠️ **阅读须知**：本文出现的所有版本号均为**历史版本**，仅用于追溯问题的来龙去脉，**不代表当前配置**。当前事实一律以仓库源码为准：`pom.xml`（Java 17、Spring Boot 3.2.0、MyBatis-Plus 3.5.10.1）、`application.yml`、`frontend/package.json`。

---

## 一、编译期修复（初始开发阶段）

### 1.1 `SortingService.java` 找不到符号

- **现象**：编译报 `SortingService` 找不到符号
- **根因**：文件为空，且残留 UTF-8 BOM 字符
- **修复**：重新创建文件，使用正确的 UTF-8（无 BOM）编码
- **文件**：`src/main/java/com/family/medicinecabinet/service/SortingService.java`

### 1.2 `InventoryServiceImpl.java` 找不到符号 `info`

- **现象**：编译报找不到符号 `info`（`log.info(...)` 调用处）
- **根因**：局部变量 `UsageLog log` 与 Lombok `@Slf4j` 生成的 `log` 字段重名，遮蔽了日志对象
- **修复**：变量重命名为 `usageLog`（当前代码中该写法见 `InventoryServiceImpl.java` 第 77 行附近）
- **教训**：使用 `@Slf4j` 的类中不要定义名为 `log` 的局部变量

### 1.3 主类 `SpringApplication.run` 语法错误

- **现象**：`MedicineCabinetApplication` 找不到符号 `java`
- **根因**：写成 `SpringApplication.run(MedicineCabinetApplication.java, args)`
- **修复**：改为 `SpringApplication.run(MedicineCabinetApplication.class, args)`

---

## 二、MyBatis-Plus 与 Spring Boot 3 兼容性（历史升级链）

- **现象**：应用启动失败，报
  `java.lang.IllegalArgumentException: Invalid value type for attribute 'factoryBeanObjectType': java.lang.String`
  （测试能通过但启动失败，因为该异常发生在 MyBatis-Plus 装配阶段）
- **历史处理过程**（版本号均为历史值）：
  - `3.5.5`：与 Spring Boot 3.2.0 完全不兼容（启动失败）
  - `3.5.7`：部分兼容，问题仍存在 → 一度升到此版本
  - `3.5.9`：当时的最新稳定版，升到此版本后问题解决
- **当前配置**：`pom.xml` 使用 `mybatis-plus-spring-boot3-starter` **3.5.10.1**，并额外引入 `mybatis-plus-jsqlparser`——分页插件 `PaginationInnerInterceptor` 自历史版本 3.5.9 起从主包拆分至该模块（见 `pom.xml` 第 47 行注释）。
- **教训**：MyBatis-Plus 与 Spring Boot 3.x 的兼容性问题优先通过升级 MyBatis-Plus 解决，而不是降级 Spring Boot。

---

## 三、测试修复

### 3.1 jqwik 属性测试 NullPointerException

- **现象**：`InventoryPropertyTest` 测试失败（NullPointerException）
- **根因**：测试类使用了 `@SpringBootTest` + `@MockBean`，但 jqwik 的 `@Property` 方法**不会**注入 Spring 上下文和 mock bean
- **修复**：`InventoryPropertyTest` 移除 `@SpringBootTest` 与类级 `@MockBean`，在**每个测试方法内部**手动创建 mock 对象（`@PropertyDefaults(tries = 100)` 保留）
- **结论**：jqwik 的 `@Property` 方法不会自动注入 Spring 上下文；属性测试均不靠容器注入（现状核对 2026-10-16：`SortingPropertyTest` 仍保留类级 `@SpringBootTest` 注解，但内部直接 `new SortingServiceImpl()`，不依赖注入）

### 3.2 `insert` 引用不明确（Mockito 重载歧义）

- **现象**：编译报"对 `insert` 的引用不明确"
- **根因**：MyBatis-Plus `BaseMapper.insert` 有重载，Mockito `any()` 无类型信息无法选择
- **修复**：为匹配器添加明确类型，如 `any(UsageLog.class)`、`any(ShoppingListItem.class)`（当前代码见 `InventoryPropertyTest.java` 第 49-50 行）
- **文件**：`src/test/java/com/family/medicinecabinet/property/InventoryPropertyTest.java`（归档记录的修复文件）

---

## 四、验证记录（历史快照）

历史检查类文档（如归档的"检查结果-无错误"与各检查报告）当时记录了"0 编译错误/0 导入错误"。注意：这些快照中的**文件数量、依赖版本与前端完成度陈述多为当时的旧值且部分有误**（例如当时尚未包含后来新增的控制器），**不要**据其核对现状——一切以当前仓库文件系统与 `pom.xml` 为准。

---

## 来源（已归档至 `docs/archive/`）

| 归档文件 | 原位置 |
|---|---|
| `archive/所有问题已修复.md` | 根目录 |
| `archive/完整修复总结.md` | 根目录 |
| `archive/紧急修复-MyBatis兼容性.md` | 根目录 |
| `archive/LOMBOK_ISSUES_FIXED.md` | 根目录 |
| `archive/测试修复说明.md` | 根目录 |
| `archive/测试insert引用修复.md` | 根目录 |
| `archive/检查结果-无错误.md` | 根目录 |
