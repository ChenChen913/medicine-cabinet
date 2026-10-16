# IDEA 编译器配置修复指南

## 问题分析

错误 "找不到符号 SortingService" 表明 IDEA 在使用 Maven 编译器时遇到了问题。这是因为 Maven 命令行编译器在 Windows + Java 17 环境下存在 Lombok 兼容性问题。

## 解决方案：配置 IDEA 使用内置编译器

### 步骤 1：切换到 IDEA 编译器

1. 打开 **File → Settings** (Ctrl + Alt + S)
2. 导航到 **Build, Execution, Deployment → Build Tools → Maven → Runner**
3. 找到 **Delegate IDE build/run actions to Maven** 选项
4. **取消勾选** 这个选项（非常重要！）
5. 点击 **Apply**

### 步骤 2：配置编译器设置

1. 在 Settings 中，导航到 **Build, Execution, Deployment → Compiler**
2. 确保以下设置：
   - ✅ **Build project automatically** (勾选)
   - ✅ **Compile independent modules in parallel** (勾选)
   - **Shared build process heap size (Mbytes)**: 设置为 `2048` 或更高
3. 点击 **Apply**

### 步骤 3：配置 Java 编译器

1. 在 Settings 中，导航到 **Build, Execution, Deployment → Compiler → Java Compiler**
2. 确保：
   - **Project bytecode version**: 17
   - **Per-module bytecode version**: 全部设置为 17
   - **Use compiler**: 选择 **Javac**
3. 点击 **Apply** 和 **OK**

### 步骤 4：清理并重新构建

1. 关闭所有打开的文件
2. 执行 **Build → Clean Project**
3. 等待清理完成
4. 执行 **File → Invalidate Caches / Restart**
5. 选择 **Invalidate and Restart**
6. IDEA 重启后，执行 **Build → Rebuild Project**

---

## 验证配置

### 检查编译输出

1. 打开 **View → Tool Windows → Build**
2. 重新构建项目
3. 查看构建输出，应该看到类似：
   ```
   Executing pre-compile tasks...
   Loading Ant configuration...
   Running Ant tasks...
   Compiling module 'medicine-cabinet'...
   ```
   而不是：
   ```
   [INFO] --- maven-compiler-plugin:3.11.0:compile
   ```

### 测试 Lombok

1. 打开 `Medicine.java`
2. 在类中输入 `medicine.get` 然后按 `Ctrl + Space`
3. 应该能看到 `getName()`, `getId()` 等方法的自动补全

---

## 如果问题仍然存在

### 方案 A：完全重置项目

1. 关闭 IDEA
2. 删除以下文件/文件夹：
   ```
   .idea/
   *.iml
   target/
   ```
3. 重新打开 IDEA
4. 选择 **File → Open**
5. 选择项目的 `pom.xml` 文件
6. 选择 **Open as Project**
7. 等待 Maven 导入完成
8. 按照上面的步骤重新配置

### 方案 B：使用命令行跳过编译问题

如果只是想运行项目而不关心测试：

```cmd
# 跳过测试直接运行
mvn spring-boot:run -DskipTests
```

或者在 IDEA 中：
1. 打开 **Run → Edit Configurations**
2. 点击 **+** → **Spring Boot**
3. 配置：
   - **Name**: Run Application
   - **Main class**: `com.family.medicinecabinet.MedicineCabinetApplication`
   - **Use classpath of module**: medicine-cabinet
4. 点击 **OK**
5. 点击运行按钮

---

## 关键配置总结

| 配置项 | 位置 | 设置值 |
|--------|------|--------|
| Delegate to Maven | Build Tools → Maven → Runner | ❌ 取消勾选 |
| Build automatically | Compiler | ✅ 勾选 |
| Enable annotation processing | Compiler → Annotation Processors | ✅ 勾选 |
| Project SDK | Project Structure → Project | Java 17 |
| Lombok Plugin | Plugins | ✅ 已安装 |

---

## 为什么这样做有效？

1. **IDEA 编译器** 直接集成了 Lombok 支持，不需要通过 Maven 插件
2. **Maven 命令行** 在 Windows 环境下对 Lombok 的支持不稳定
3. **取消委托给 Maven** 让 IDEA 使用自己的编译器，绕过 Maven 的问题
4. **IDEA 的增量编译** 更快，更适合开发环境

---

## 最终测试

配置完成后，尝试：

1. ✅ 打开任意 Java 文件，应该没有红色错误标记
2. ✅ 右键点击 `MedicineCabinetApplication` → Run，应该能启动
3. ✅ 右键点击测试类 → Run，应该能运行测试
4. ✅ 修改代码后自动编译，无需手动构建

如果以上都正常，说明配置成功！
