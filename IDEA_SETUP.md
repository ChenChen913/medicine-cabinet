# IntelliJ IDEA 项目配置指南

## 问题说明

当前项目在命令行使用 `mvn compile` 时遇到 Lombok 注解处理器失效的问题。这是 Maven 命令行在 Windows + Java 17 环境下的已知兼容性问题。

**解决方案**：使用 IntelliJ IDEA 进行编译、测试和运行。

---

## 配置步骤

### 1. 安装 Lombok 插件

1. 打开 IntelliJ IDEA
2. 进入 **File → Settings** (或按 `Ctrl + Alt + S`)
3. 选择 **Plugins**
4. 在搜索框中输入 **"Lombok"**
5. 找到 **Lombok** 插件，点击 **Install**
6. 安装完成后，点击 **Restart IDE** 重启 IDEA

### 2. 启用注解处理

1. 打开 **File → Settings** (或按 `Ctrl + Alt + S`)
2. 导航到 **Build, Execution, Deployment → Compiler → Annotation Processors**
3. 勾选 **Enable annotation processing**
4. 确保以下选项已设置：
   - **Obtain processors from project classpath** (选中)
   - **Store generated sources relative to**: Module content root
   - **Production sources directory**: target/generated-sources/annotations
   - **Test sources directory**: target/generated-test-sources/test-annotations
5. 点击 **Apply** 和 **OK**

### 3. 配置项目 SDK

1. 打开 **File → Project Structure** (或按 `Ctrl + Alt + Shift + S`)
2. 选择 **Project**
3. 确认 **Project SDK** 设置为 **Java 17**
4. **Project language level** 设置为 **17 - Sealed types, always-strict floating-point semantics**
5. 点击 **Apply** 和 **OK**

### 4. 重新导入 Maven 项目

1. 在右侧边栏找到 **Maven** 工具窗口（如果没有显示，点击 View → Tool Windows → Maven）
2. 点击刷新按钮（🔄 Reload All Maven Projects）
3. 等待依赖下载完成

### 5. 清理并重新构建项目

1. 选择 **Build → Clean Project**
2. 等待清理完成
3. 选择 **Build → Rebuild Project**
4. 等待构建完成

---

## 运行测试

### 方式 1：在 IDE 中运行所有测试

1. 在项目结构中找到 `src/test/java` 目录
2. 右键点击 `src/test/java`
3. 选择 **Run 'All Tests'**

### 方式 2：运行特定的测试类

1. 打开测试文件，例如 `MedicinePropertyTest.java`
2. 点击类名旁边的绿色运行按钮
3. 选择 **Run 'MedicinePropertyTest'**

### 方式 3：运行单个测试方法

1. 打开测试文件
2. 找到要运行的测试方法
3. 点击方法名旁边的绿色运行按钮
4. 选择 **Run 'testMethodName()'**

---

## 运行应用程序

### 方式 1：使用 Spring Boot 运行配置

1. 找到主类 `MedicineCabinetApplication.java`
2. 右键点击文件
3. 选择 **Run 'MedicineCabinetApplication'**

### 方式 2：使用 Maven 运行

1. 打开右侧的 **Maven** 工具窗口
2. 展开 **medicine-cabinet → Plugins → spring-boot**
3. 双击 **spring-boot:run**

---

## 验证 Lombok 是否工作

### 快速测试

1. 打开任意实体类，例如 `Medicine.java`
2. 在类中输入 `medicine.get` 然后按 `Ctrl + Space`
3. 如果看到自动补全提示中有 `getName()`, `getId()` 等方法，说明 Lombok 工作正常

### 查看生成的代码

1. 打开任意使用 `@Data` 注解的类
2. 按 `Ctrl + F12` 查看类的结构
3. 应该能看到 Lombok 生成的 getter/setter 方法

---

## 常见问题

### Q1: 提示 "Cannot resolve symbol 'log'"

**原因**：Lombok 插件未正确安装或注解处理未启用

**解决方案**：
1. 确认 Lombok 插件已安装
2. 确认注解处理已启用
3. 重启 IDEA
4. 重新构建项目

### Q2: 编译时提示找不到 getter/setter 方法

**原因**：注解处理器未运行

**解决方案**：
1. **Build → Clean Project**
2. **Build → Rebuild Project**
3. 如果还不行，尝试 **File → Invalidate Caches / Restart**

### Q3: Maven 工具窗口中的测试失败

**原因**：Maven 命令行编译问题

**解决方案**：
- 不要使用 Maven 工具窗口中的 **test** 生命周期
- 改用 IDE 的测试运行器（右键点击测试类 → Run）

### Q4: 数据库连接失败

**原因**：MySQL 未启动或配置错误

**解决方案**：
1. 启动 MySQL 服务
2. 检查 `src/main/resources/application.yml` 中的数据库配置
3. 确保数据库 `medicine_cabinet` 已创建

---

## 推荐的工作流程

### 开发流程

1. **编写代码** → 在 IDEA 中编辑 Java 文件
2. **自动编译** → IDEA 会自动编译（如果启用了 Build automatically）
3. **运行测试** → 右键点击测试类 → Run
4. **调试** → 使用 IDEA 的调试器（点击 Debug 按钮）
5. **运行应用** → 右键点击主类 → Run

### 测试流程

1. 打开测试类（例如 `MedicinePropertyTest.java`）
2. 右键点击类名
3. 选择 **Run 'MedicinePropertyTest'**
4. 查看测试结果窗口
5. 如果测试失败，点击失败的测试查看详细信息

---

## 性能优化建议

### 启用自动构建

1. **File → Settings → Build, Execution, Deployment → Compiler**
2. 勾选 **Build project automatically**
3. 这样修改代码后会自动编译

### 增加 IDEA 内存

如果项目较大，可以增加 IDEA 的内存：

1. **Help → Edit Custom VM Options**
2. 修改以下参数：
```
-Xms512m
-Xmx2048m
```

---

## 总结

使用 IntelliJ IDEA 的优势：

✅ Lombok 插件完美支持  
✅ 自动编译和热重载  
✅ 强大的调试功能  
✅ 智能代码补全  
✅ 集成的测试运行器  
✅ 无需处理 Maven 命令行兼容性问题  

**建议**：完全在 IDEA 中进行开发、测试和运行，避免使用命令行 Maven。

---

## 下一步

配置完成后，你可以：

1. ✅ 运行所有测试：右键点击 `src/test/java` → Run 'All Tests'
2. ✅ 启动应用：右键点击 `MedicineCabinetApplication` → Run
3. ✅ 访问应用：http://localhost:8080/api

如果遇到任何问题，请检查上面的"常见问题"部分。
