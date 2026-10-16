# Maven 生命周期和常用命令详解

## 你的做法评价

### ✅ 非常合理！

你的流程：`clean → compile → test → 启动项目` 是标准的开发最佳实践！

**为什么这样做好？**

1. **clean** - 清理旧的编译文件，避免缓存问题
2. **compile** - 编译代码，确保没有语法错误
3. **test** - 运行测试，确保功能正确
4. **启动** - 在确保一切正常后再启动

这个流程确保了：
- ✅ 没有旧文件干扰
- ✅ 代码可以编译
- ✅ 测试全部通过
- ✅ 应用可以安全启动

---

## Maven 生命周期详解

Maven 有三个独立的生命周期：

### 1. Clean 生命周期（清理）

| 阶段 | 说明 | 常用度 |
|------|------|--------|
| **pre-clean** | 执行清理前的工作 | ⭐ |
| **clean** | 删除 target 目录 | ⭐⭐⭐⭐⭐ |
| **post-clean** | 执行清理后的工作 | ⭐ |

### 2. Default 生命周期（构建）⭐ 最重要

| 阶段 | 说明 | 做什么 | 常用度 |
|------|------|--------|--------|
| **validate** | 验证项目 | 检查项目配置是否正确 | ⭐⭐ |
| **compile** | 编译 | 编译 src/main/java 下的代码 | ⭐⭐⭐⭐⭐ |
| **test** | 测试 | 运行 src/test/java 下的测试 | ⭐⭐⭐⭐⭐ |
| **package** | 打包 | 生成 jar 或 war 文件 | ⭐⭐⭐⭐⭐ |
| **verify** | 验证 | 检查包是否有效 | ⭐⭐⭐ |
| **install** | 安装 | 安装到本地 Maven 仓库 | ⭐⭐⭐⭐ |
| **deploy** | 部署 | 部署到远程 Maven 仓库 | ⭐⭐⭐ |

### 3. Site 生命周期（文档）

| 阶段 | 说明 | 常用度 |
|------|------|--------|
| **pre-site** | 生成站点前的工作 | ⭐ |
| **site** | 生成项目站点文档 | ⭐⭐ |
| **post-site** | 生成站点后的工作 | ⭐ |
| **site-deploy** | 部署站点到服务器 | ⭐ |

---

## IDEA Maven 面板常用按钮详解

### 🔧 顶部工具栏按钮

```
┌─────────────────────────────────────────────────┐
│ 🔄 ⚡ 📦 ⬇️ 🔍 ⚙️                              │
└─────────────────────────────────────────────────┘
```

| 图标 | 名称 | 功能 | 使用场景 |
|------|------|------|----------|
| 🔄 | Reload | 重新加载 Maven 项目 | pom.xml 修改后 |
| ⚡ | Execute | 执行 Maven 目标 | 运行自定义命令 |
| 📦 | Generate | 生成源码 | 很少用 |
| ⬇️ | Download | 下载源码和文档 | 查看依赖源码 |
| 🔍 | Show Dependencies | 显示依赖树 | 排查依赖冲突 |
| ⚙️ | Settings | Maven 设置 | 配置 Maven |

### 📁 Lifecycle（生命周期）

这是你最常用的部分！

```
Lifecycle
├── clean          ⭐⭐⭐⭐⭐ 清理 target 目录
├── validate       ⭐⭐ 验证项目配置
├── compile        ⭐⭐⭐⭐⭐ 编译主代码
├── test           ⭐⭐⭐⭐⭐ 运行测试
├── package        ⭐⭐⭐⭐⭐ 打包成 jar/war
├── verify         ⭐⭐⭐ 验证包
├── install        ⭐⭐⭐⭐ 安装到本地仓库
└── deploy         ⭐⭐⭐ 部署到远程仓库
```

### 🔌 Plugins（插件）

常用插件命令：

```
Plugins
├── spring-boot
│   ├── spring-boot:run      ⭐⭐⭐⭐⭐ 运行 Spring Boot 应用
│   ├── spring-boot:start    ⭐⭐⭐ 后台启动应用
│   └── spring-boot:stop     ⭐⭐⭐ 停止应用
├── compiler
│   ├── compiler:compile     ⭐⭐⭐⭐ 编译代码
│   └── compiler:testCompile ⭐⭐⭐ 编译测试代码
├── surefire
│   └── surefire:test        ⭐⭐⭐⭐ 运行单元测试
└── dependency
    ├── dependency:tree      ⭐⭐⭐⭐ 查看依赖树
    └── dependency:analyze   ⭐⭐⭐ 分析依赖
```

---

## 常用操作流程

### 1. 日常开发流程（你的做法）✅

```
clean → compile → test → 启动应用
  ↓         ↓        ↓         ↓
清理旧文件  编译代码  运行测试  验证功能
```

**命令**：
```bash
mvn clean compile test
```

**IDEA 操作**：
1. 双击 `clean`
2. 双击 `compile`
3. 双击 `test`
4. 右键 `MedicineCabinetApplication` → Run

### 2. 快速编译（跳过测试）

```
clean → compile → package -DskipTests
```

**命令**：
```bash
mvn clean package -DskipTests
```

**使用场景**：
- 测试很慢，只想快速打包
- 已经确认测试通过，不想重复运行

### 3. 完整构建（推荐用于发布）

```
clean → compile → test → package → install
```

**命令**：
```bash
mvn clean install
```

**使用场景**：
- 准备发布版本
- 需要在本地仓库安装项目

### 4. 只运行测试

```
test
```

**命令**：
```bash
mvn test
```

**使用场景**：
- 代码已编译，只想重新运行测试
- 调试测试用例

### 5. 只编译不测试

```
clean → compile
```

**命令**：
```bash
mvn clean compile
```

**使用场景**：
- 快速检查代码是否有语法错误
- 不关心测试结果

---

## Maven 阶段的依赖关系

**重要**：Maven 的阶段是有顺序的，执行后面的阶段会自动执行前面的阶段！

```
validate → compile → test → package → verify → install → deploy
    ↓          ↓        ↓        ↓         ↓         ↓         ↓
  验证      编译      测试     打包      验证      安装      部署
```

### 示例：

```bash
# 执行 test 会自动执行 validate 和 compile
mvn test
# 等同于：
mvn validate compile test

# 执行 package 会自动执行前面所有阶段
mvn package
# 等同于：
mvn validate compile test package

# 执行 install 会自动执行前面所有阶段
mvn install
# 等同于：
mvn validate compile test package verify install
```

---

## 你的项目推荐流程

### 开发阶段（每次修改代码后）

```
1. clean    - 清理旧文件
2. compile  - 编译代码
3. test     - 运行测试
4. 启动应用 - 验证功能
```

### 提交代码前

```
1. clean
2. install  - 会自动执行 compile、test、package
3. 确认所有测试通过
4. 提交代码
```

### 准备部署

```
1. clean
2. package -DskipTests  - 打包（如果测试已通过可跳过）
3. 检查 target/medicine-cabinet-1.0.0.jar
4. 部署到服务器
```

---

## 常见问题

### Q1: 为什么要先 clean？

**A**: 清理旧的编译文件，避免：
- 旧的 class 文件干扰
- 删除的文件仍然存在
- 缓存导致的奇怪问题

### Q2: compile 和 test 的区别？

**A**: 
- `compile` - 只编译 `src/main/java` 下的代码
- `test` - 编译并运行 `src/test/java` 下的测试

### Q3: 什么时候用 package？

**A**: 
- 需要生成 jar/war 文件时
- 准备部署到服务器时
- 需要分发给其他人时

### Q4: install 和 deploy 的区别？

**A**:
- `install` - 安装到**本地** Maven 仓库（~/.m2/repository）
- `deploy` - 部署到**远程** Maven 仓库（公司私服或 Maven Central）

### Q5: 为什么有时候 Maven 很慢？

**A**: 可能原因：
- 下载依赖（第一次）
- 运行测试（特别是集成测试）
- 网络问题（连接 Maven 中央仓库）

**解决方案**：
- 使用国内镜像（阿里云）
- 跳过测试：`-DskipTests`
- 使用 IDEA 内置编译器

---

## IDEA 特有功能

### 1. 右键菜单快捷方式

在 Maven 面板的任何阶段上右键：

```
├── Run Maven Build          - 运行该阶段
├── Debug Maven Build        - 调试该阶段
├── Create 'xxx'...          - 创建运行配置
└── Assign Shortcut          - 分配快捷键
```

### 2. 组合命令

在顶部工具栏点击 ⚡ Execute Maven Goal：

```
# 常用组合
clean install
clean package -DskipTests
clean test
```

### 3. 运行配置

可以创建自定义的 Maven 运行配置：

```
Run → Edit Configurations → + → Maven
Command line: clean install -DskipTests
```

---

## 最佳实践总结

### ✅ 推荐做法

1. **每次修改代码后**：`clean → compile → test`
2. **提交代码前**：`clean → install`
3. **准备部署**：`clean → package`
4. **使用 IDEA 编译器**：避免 Maven + Lombok 问题

### ❌ 避免做法

1. **不要跳过 clean**：可能导致缓存问题
2. **不要跳过 test**：可能引入 bug
3. **不要直接 deploy**：先在本地测试

### 🎯 你的流程评分

```
你的流程：clean → compile → test → 启动
评分：⭐⭐⭐⭐⭐ (5/5)
评价：完美！这是标准的开发最佳实践！
```

---

## 快速参考卡片

```
┌─────────────────────────────────────────┐
│  Maven 常用命令速查                      │
├─────────────────────────────────────────┤
│  mvn clean              清理             │
│  mvn compile            编译             │
│  mvn test               测试             │
│  mvn package            打包             │
│  mvn install            安装到本地       │
│  mvn clean install      完整构建         │
│  mvn clean package -DskipTests  快速打包│
│  mvn spring-boot:run    运行应用         │
│  mvn dependency:tree    查看依赖树       │
└─────────────────────────────────────────┘
```

---

## 总结

你的做法 `clean → compile → test → 启动` 是**完全正确**的！

这个流程：
- ✅ 确保代码可以编译
- ✅ 确保测试全部通过
- ✅ 避免缓存问题
- ✅ 符合行业最佳实践

继续保持这个好习惯！🎉
