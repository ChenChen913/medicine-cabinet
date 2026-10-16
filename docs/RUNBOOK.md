# 运行手册（RUNBOOK）

本项目启动、排障、数据库初始化与 IDEA 配置的操作手册。所有命令与端口均以仓库内实际脚本和配置为准。

---

## 1. 环境准备

### 1.1 JDK 17（必须）

`pom.xml` 要求 Java 17（`<java.version>17</java.version>`，maven-compiler-plugin `source/target=17`）。

> ⚠️ 本机默认环境实测为 **Java 11**（`java -version` → `11.0.8`，`JAVA_HOME=D:\java\jdk11`，2026-10-16 实测），**低于项目要求**。直接编译会失败，必须先切到 JDK 17（本机已安装在 `D:\java\jdk17`）。

切换方式（任选其一）：

```bat
:: 方式一：使用根目录脚本（脚本内已 set JAVA_HOME=D:\java\jdk17）
compile-with-java17.bat        :: 编译
run-with-java17.bat            :: 启动应用

:: 方式二：当前命令行窗口临时切换
set JAVA_HOME=D:\java\jdk17
set PATH=%JAVA_HOME%\bin;%PATH%
java -version                  :: 应显示 17.x
```

方式三：系统环境变量中将 `JAVA_HOME` 永久改为 `D:\java\jdk17`（本机 `D:\java` 下同时存在其他版本，注意别选错）。

> 注意：`start-backend.bat` **本身不切换** `JAVA_HOME`，仅依次检查 Java 与 Maven 版本后执行 Maven；如果默认 `JAVA_HOME` 仍指向 JDK 11，脚本会在编译阶段失败。此时请改用 `run-with-java17.bat`。

### 1.2 其他依赖

| 依赖 | 要求 | 说明 |
|---|---|---|
| Maven | 3.6+ | `start-backend.bat` 与手动启动均使用 `mvn` |
| MySQL | 8 | 库名 `medicine_cabinet`（见 `schema.sql`） |
| Node.js | 18+ | 前端（本机实测 v24.18.1）；`start-frontend.bat` 会检查 `node -v` |

关于 Maven 命令：根目录脚本（`start-backend.bat`、`run-with-java17.bat`、`compile-with-java17.bat`）内部都是调用 `mvn`。命令行直接使用 `mvn` 是正常方式，唯一前提是 `JAVA_HOME` 指向 JDK 17。（历史文档中"绝对禁止使用 mvn"的说法与脚本自身行为矛盾，已作废。）

---

## 2. 数据库初始化（一次性）

### 方式一：脚本

```bat
init-database.bat
```

交互式输入 MySQL 用户名（默认 `root`）和密码，脚本会：
1. `CREATE DATABASE IF NOT EXISTS medicine_cabinet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
2. 执行 `src\main\resources\sql\schema.sql` 建表

### 方式二：手动

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS medicine_cabinet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p medicine_cabinet < src/main/resources/sql/schema.sql
```

`schema.sql` 会建立 5 张表（`medicines`、`categories`、`medicine_categories`、`usage_logs`、`shopping_list`）并插入 5 条初始类别数据（感冒药/止痛药/肠胃药/外用药/维生素）。

### 验证

```bash
mysql -u root -p medicine_cabinet -e "SHOW TABLES;"
```

### 连接配置

`src/main/resources/application.yml`：数据库 `jdbc:mysql://localhost:3306/medicine_cabinet`，用户名/密码默认 `root`/`root`，可用环境变量覆盖：

```bash
set DB_USERNAME=你的用户名
set DB_PASSWORD=你的密码
```

---

## 3. 启动后端

```bat
start-backend.bat        :: [1/4] 检查 Java → [2/4] 检查 Maven → [3/4] mvn clean compile -DskipTests → [4/4] mvn spring-boot:run
```

或手动（需 JDK 17 环境）：

```bash
mvn clean compile -DskipTests
mvn spring-boot:run
```

- 端口 `8080`，context-path `/api`（`application.yml`），所有接口前缀均为 `/api`
- 验证：浏览器/curl 访问 `http://localhost:8080/api/`，应返回"欢迎使用家庭智慧药箱管理系统API服务！"；`http://localhost:8080/api/medicines` 应返回分页数据

---

## 4. 启动前端

```bat
start-frontend.bat       :: [1/3] 检查 Node → [2/3] cd frontend → [3/3] 无 node_modules 时 npm install → npm run dev
```

或手动：

```bash
cd frontend
npm install              # 首次
npm run dev
```

- 访问 `http://localhost:3000`（`vite.config.ts` 中 `server.port: 3000`）
- 开发代理：`/api` → `http://localhost:8080`（`vite.config.ts`），因此前端页面无需关心跨域

---

## 5. 运行测试

```bash
mvn test
```

- 属性测试使用 jqwik 1.7.4（`@PropertyDefaults(tries = 100)`，每个属性运行 100 次）
- 注意：jqwik 的 `@Property` 方法**不会**自动注入 Spring 上下文/`@MockBean`；属性测试均不靠容器注入——`InventoryPropertyTest`、`MedicinePropertyTest` 在方法内手动创建 mock，`SortingPropertyTest` 直接 `new SortingServiceImpl()`（该类上仍保留 `@SpringBootTest` 注解）
- 普通单元测试（`InventoryServiceImplTest`、`ShoppingListServiceImplTest`）为纯 Mockito（手动 `mock()`，不启动 Spring）；`MedicineValidationTest` 使用 `@SpringBootTest` + MockMvc 走完整校验链路

---

## 6. IDEA 配置（一次性）

1. **Project SDK**：`File → Project Structure → Project`，SDK 选择 `17`（`D:\java\jdk17`），Language level 17
2. **Maven**：`Settings → Build, Execution, Deployment → Build Tools → Maven`，Maven home 与 JDK 使用本机 17 版；修改 `pom.xml` 后点 Maven 面板 🔄 Reload
3. **Lombok**：
   - 安装 Lombok 插件（新版 IDEA 已内置）
   - 勾选 `Settings → Build, Execution, Deployment → Compiler → Annotation Processors` 的 **Enable annotation processing**（不勾选会出现"找不到符号 log/info"一类编译错误）
4. 编译/运行：`Build → Rebuild Project`；右键 `MedicineCabinetApplication` → Run

---

## 7. 常见问题排障

| 现象 | 原因 | 处理 |
|---|---|---|
| 编译报 `不支持发行版本 17`（release version 17 not supported）一类错误 | `JAVA_HOME` 不是 JDK 17（JDK 11 下 javac 对 `--release 17` 的实测报错文本，2026-10-16） | 按 §1.1 切换；或用 `compile-with-java17.bat` |
| 编译报"找不到符号"（`log`、`info` 等） | Lombok 注解处理未生效（IDEA）或变量名与 `@Slf4j` 的 `log` 冲突（历史问题） | 勾选 Enable annotation processing；历史修复过程见 `docs/CHANGELOG.md` |
| 启动报 `Invalid value type for attribute 'factoryBeanObjectType': java.lang.String` | MyBatis-Plus 与 Spring Boot 3.2 的兼容性问题（历史版本） | 当前 `pom.xml` 已为 3.5.10.1；升级过程见 `docs/CHANGELOG.md` |
| 后端启动报端口占用 | 8080 被占用 | 关掉占用进程，或临时改 `application.yml` 的 `server.port` |
| 前端启动报端口占用 | 3000 被占用 | 改 `vite.config.ts` 的 `server.port` |
| `npm install` 慢或失败 | 网络问题 | `npm config set registry https://registry.npmmirror.com` 后重试 |
| 启动连不上数据库 | MySQL 未启动 / 库不存在 / 密码不对 | 检查服务、执行 §2、核对 `application.yml` 或 `DB_USERNAME`/`DB_PASSWORD` |
| IDEA 中 Maven 报依赖解析失败 | 本地仓库缓存/网络 | Maven 面板 Reload；检查网络或配置镜像 |

---

## 8. 相关文档

- 项目入口与接口概览：[../README.md](../README.md)
- 历次修复记录：[CHANGELOG.md](CHANGELOG.md)
- 项目报告：[PROJECT_REPORT.md](PROJECT_REPORT.md)
- 历史过程文档（可能过时）：`archive/`
