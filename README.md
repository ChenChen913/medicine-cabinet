# 家庭智慧药箱管理系统

一个家庭药品管理全栈应用：药品库存与最小单位存储、基于用药频次的智能排序、库存/过期自动监控、自动补货购物清单闭环。

- 后端：Spring Boot 3 + MyBatis-Plus + MySQL，REST API 挂载于 `http://localhost:8080/api`
- 前端：Vue 3 + TypeScript + ant-design-vue + Vite，开发服务器 `http://localhost:3000`（`/api` 请求自动代理到后端）

> 本 README 是项目唯一入口。启动/排障请看 [docs/RUNBOOK.md](docs/RUNBOOK.md)，历次修复记录见 [docs/CHANGELOG.md](docs/CHANGELOG.md)，项目报告见 [docs/PROJECT_REPORT.md](docs/PROJECT_REPORT.md)，历史文档已归档至 `docs/archive/`。

## 技术栈（以源码为准）

| 层 | 技术 | 出处 |
|---|---|---|
| 后端 | Spring Boot 3.2.0 / Java 17 | `pom.xml`（parent 3.2.0、`<java.version>17</java.version>`） |
| ORM | MyBatis-Plus 3.5.10.1（含 `mybatis-plus-jsqlparser` 分页模块） | `pom.xml`（`<mybatis-plus.version>`） |
| 数据库 | MySQL 8，库名 `medicine_cabinet`，5 张表 | `src/main/resources/application.yml`、`src/main/resources/sql/schema.sql` |
| 测试 | jqwik 1.7.4 属性测试 + JUnit 单元测试 | `pom.xml`、`src/test/java/**` |
| 前端 | Vue 3.4 / TypeScript 5.3 / ant-design-vue 4.1（非 Pro）/ Vite 5 / Vue Router 4 / Axios 1.6 | `frontend/package.json` |

## 环境要求

- **JDK 17**（必需；`pom.xml` 的 `<java.version>` 与 maven-compiler-plugin 均为 17）
  - ⚠️ 本机默认环境为 **Java 11**（实测 `java -version` = `11.0.8`，`JAVA_HOME=D:\java\jdk11`，2026-10-16），**低于项目要求，直接编译会失败**。请先把 `JAVA_HOME` 切到 JDK 17（本机已安装在 `D:\java\jdk17`），或直接使用根目录脚本 `run-with-java17.bat` / `compile-with-java17.bat`（脚本内已设置 `JAVA_HOME=D:\java\jdk17`）。
- **Maven 3.6+**
- **MySQL 8**（用户名/密码默认 `root`/`root`，可用环境变量 `DB_USERNAME` / `DB_PASSWORD` 覆盖，见 `application.yml`）
- **Node.js 18+**（前端；本机实测 v24.18.1）

## 目录结构

```
.
├── src/main/java/com/family/medicinecabinet/
│   ├── controller/      # 4 个 REST 控制器（Medicine / Category / ShoppingList / Home）
│   ├── service/         # 业务层（库存、排序、过期监控、购物清单、类别）
│   ├── mapper/          # MyBatis-Plus Mapper 接口
│   ├── entity/          # 实体 + 枚举（entity/enums）
│   ├── exception/       # 自定义异常 + 全局异常处理器
│   └── config/ common/  # 配置与通用返回
├── src/main/resources/
│   ├── application.yml  # 端口 8080，context-path=/api，数据源
│   ├── mapper/          # 4 个 MyBatis XML（CategoryMapper.xml 等）
│   └── sql/schema.sql   # 建库建表脚本（5 张表 + 初始类别数据）
├── src/test/java/       # 属性测试（jqwik）与单元测试
├── frontend/            # Vue 3 前端（说明见 frontend/README.md）
├── docs/                # RUNBOOK / CHANGELOG / PROJECT_REPORT / archive
├── .kiro/specs/family-medicine-cabinet/   # 需求 / 设计 / 任务（Spec 文档）
├── pom.xml
├── init-database.bat    # 数据库初始化脚本
├── start-backend.bat    # 后端启动脚本
├── start-frontend.bat   # 前端启动脚本
├── run-with-java17.bat / compile-with-java17.bat   # 切 JDK 17 的启动/编译脚本
└── README.md            # 本文件
```

## 启动步骤

### 1. 初始化数据库（一次性）

```bash
# 方式一：脚本（交互输入 MySQL 用户名/密码）
init-database.bat

# 方式二：手动
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS medicine_cabinet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p medicine_cabinet < src/main/resources/sql/schema.sql
```

### 2. 启动后端（需已在 JDK 17 环境）

```bash
# 方式一：脚本（依次执行 mvn clean compile -DskipTests 与 mvn spring-boot:run）
start-backend.bat
# 若默认 JAVA_HOME 为 JDK 11，改用：
run-with-java17.bat

# 方式二：手动（先确保 JAVA_HOME 指向 JDK 17）
mvn spring-boot:run
```

验证：访问 `http://localhost:8080/api/` 返回欢迎信息；`http://localhost:8080/api/medicines` 返回药品分页数据。

### 3. 启动前端

```bash
start-frontend.bat          # 或手动：cd frontend && npm install && npm run dev
```

访问 `http://localhost:3000`（vite.config.ts 中 `server.port=3000`，`/api` 代理到 `http://localhost:8080`）。

## 前端页面（路由）

| 路由 | 页面 | 说明 |
|---|---|---|
| `/` | — | 重定向到 `/dashboard` |
| `/dashboard` | 数据看板 | 统计卡片（图表功能待实现） |
| `/medicines` | 药品列表 | 增删改、记录用药、补货 |
| `/shopping` | 购物清单 | 待购买清单、标记已购买 |
| `/categories` | 类别管理 | 添加类别、查看类别下药品 |
| `/:pathMatch(.*)*` | 404 | 未匹配路径兜底 |

来源：`frontend/src/router/index.ts`。

## 接口概览（与 `src/main/java/.../controller/` 实际端点一致）

所有接口前缀为 `/api`（`application.yml` 中 `server.servlet.context-path`）。

**药品 `/medicines`（MedicineController）**

- `GET /medicines?current=1&size=10&name=` — 分页查询（名称模糊搜索可选）
- `GET /medicines/{id}` — 详情
- `POST /medicines` — 创建
- `PUT /medicines/{id}` — 更新
- `DELETE /medicines/{id}` — 删除（**物理删除**，直接删除数据行；见下方注）
- `POST /medicines/{id}/consume?amount=&userName=&symptom=` — 记录用药（原子扣减库存并写入用药记录；耗尽时自动进购物清单、频次分 +1）
- `POST /medicines/{id}/restock?amount=` — 补货

> **关于删除语义（勿被配置误导）**：`application.yml` 第 26-28 行存在 `logic-delete-field: deleted` 等"逻辑删除"配置，但**在本项目未生效**——`schema.sql` 所有表均无 `deleted` 列，实体类亦无 `deleted` 字段（`src/` 中检索 `deleted`/`@TableLogic` 仅命中该配置本身），因此 `deleteById` 实际执行物理 `DELETE`（2026-10-16 经 DDL + 实体 + 接口实测核实：调用后数据行从库中消失）。

**类别 `/categories`（CategoryController）**

- `GET /categories` — 全部类别
- `POST /categories` — 创建类别（名称唯一）
- `GET /categories/{id}/medicines` — 该类别下药品（按频次智能排序）
- `POST /categories/assign?medicineId=&categoryIds=` — 药品分配类别（多对多）

**购物清单 `/shopping-list`（ShoppingListController）**

- `GET /shopping-list/pending` — 待购买清单（含药品名称）
- `POST /shopping-list?medicineId=&reason=&customNote=` — 手动添加（reason：`EXPIRED`/`DEPLETED`/`MANUAL`）
- `POST /shopping-list/{id}/purchased?quantity=` — 标记已购买（回填库存）
- `DELETE /shopping-list/{id}` — 删除清单项

**首页（HomeController）**

- `GET /` — 欢迎信息

## 相关文档

| 文档 | 内容 |
|---|---|
| [docs/RUNBOOK.md](docs/RUNBOOK.md) | 启动、排障、数据库初始化、IDEA 配置 |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | 历次修复记录（含历史版本号） |
| [docs/PROJECT_REPORT.md](docs/PROJECT_REPORT.md) | 项目报告（架构、功能、测试覆盖） |
| [.kiro/specs/family-medicine-cabinet/](.kiro/specs/family-medicine-cabinet/) | 需求 / 设计 / 任务（含「实现偏差记录」） |
| [frontend/README.md](frontend/README.md) | 前端说明 |
| `docs/archive/` | 归档的历史过程文档（仅供追溯，内容可能过时） |

## 许可

MIT License
