# 家庭智慧药箱管理系统 - 快速启动指南

## 🚨 重要提示

**当前系统Java版本**: Java 8 (1.8.0_401)  
**项目要求Java版本**: Java 17+  
**状态**: ❌ 需要升级Java版本才能运行

## 📋 前置条件检查

### 1. Java版本检查
```bash
java -version
```

**期望输出**:
```
java version "17.0.x" 或更高版本
```

**当前输出**:
```
java version "1.8.0_401"  ❌ 版本过低
```

### 2. Maven版本检查
```bash
mvn -version
```

**要求**: Maven 3.6+

### 3. MySQL版本检查
```bash
mysql --version
```

**要求**: MySQL 8.0+

## 🔧 解决方案

### 方案A: 升级Java版本 (强烈推荐)

#### Windows系统:

**步骤1**: 下载Java 17
- Oracle JDK: https://www.oracle.com/java/technologies/downloads/#java17
- OpenJDK (推荐): https://adoptium.net/temurin/releases/?version=17

**步骤2**: 安装Java 17
- 运行下载的安装程序
- 记住安装路径，例如: `C:\Program Files\Java\jdk-17`

**步骤3**: 设置环境变量
```cmd
# 打开命令提示符（管理员权限）
setx JAVA_HOME "C:\Program Files\Java\jdk-17" /M
setx PATH "%JAVA_HOME%\bin;%PATH%" /M
```

**步骤4**: 验证安装
```bash
# 关闭并重新打开命令提示符
java -version
```

应该显示:
```
java version "17.0.x"
```

### 方案B: 使用Java 8兼容版本 (不推荐)

**警告**: Spring Boot 3.x 需要 Java 17+。降级到Spring Boot 2.7.x会失去一些新特性。

**步骤1**: 备份当前pom.xml
```bash
copy pom.xml pom-java17.xml.backup
```

**步骤2**: 使用Java 8兼容配置
```bash
copy pom-java8.xml.backup pom.xml
```

**步骤3**: 清理并重新编译
```bash
mvn clean compile
```

## 🚀 启动步骤 (升级Java后)

### 1. 创建数据库
```sql
CREATE DATABASE medicine_cabinet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 执行建表脚本
```bash
mysql -u root -p medicine_cabinet < src/main/resources/sql/schema.sql
```

或者直接在MySQL客户端中执行 `src/main/resources/sql/schema.sql` 文件内容。

### 3. 配置数据库连接
编辑 `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/medicine_cabinet?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root      # 修改为你的MySQL用户名
    password: your_password  # 修改为你的MySQL密码
```

### 4. 编译项目
```bash
mvn clean compile
```

**期望输出**:
```
[INFO] BUILD SUCCESS
```

### 5. 运行测试 (可选)
```bash
mvn test
```

### 6. 启动应用
```bash
mvn spring-boot:run
```

**期望输出**:
```
Started MedicineCabinetApplication in X.XXX seconds
```

### 7. 验证启动
打开浏览器访问:
```
http://localhost:8080/api/medicines
```

应该返回空列表或药品数据（JSON格式）。

## 📝 API测试

### 使用curl测试

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
    \"sideEffects\": \"偶见恶心、头晕\"
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

### 使用Postman测试
1. 导入API集合
2. 设置Base URL: `http://localhost:8080/api`
3. 测试各个接口

## 🐛 常见问题

### Q1: 编译错误 "无效的标记: --release"
**原因**: Java版本过低  
**解决**: 升级到Java 17

### Q2: 数据库连接失败
**原因**: MySQL未启动或配置错误  
**解决**: 
1. 检查MySQL服务是否运行
2. 验证用户名密码
3. 确认数据库已创建

### Q3: 端口8080已被占用
**解决**: 修改 `application.yml` 中的端口:
```yaml
server:
  port: 8081  # 改为其他端口
```

### Q4: 定时任务不执行
**原因**: 可能是时区问题  
**解决**: 检查系统时区设置

## 📊 项目状态

- ✅ 后端代码: 100%完成
- ✅ 数据库设计: 100%完成
- ✅ API接口: 100%完成
- ✅ 属性测试: 100%完成
- ❌ 前端界面: 未实现
- ❌ Java版本: 需要升级

## 📞 获取帮助

如果遇到问题:
1. 查看 `PROJECT_CHECK_REPORT.md` 了解详细检查结果
2. 查看 `PROJECT_SUMMARY.md` 了解项目概述
3. 查看日志文件排查错误

## 🎯 下一步

1. **立即**: 升级Java版本到17
2. **然后**: 按照上述步骤启动项目
3. **接着**: 使用API测试工具验证功能
4. **最后**: 开发前端界面

---

**祝你使用愉快！** 🎉
