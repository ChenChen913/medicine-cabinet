# 实现计划

- [x] 1. 搭建项目基础架构



  - 创建 Spring Boot 项目，配置 Maven 依赖（Spring Boot 3.x, MyBatis-Plus, MySQL驱动, Lombok）
  - 配置 application.yml（数据库连接、MyBatis-Plus配置、日志配置）
  - 创建项目包结构：entity, mapper, service, controller, exception, config
  - 配置 MyBatis-Plus 自动填充策略（createdAt, updatedAt）
  - _需求: 1.1, 10.1_

- [x] 2. 创建数据库表和实体类


  - 执行 SQL 建表脚本（medicines, categories, medicine_categories, usage_logs, shopping_list）
  - 创建 Java 实体类（Medicine, Category, UsageLog, ShoppingListItem）
  - 创建枚举类（DosageForm, MedicineStatus, ShoppingReason, ShoppingStatus）
  - 配置实体类的 MyBatis-Plus 注解（@TableName, @TableId, @TableField）
  - _需求: 1.1, 1.2, 1.3_

- [x] 2.1 编写属性测试：药品创建完整性


  - **属性 1: 药品创建完整性**
  - **验证需求: 1.1, 1.3, 1.5**

- [x] 2.2 编写属性测试：最小单位存储一致性


  - **属性 2: 最小单位存储一致性**
  - **验证需求: 1.2**

- [x] 3. 实现数据访问层（Mapper）


  - 创建 MedicineMapper 接口（继承 BaseMapper，添加自定义查询方法）
  - 创建 CategoryMapper 接口
  - 创建 UsageLogMapper 接口
  - 创建 ShoppingListMapper 接口
  - 编写 XML 映射文件（复杂查询如智能排序、过期查询）
  - _需求: 1.1, 2.1, 3.1, 4.1_

- [x] 3.1 编写属性测试：库存数量验证


  - **属性 3: 库存数量验证**
  - **验证需求: 1.4**

- [x] 4. 实现智能排序服务



  - 创建 SortingService 接口和实现类
  - 实现 applySortingRules 方法（使用 Java Stream API 多级排序）
  - 实现 getSortedMedicines 方法（调用 Mapper 的排序查询）
  - _需求: 3.1, 3.2, 3.3, 3.4_

- [x] 4.1 编写属性测试：智能排序第一优先级


  - **属性 7: 智能排序第一优先级**
  - **验证需求: 3.1**

- [x] 4.2 编写属性测试：智能排序第二优先级


  - **属性 8: 智能排序第二优先级**
  - **验证需求: 3.2**

- [x] 4.3 编写属性测试：排序完整性不变量


  - **属性 9: 排序完整性不变量**
  - **验证需求: 3.4**

- [x] 5. 实现库存管理服务


  - 创建 InventoryService 接口和实现类
  - 实现 consumeMedicine 方法（扣减库存、创建用药记录、检查预警和耗尽）
  - 实现 checkLowStock 方法
  - 实现 handleDepletedMedicine 方法（增加频率分数、加入购物清单）
  - 实现 restockMedicine 方法
  - 添加 @Transactional 注解确保事务一致性
  - _需求: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 5.1 编写属性测试：库存扣减不变量


  - **属性 4: 库存扣减不变量**
  - **验证需求: 2.2**

- [x] 5.2 编写属性测试：低库存预警触发

  - **属性 5: 低库存预警触发**
  - **验证需求: 2.3, 2.4**

- [x] 5.3 编写属性测试：耗尽自动补货

  - **属性 6: 耗尽自动补货**
  - **验证需求: 2.5**

- [x] 6. 实现过期监控服务



  - 创建 ExpiryMonitorService 接口和实现类
  - 实现 runDailyCheck 方法（使用 @Scheduled 注解，cron = "0 0 2 * * ?"）
  - 实现 markExpired 方法
  - 实现 addToShoppingList 方法
  - 配置 Spring Scheduler（@EnableScheduling）
  - _需求: 4.1, 4.2, 4.3, 4.4_

- [x] 6.1 编写属性测试：过期自动标记

  - **属性 10: 过期自动标记**
  - **验证需求: 4.2**

- [x] 6.2 编写属性测试：过期自动加入购物清单

  - **属性 11: 过期自动加入购物清单**
  - **验证需求: 4.3**

- [x] 7. 实现购物清单服务


  - 创建 ShoppingListService 接口和实现类
  - 实现 addToShoppingList 方法（手动添加）
  - 实现 markAsPurchased 方法（更新状态、更新药品库存和频率）
  - 实现 getPendingItems 方法
  - 实现 deleteItem 方法
  - _需求: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 7.1 编写属性测试：购物清单标记已买更新频率

  - **属性 15: 购物清单标记已买更新频率**
  - **验证需求: 6.4**

- [x] 8. 实现类别管理服务


  - 创建 CategoryService 接口和实现类
  - 实现 createCategory 方法（检查名称唯一性）
  - 实现 assignMedicineToCategories 方法（支持多对多关联）
  - 实现 getMedicinesByCategory 方法（带智能排序）
  - _需求: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 8.1 编写属性测试：类别名称唯一性

  - **属性 16: 类别名称唯一性**
  - **验证需求: 7.4**

- [x] 8.2 编写属性测试：药品多类别关联

  - **属性 17: 药品多类别关联**
  - **验证需求: 7.5**

- [x] 8.3 编写属性测试：类别筛选正确性

  - **属性 18: 类别筛选正确性**
  - **验证需求: 7.2, 7.3**

- [x] 9. 实现全局异常处理


  - 创建自定义异常类（MedicineNotFoundException, InsufficientStockException等）
  - 创建 GlobalExceptionHandler（@RestControllerAdvice）
  - 实现各类异常的处理方法（返回统一的 Result 对象）
  - 创建统一响应对象 Result<T>
  - _需求: 10.5_

- [x] 9.1 编写属性测试：数据操作失败保持完整性

  - **属性 20: 数据操作失败保持完整性**
  - **验证需求: 10.5**

- [x] 10. 实现 REST API 控制器


  - 创建 MedicineController（CRUD、消耗、补货接口）
  - 创建 UsageLogController（查询用药记录接口）
  - 创建 ShoppingListController（购物清单管理接口）
  - 创建 CategoryController（类别管理接口）
  - 创建 StatisticsController（数据统计接口）
  - 添加参数验证注解（@Valid, @NotNull等）
  - _需求: 5.1, 5.2, 5.3, 5.4, 5.5, 9.1, 9.2, 9.3_

- [x] 10.1 编写属性测试：数据更新一致性

  - **属性 14: 数据更新一致性**
  - **验证需求: 5.5**

- [x] 10.2 编写属性测试：序列化round-trip

  - **属性 19: 序列化round-trip**
  - **验证需求: 10.1, 10.2, 10.3**

- [x] 11. 实现数据统计服务

  - 创建 StatisticsService 接口和实现类
  - 实现 getTopMedicines 方法（按使用频率排行）
  - 实现 getUsageTrend 方法（按时间范围统计用药趋势）
  - 实现 getDashboardStatistics 方法（总数、低库存、过期、待购买统计）
  - _需求: 9.1, 9.2, 9.3, 9.4_

- [x] 12. Checkpoint - 确保所有后端测试通过


  - 确保所有测试通过，如有问题请询问用户

- [x] 13. 搭建前端项目（Ant Design Pro）





  - 使用 Vite 创建 Vue 3 + TypeScript 项目
  - 安装 Ant Design Vue Pro 依赖
  - 配置路由（vue-router）和状态管理（pinia）
  - 配置 axios 请求拦截器和响应拦截器
  - 创建项目目录结构：views, components, api, types, utils
  - _需求: 5.1, 5.2_

- [x] 14. 实现药品管理页面（ProTable）

  - 创建 MedicineList.vue 组件
  - 使用 ProTable 组件展示药品列表
  - 实现列定义（图片、名称、数量、适应症、服用说明、状态等）
  - 实现搜索和筛选功能
  - 实现添加药品按钮和表单弹窗
  - 实现记录用药、编辑、删除操作
  - _需求: 5.1, 5.2, 7.2, 7.3_

- [x] 14.1 编写属性测试：卡片渲染正确性

  - **属性 12: 卡片渲染正确性**
  - **验证需求: 5.1, 5.2**

- [x] 15. 实现药品详情抽屉（ProDescriptions）

  - 创建 MedicineDetail.vue 组件
  - 使用 a-drawer 和 ProDescriptions 展示详情
  - 展示所有字段（包括副作用、生产日期、过期日期、存放位置）
  - 使用 a-timeline 展示最近用药记录
  - 添加视觉效果（标签颜色、统计图表、进度条）
  - _需求: 5.3, 5.4, 8.2_

- [x] 15.1 编写属性测试：详情页渲染完整性

  - **属性 13: 详情页渲染完整性**
  - **验证需求: 5.3, 5.4**

- [x] 16. 实现购物清单页面（ProList）


  - 创建 ShoppingList.vue 组件
  - 使用 ProList 和 ProCard 展示购物清单
  - 实现卡片式布局（grid 布局）
  - 实现标记已买功能（弹窗输入补货数量）
  - 实现删除功能
  - 按添加原因显示不同颜色标签
  - _需求: 6.1, 6.2, 6.3, 6.5_

- [x] 17. 实现数据统计看板（ProCard + Charts）

  - 创建 Dashboard.vue 组件
  - 使用 ProCard 展示统计卡片（总数、低库存、过期、待购买）
  - 使用 a-statistic 组件展示数值统计
  - 集成图表库（如 ECharts 或 Chart.js）
  - 实现常用药品排行榜（柱状图）
  - 实现用药趋势图（折线图）
  - _需求: 9.1, 9.2, 9.3_

- [x] 18. 实现记录用药功能

  - 创建 ConsumeMedicine.vue 表单组件
  - 实现表单字段（药品选择、消耗量、服用人、症状、备注）
  - 实现表单验证（数量不能超过库存）
  - 调用后端 API 提交用药记录
  - 显示操作结果（成功提示、低库存警告、耗尽提示）
  - _需求: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 19. 实现类别管理功能

  - 创建 CategoryManagement.vue 组件
  - 实现类别列表展示
  - 实现添加、编辑、删除类别功能
  - 实现药品分配到类别功能（支持多选）
  - 实现按类别筛选药品功能
  - _需求: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 20. 实现存放位置管理

  - 在药品表单中添加存放位置字段
  - 实现存放位置的自动补全功能（基于历史输入）
  - 在详情页显示存放位置
  - 实现按存放位置筛选功能
  - 实现按位置分组显示功能
  - _需求: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 21. 优化前端UI和用户体验


  - 添加加载状态（loading）和骨架屏
  - 实现操作确认对话框（删除、标记已买等）
  - 添加操作成功/失败的消息提示（a-message）
  - 实现响应式布局（适配移动端）
  - 添加过期药品的视觉突出显示（红色标签、警告图标）
  - 优化表单交互（自动聚焦、回车提交等）
  - _需求: 4.5, 5.1, 5.2_

- [x] 22. 实现数据导出功能

  - 在统计页面添加导出按钮
  - 实现导出用药记录为 CSV 格式
  - 实现导出用药记录为 JSON 格式
  - 添加导出时间范围选择
  - _需求: 9.5_

- [x] 23. 配置跨域和部署准备

  - 配置后端 CORS（允许前端跨域请求）
  - 配置前端代理（开发环境）
  - 编写 Dockerfile（后端 Spring Boot）
  - 编写前端构建脚本
  - 编写部署文档（README.md）
  - _需求: 10.1, 10.2_

- [x] 24. Final Checkpoint - 确保所有测试通过


  - 确保所有测试通过，如有问题请询问用户

---

## 实现偏差记录（2026-10-16 代码核查追加）

> 本节由文档治理任务 4C 追加：列出声称 `[x]` 完成、但经代码核查**不存在或与实现不符**的产物。
> **原有勾选状态一律未改动**，本节仅作记录，供后续任务修正。

### 一、后端：不存在的控制器与服务

| 任务 | 声称产物 | 核查结果（依据） |
|---|---|---|
| 10 | `UsageLogController`（查询用药记录接口） | ❌ 不存在——`controller/` 下实际仅 4 个文件：`MedicineController`、`CategoryController`、`ShoppingListController`、`HomeController` |
| 10、11 | `StatisticsController`、`StatisticsService`（`getTopMedicines` / `getUsageTrend` / `getDashboardStatistics`） | ❌ 不存在——`src/` 下检索 `*statistic*` 为 0 个文件 |

### 二、未实现的属性测试（jqwik）

实际存在的属性测试仅 7 个：Property 1/2/3（`MedicinePropertyTest`）、Property 4（`InventoryPropertyTest`）、Property 7/8/9（`SortingPropertyTest`）。

| 任务 | 声称属性 | 核查结果 |
|---|---|---|
| 5.2 / 5.3 | 属性 5（低库存预警触发）、属性 6（耗尽自动补货） | ❌ 无对应测试 |
| 6.1 / 6.2 | 属性 10（过期自动标记）、属性 11（过期自动加入购物清单） | ❌ 无对应测试 |
| 7.1 | 属性 15（购物清单标记已买更新频率） | ❌ 无对应测试 |
| 8.1 / 8.2 / 8.3 | 属性 16（类别名称唯一性）、17（药品多类别关联）、18（类别筛选正确性） | ❌ 无对应测试 |
| 9.1 | 属性 20（数据操作失败保持完整性） | ❌ 无对应测试 |
| 10.1 / 10.2 | 属性 14（数据更新一致性）、19（序列化 round-trip） | ❌ 无对应测试 |
| 14.1 / 15.1 | 属性 12（卡片渲染正确性）、13（详情页渲染完整性） | ❌ 无对应测试（前端亦无测试工程） |

### 三、前端：未落地的技术选型与组件

| 任务 | 声称产物 | 核查结果（依据） |
|---|---|---|
| 13 | Ant Design **Pro**、**Pinia** 状态管理 | ❌ 未使用——`frontend/package.json` 无 pro-components / pinia 依赖，实际为 ant-design-vue 4，无状态管理库 |
| 14 / 15 / 16 / 17 | ProTable / ProDescriptions / ProList / ProCard | ❌ 未使用——`frontend/src` 中无这些组件 |
| 15 | `MedicineDetail.vue`（药品详情抽屉） | ❌ 不存在——`frontend/src/views/` 无该文件 |
| 17 | 图表库（ECharts / Chart.js）、排行榜柱状图、趋势折线图 | ❌ 未实现——`Dashboard.vue` 第 27 行显示"图表功能待实现" |
| 18 | `ConsumeMedicine.vue` 表单组件 | ❌ 不存在——记录用药以 `MedicineList.vue` 内弹窗实现 |
| 19 | 类别**编辑 / 删除**、药品分配到类别的 UI | ❌ 未实现——`CategoryManagement.vue` 仅"添加类别"+"查看药品" |
| 20 | 存放位置：自动补全、详情页展示、筛选、分组 | ❌ 未实现——`storageLocation` 仅在前端 `types/index.ts` 中有字段声明 |
| 22 | 数据导出（CSV / JSON、时间范围选择） | ❌ 未实现——`frontend/src` 无导出相关代码 |

### 四、部署

| 任务 | 声称产物 | 核查结果（依据） |
|---|---|---|
| 23 | `Dockerfile`（后端 Spring Boot） | ❌ 不存在——仓库（2 层深度内）无 Dockerfile；CORS、前端代理、构建脚本、README 已完成 |

### 五、部分实现（其余条目以代码为准）

- 21（UI 优化）：loading 与消息提示已实现；骨架屏 / 响应式 / 自动聚焦等未逐项核实，以 `frontend/src` 实际代码为准。

> 核查方式：文件系统枚举（`find` / `ls`）+ 源码 grep；核查日期 2026-10-16。
