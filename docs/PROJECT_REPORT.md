# 项目报告（PROJECT_REPORT）

本文由 5 份历史报告合并而成（来源见文末），**已剔除其中被证伪的结论**，所有可核事实均以当前仓库源码为准（实测日期 2026-10-16）。

---

## 1. 项目概述

家庭智慧药箱管理系统（`pom.xml`：`medicine-cabinet` v1.0.0）：面向家庭场景的药品库存管理应用，采用 Spec 驱动开发（需求 → 设计 → 任务，见 `.kiro/specs/family-medicine-cabinet/`）。

核心能力：最小单位库存存储、基于用药频次的智能排序、低库存/过期自动监控、从"耗尽"到"购物清单"再到"补货回填"的自动闭环。

## 2. 技术架构

```
Vue 3 (ant-design-vue + Vite, :3000)
        │  /api 代理
        ▼
Controller (4 个) → Service (5 接口/5 实现 + DTO) → Mapper (MyBatis-Plus 3.5.10.1)
        ▼
MySQL 8 (medicine_cabinet, 5 张表)
```

### 后端代码规模（`src/main/java`，实测 38 个 Java 文件）

| 包 | 文件数 | 内容 |
|---|---|---|
| （根） | 1 | `MedicineCabinetApplication`（含 `@EnableScheduling`） |
| `common` | 1 | `Result` 统一响应 |
| `config` | 2 | `CorsConfig`、`MyBatisPlusConfig` |
| `controller` | 4 | `MedicineController`、`CategoryController`、`ShoppingListController`、`HomeController` |
| `entity` + `entity/enums` | 4 + 4 | 实体 4 个；枚举 4 个（DosageForm / MedicineStatus / ShoppingReason / ShoppingStatus） |
| `exception` | 6 | 5 个自定义异常 + `GlobalExceptionHandler` |
| `mapper` | 4 | MyBatis-Plus Mapper 接口（对应 `src/main/resources/mapper/` 下 4 个 XML） |
| `service` | 5 + 2 + 5 | 5 个接口、2 个 DTO（`ConsumeResult`、`ExpiryCheckResult`）、5 个实现 |

资源：`application.yml`（端口 8080、context-path `/api`）、`sql/schema.sql`、4 个 Mapper XML。

### 测试代码（`src/test/java`，7 个文件）

- 3 个属性测试类（jqwik）：`MedicinePropertyTest`、`InventoryPropertyTest`、`SortingPropertyTest`
- 2 个服务单元测试：`InventoryServiceImplTest`、`ShoppingListServiceImplTest`
- 1 个控制器校验测试：`MedicineValidationTest`
- 1 个 mock 工具：`support/MockServices`

### 前端（`frontend/src`）

- 视图 6 个：`Dashboard.vue`、`MedicineList.vue`（含记录用药弹窗）、`ShoppingList.vue`、`CategoryManagement.vue`、`NotFound.vue`（+ `App.vue`）
- API 封装 3 个（`api/medicine.ts`、`api/shopping.ts`、`api/category.ts`）、`utils/request.ts`（Axios 封装）、`utils/dict.ts`（枚举中文映射）、`types/index.ts`（大写枚举类型）
- 无 Pinia、无图表库（`package.json` 实测）

## 3. 功能实现（与代码对应）

| 功能 | 实现要点 | 出处 |
|---|---|---|
| 药品 CRUD | 创建（null 字段兜底默认值）、更新、物理删除、按 id 查询 | `MedicineController` |
| 分页查询 | `current`/`size`/`name`（名称模糊） | `MedicineController.list` |
| 记录用药 | 扣减库存 + 写 `usage_logs` + 低库存/耗尽分支（耗尽时频率分 +1）；耗尽自动进购物清单 | `InventoryServiceImpl` |
| 补货 | `restockMedicine`（`@Transactional`） | `InventoryServiceImpl` |
| 智能排序 | 三级：频率分降序 → 最近购买日期降序 → 创建时间降序 | `SortingServiceImpl`（`comparing/thenComparing`） |
| 过期监控 | `@Scheduled(cron = "0 0 2 * * ?")` 每日 02:00；`@EnableScheduling` 开启 | `ExpiryMonitorServiceImpl:38`、`MedicineCabinetApplication:16` |
| 类别管理（后端） | 创建（名称唯一）、多对多分配、按类别查询（带智能排序） | `CategoryController`/`CategoryService` |
| 购物清单 | 待购买列表（LEFT JOIN 回填药品名）、手动添加、标记已购（回填库存）、删除 | `ShoppingListController`、`ShoppingListMapper` |
| 异常处理 | 统一 `Result` 返回；`@RestControllerAdvice` | `exception/GlobalExceptionHandler` |
| 前端页面 | 看板（统计卡片；图表待实现）、药品列表（增删改/用药/补货）、购物清单、类别管理 | `frontend/src/views/**` |

## 4. 数据库设计（`src/main/resources/sql/schema.sql`）

5 张表（InnoDB/utf8mb4）：

1. `medicines` — 药品主表（含 `dosage_form` / `status` ENUM、`usage_frequency_score`）
2. `categories` — 类别表（名称唯一）
3. `medicine_categories` — 药品-类别多对多关联（复合主键 + 级联删除）
4. `usage_logs` — 用药记录（人、量、症状、时间）
5. `shopping_list` — 购物清单（`reason` / `status` ENUM）

关键约束：枚举值均为**大写**（`TABLET…OTHER`、`NORMAL/LOW_STOCK/EXPIRED/DEPLETED`、`EXPIRED/DEPLETED/MANUAL`、`PENDING/PURCHASED`，见 schema.sql 第 16/38/89/91 行）；索引覆盖过期日、状态、频率分；插入 5 条初始类别（感冒药/止痛药/肠胃药/外用药/维生素，第 101-106 行）。

> ⚠️ **`application.yml` 第 26-28 行的 `logic-delete-*` 是未生效的配置**：全部表无 `deleted` 列、全部实体无 `deleted` 字段（`src/` 中检索 `deleted`/`@TableLogic` 仅命中该配置本身），删除实际走物理 `DELETE`（2026-10-16 实测）。勿据此配置推断删除行为。

## 5. 测试覆盖（实测）

- 属性测试（jqwik 1.7.4，`tries = 100`）已实现 7 个：
  - Property 1 药品创建完整性、Property 2 最小单位存储一致性、Property 3 库存数量验证（`MedicinePropertyTest`）
  - Property 4 库存扣减不变量（`InventoryPropertyTest`）
  - Property 7 智能排序第一优先级、Property 8 第二优先级、Property 9 排序完整性不变量（`SortingPropertyTest`）
- 单元测试：库存服务、购物清单服务（Mockito）
- 校验测试：Controller 入参校验

> ⚠️ 任务清单中标为完成的其余属性测试（Property 5/6/10–20 等）**在代码中不存在**，详见 `tasks.md` 末尾「实现偏差记录」。

## 6. 历史报告的勘误（**已被证伪，不得沿用**）

| 旧报告的说法 | 事实 | 依据 |
|---|---|---|
| "缺少 `CategoryMapper.xml`" | **错误**——该文件存在，`mapper/` 下 4 个 XML 齐备 | `src/main/resources/mapper/` 实测 |
| "系统当前是 Java 8" / "前端未实现（0%）" | **错误**——本机默认为 Java 11（需切 JDK 17）；前端 6 个视图均已实现 | 实测环境；`frontend/src/views/` |
| 旧版本号（依赖/前端端口/框架名） | **过时**——MyBatis-Plus 3.5.10.1、前端 3000 端口、ant-design-vue（非 Pro） | `pom.xml`、`vite.config.ts`、`package.json` |
| "37 个 Java 文件 / 3 个 Controller" 等计数 | **过时**——当前 main 38 个 Java 文件、4 个 Controller | 文件系统实测 |

## 7. 已知偏差与未实现项

以 `.kiro/specs/family-medicine-cabinet/tasks.md` 末尾的「实现偏差记录」为准（列出声称完成但代码中不存在的产物，如 `StatisticsController`/`StatisticsService`、`UsageLogController`、`MedicineDetail.vue`、`ConsumeMedicine.vue`、数据导出、存放位置自动补全、Dockerfile 等）。

## 8. 后续扩展建议（保留自旧报告，属规划而非现状）

1. 数据统计：实现统计服务 + 看板图表（当前看板仅统计卡片）
2. 补齐 Spec 中规划的属性测试与统计/导出接口
3. 用户系统与多用户支持；用药/过期提醒
4. 导入导出（Excel/CSV）、药品图片管理
5. 部署准备：Dockerfile、前端构建脚本

---

## 来源（已归档至 `docs/archive/`）

| 归档文件 | 原位置 |
|---|---|
| `archive/FINAL_PROJECT_REPORT.md` | 根目录 |
| `archive/PROJECT_SUMMARY.md` | 根目录 |
| `archive/PROJECT_CHECK_REPORT.md` | 根目录 |
| `archive/项目交付说明.md` | 根目录 |
| `archive/项目代码检查报告.md` | 根目录 |
