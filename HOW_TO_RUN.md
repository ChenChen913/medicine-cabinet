# 如何运行项目 - 完整指南

## 📋 目录
1. [环境准备](#环境准备)
2. [后端运行](#后端运行)
3. [前端运行](#前端运行)
4. [常见问题](#常见问题)
5. [测试验证](#测试验证)

---

## 环境准备

### 1. 检查Java版本

```bash
java -version
```

**要求**: Java 17 或更高版本

**当前系统**: Java 8 (需要升级)

#### 升级Java到17 (Windows)

**步骤1**: 下载Java 17
- 访问: https://adoptium.net/temurin/releases/?version=17
- 选择: Windows x64, JDK 17 (LTS)
- 下载 `.msi` 安装包

**步骤2**: 安装Java 17
- 运行下载的安装程序
- 记住安装路径，例如: `C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot`

**步骤3**: 设置环境变量
```cmd
# 以管理员身份打开命令提示符
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot" /M
setx PATH "%JAVA_HOME%\bin;%PATH%" /M
```

**步骤4**: 验证安装
```bash
# 关闭并重新打开命令提示符
java -version
```

应该显示:
```
openjdk version "17.0.x"
```

### 2. 检查Maven

```bash
mvn -version
```

**要求**: Maven 3.6 或更高版本

如果没有安装Maven，下载: https://maven.apache.org/download.cgi

### 3. 检查MySQL

```bash
mysql --version
```

**要求**: MySQL 8.0 或更高版本

如果没有安装MySQL，下载: https://dev.mysql.com/downloads/mysql/

### 4. 检查Node.js (前端需要)

```bash
node -v
npm -v
```

**要求**: Node.js 18 或更高版本

如果没有安装Node.js，下载: https://nodejs.org/

---

## 后端运行

### 步骤1: 创建数据库

打开MySQL命令行或MySQL Workbench:

```sql
-- 创建数据库
CREATE DATABASE medicine_cabinet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 查看数据库
SHOW DATABASES;
```

### 步骤2: 执行建表脚本

**方法1: 使用命令行**
```bash
mysql -u root -p medicine_cabinet < src/main/resources/sql/schema.sql
```

**方法2: 使用MySQL Workbench**
1. 打开 `src/main/resources/sql/schema.sql` 文件
2. 复制所有内容
3. 在MySQL Workbench中执行

**验证表是否创建成功**:
```sql
USE medicine_cabinet;
SHOW TABLES;
```

应该看到5个表:
- medicines
- categories
- medicine_categories
- usage_logs
- shopping_list

### 步骤3: 配置数据库连接

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/medicine_cabinet?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root          # 修改为你的MySQL用户名
    password: your_password # 修改为你的MySQL密码
```

### 步骤4: 编译项目

```bash
# 清理并编译
mvn clean compile
```

**期望输出**:
```
[INFO] BUILD SUCCESS
```

**如果出现编译错误**:
- 检查Java版本是否为17
- 检查Maven配置是否正确
- 查看错误信息并参考"常见问题"部分

### 步骤5: 运行测试 (可选)

```bash
# 运行所有测试
mvn test

# 只运行属性测试
mvn test -Dtest=*PropertyTest
```

### 步骤6: 启动后端服务

```bash
mvn spring-boot:run
```

**期望输出**:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

...
Started MedicineCabinetApplication in X.XXX seconds
```

**后端服务地址**: http://localhost:8080/api

### 步骤7: 验证后端是否启动成功

打开浏览器或使用curl:

```bash
# 测试API是否可访问
curl http://localhost:8080/api/medicines

# 或者在浏览器中访问
# http://localhost:8080/api/medicines
```

应该返回JSON格式的空列表或药品数据。

---

## 前端运行

### 重要说明

前端采用**文档化实现方案**，需要先创建完整的文件结构。

### 步骤1: 查看前端实现文档

```bash
# 查看完整的前端实现文档
cat frontend/FRONTEND_IMPLEMENTATION.md

# 或在编辑器中打开
code frontend/FRONTEND_IMPLEMENTATION.md
```

### 步骤2: 创建前端文件结构

根据 `frontend/FRONTEND_IMPLEMENTATION.md` 文档，创建以下目录和文件:

```
frontend/src/
├── api/
│   ├── medicine.ts
│   ├── shopping.ts
│   └── category.ts
├── assets/
├── components/
│   ├── MedicineCard.vue
│   └── ConsumeModal.vue
├── router/
│   └── index.ts
├── stores/
│   ├── medicine.ts
│   └── shopping.ts
├── types/
│   └── index.ts
├── utils/
│   ├── request.ts
│   └── format.ts
├── views/
│   ├── Dashboard.vue
│   ├── MedicineList.vue
│   ├── ShoppingList.vue
│   └── CategoryManagement.vue
├── App.vue
├── main.ts (已存在)
└── style.css
```

### 步骤3: 复制代码

从 `frontend/FRONTEND_IMPLEMENTATION.md` 文档中复制对应的代码到各个文件。

**关键文件**:
1. `src/types/index.ts` - TypeScript类型定义
2. `src/utils/request.ts` - Axios封装
3. `src/api/medicine.ts` - 药品API
4. `src/router/index.ts` - 路由配置
5. `src/App.vue` - 主应用组件
6. `src/views/MedicineList.vue` - 药品列表页面
7. `src/style.css` - 全局样式

### 步骤4: 安装依赖

```bash
cd frontend
npm install
```

**期望输出**:
```
added XXX packages in XXs
```

### 步骤5: 启动前端开发服务器

```bash
npm run dev
```

**期望输出**:
```
  VITE v5.0.x  ready in XXX ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
  ➜  press h to show help
```

**前端应用地址**: http://localhost:3000

### 步骤6: 访问前端应用

打开浏览器访问: http://localhost:3000

应该看到药箱管理系统的界面。

---

## 常见问题

### Q1: 编译错误 "无效的标记: --release"

**原因**: Java版本过低（当前是Java 8，需要Java 17）

**解决方案**:
1. 升级Java到17（参考"环境准备"部分）
2. 或使用Java 8兼容版本:
```bash
# 备份当前pom.xml
copy pom.xml pom-java17.xml.backup

# 使用Java 8兼容配置
copy pom-java8.xml.backup pom.xml

# 重新编译
mvn clean compile
```

### Q2: 数据库连接失败

**错误信息**: `Communications link failure`

**解决方案**:
1. 检查MySQL服务是否启动
```bash
# Windows
net start MySQL80

# 或在服务管理器中启动MySQL服务
```

2. 检查数据库配置
- 用户名和密码是否正确
- 数据库名称是否正确
- 端口是否为3306

3. 测试数据库连接
```bash
mysql -u root -p
```

### Q3: 端口8080已被占用

**错误信息**: `Port 8080 was already in use`

**解决方案**:
修改 `src/main/resources/application.yml`:
```yaml
server:
  port: 8081  # 改为其他端口
```

### Q4: 前端无法连接后端

**错误信息**: `Network Error` 或 `CORS error`

**解决方案**:
1. 确认后端已启动（http://localhost:8080/api）
2. 检查前端代理配置 `frontend/vite.config.ts`
3. 确认后端CORS配置正确（已在CorsConfig.java中配置）

### Q5: Maven下载依赖很慢

**解决方案**:
配置Maven使用阿里云镜像。

编辑 `~/.m2/settings.xml`:
```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### Q6: npm install 很慢

**解决方案**:
使用淘宝镜像:
```bash
npm config set registry https://registry.npmmirror.com
npm install
```

---

## 测试验证

### 后端API测试

#### 1. 创建药品

```bash
curl -X POST http://localhost:8080/api/medicines \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"感冒灵颗粒\",
    \"dosageForm\": \"GRANULE\",
    \"unit\": \"包\",
    \"totalQuantity\": 20,
    \"threshold\": 5,
    \"expiryDate\": \"2025-12-31\",
    \"symptomsTreated\": \"感冒、发烧、头痛\",
    \"dosageInstruction\": \"一日3次，一次1包\",
    \"sideEffects\": \"偶见恶心、头晕\",
    \"usageFrequencyScore\": 0,
    \"status\": \"NORMAL\"
  }"
```

#### 2. 查询药品列表

```bash
curl http://localhost:8080/api/medicines
```

#### 3. 消耗药品

```bash
curl -X POST "http://localhost:8080/api/medicines/{药品ID}/consume?amount=2&userName=张三&symptom=感冒"
```

#### 4. 查询购物清单

```bash
curl http://localhost:8080/api/shopping-list/pending
```

### 前端功能测试

1. **药品管理**
   - 添加药品
   - 查看药品列表
   - 编辑药品信息
   - 删除药品

2. **用药记录**
   - 记录用药
   - 查看库存变化
   - 查看低库存预警

3. **购物清单**
   - 查看待购买清单
   - 标记已购买
   - 删除清单项

4. **类别管理**
   - 创建类别
   - 分配药品到类别
   - 按类别筛选药品

---

## 🎯 快速启动命令总结

### 后端
```bash
# 1. 创建数据库（在MySQL中执行）
CREATE DATABASE medicine_cabinet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2. 执行建表脚本
mysql -u root -p medicine_cabinet < src/main/resources/sql/schema.sql

# 3. 修改数据库配置
# 编辑 src/main/resources/application.yml

# 4. 启动后端
mvn spring-boot:run
```

### 前端
```bash
# 1. 进入前端目录
cd frontend

# 2. 创建文件结构（参考 FRONTEND_IMPLEMENTATION.md）

# 3. 安装依赖
npm install

# 4. 启动前端
npm run dev
```

---

## 📚 相关文档

- **QUICK_START.md** - 快速启动指南
- **FINAL_PROJECT_REPORT.md** - 项目完成报告
- **frontend/FRONTEND_IMPLEMENTATION.md** - 前端完整实现文档
- **PROJECT_CHECK_REPORT.md** - 代码检查报告

---

## 🆘 获取帮助

如果遇到问题:
1. 查看本文档的"常见问题"部分
2. 查看 `QUICK_START.md`
3. 查看 `FINAL_PROJECT_REPORT.md`
4. 检查日志输出中的错误信息

---

**祝你运行成功！** 🎉
