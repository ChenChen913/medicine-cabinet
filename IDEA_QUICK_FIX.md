# IDEA 快速修复指南 - 解决 SortingService 编译错误

## 问题现状

✅ `SortingService.java` 文件已经修复（无 BOM 字符）
❌ IDEA 仍然报错 "找不到符号 SortingService"

**根本原因**：IDEA 正在使用 Maven 编译器，而 Maven 在 Windows + Java 17 环境下无法正确处理 Lombok。

---

## 立即修复步骤（5 分钟）

### 第 1 步：切换到 IDEA 编译器 ⭐ 最关键

1. 按 `Ctrl + Alt + S` 打开设置
2. 导航到：**Build, Execution, Deployment → Build Tools → Maven → Runner**
3. 找到 **Delegate IDE build/run actions to Maven**
4. **取消勾选** 这个选项（这是最关键的一步！）
5. 点击 **Apply**

![Maven Runner Settings](https://i.imgur.com/example.png)

### 第 2 步：启用注解处理器

1. 在设置中，导航到：**Build, Execution, Deployment → Compiler → Annotation Processors**
2. 确保勾选：**Enable annotation processing**
3. 点击 **Apply**

### 第 3 步：验证 Lombok 插件

1. 在设置中，导航到：**Plugins**
2. 搜索 "Lombok"
3. 确保 **Lombok** 插件已安装并启用
4. 如果未安装，点击 **Install** 并重启 IDEA

### 第 4 步：刷新并重新构建

1. 点击 **OK** 关闭设置
2. 在项目视图中，右键点击 `SortingService.java`
3. 选择 **Reload from Disk**（如果有这个选项）
4. 执行：**Build → Rebuild Project**
5. 等待构建完成（大约 30 秒）

---

## 验证修复

构建完成后，检查：

1. ✅ `CategoryServiceImpl.java` 中的 `SortingService` 导入不再报红
2. ✅ Build 窗口显示 "Build completed successfully"
3. ✅ 没有 "找不到符号" 错误

---

## 如果仍然失败

### 方案 A：完全清理缓存

1. 执行：**File → Invalidate Caches / Restart**
2. 选择：**Invalidate and Restart**
3. IDEA 重启后，再次执行：**Build → Rebuild Project**

### 方案 B：重新导入项目

1. 关闭 IDEA
2. 删除项目根目录下的 `.idea` 文件夹
3. 删除所有 `*.iml` 文件
4. 重新打开 IDEA
5. 选择 **File → Open**，选择 `pom.xml`
6. 选择 **Open as Project**
7. 等待 Maven 导入完成
8. 重复上面的第 1-4 步

---

## 为什么这样做有效？

| 问题 |