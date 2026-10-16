# 家庭智慧药箱管理系统 - 前端

## 📋 项目说明

本前端项目使用 Vue 3 + TypeScript + Ant Design Vue 开发。

由于会话长度限制，前端采用**文档化实现方案**。完整的实现文档请查看 `FRONTEND_IMPLEMENTATION.md`。

## 🚀 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:3000

### 3. 构建生产版本

```bash
npm run build
```

## 📁 项目结构

```
frontend/
├── public/              # 静态资源
├── src/
│   ├── api/            # API接口封装
│   ├── assets/         # 资源文件
│   ├── components/     # 公共组件
│   ├── router/         # 路由配置
│   ├── stores/         # Pinia状态管理
│   ├── types/          # TypeScript类型定义
│   ├── utils/          # 工具函数
│   ├── views/          # 页面组件
│   ├── App.vue         # 主应用组件
│   ├── main.ts         # 应用入口
│   └── style.css       # 全局样式
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── FRONTEND_IMPLEMENTATION.md  # 完整实现文档
```

## 📝 实现状态

### 已创建文件
- ✅ package.json - 依赖配置
- ✅ vite.config.ts - Vite配置
- ✅ tsconfig.json - TypeScript配置
- ✅ index.html - HTML入口
- ✅ src/main.ts - 应用入口

### 需要创建的文件
根据 `FRONTEND_IMPLEMENTATION.md` 文档创建以下文件：

#### 核心文件
- [ ] src/App.vue - 主应用组件
- [ ] src/style.css - 全局样式
- [ ] src/router/index.ts - 路由配置

#### 类型定义
- [ ] src/types/index.ts - TypeScript类型定义

#### 工具函数
- [ ] src/utils/request.ts - Axios封装
- [ ] src/utils/format.ts - 格式化工具

#### API接口
- [ ] src/api/medicine.ts - 药品API
- [ ] src/api/shopping.ts - 购物清单API
- [ ] src/api/category.ts - 类别API

#### 页面组件
- [ ] src/views/Dashboard.vue - 数据看板
- [ ] src/views/MedicineList.vue - 药品列表
- [ ] src/views/ShoppingList.vue - 购物清单
- [ ] src/views/CategoryManagement.vue - 类别管理

#### 公共组件
- [ ] src/components/MedicineCard.vue - 药品卡片
- [ ] src/components/ConsumeModal.vue - 用药记录弹窗

## 📖 实现指南

### 步骤1: 创建类型定义

参考 `FRONTEND_IMPLEMENTATION.md` 中的 "TypeScript类型定义" 部分，创建 `src/types/index.ts`。

### 步骤2: 创建Axios封装

参考文档中的 "Axios封装" 部分，创建 `src/utils/request.ts`。

### 步骤3: 创建API接口

参考文档中的 "API接口" 部分，创建 `src/api/` 目录下的文件。

### 步骤4: 创建路由配置

参考文档中的 "路由配置" 部分，创建 `src/router/index.ts`。

### 步骤5: 创建主应用组件

参考文档中的 "主应用组件" 部分，创建 `src/App.vue`。

### 步骤6: 创建页面组件

参考文档中的 "药品列表页面" 等部分，创建各个页面组件。

### 步骤7: 创建样式文件

参考文档中的 "样式文件" 部分，创建 `src/style.css`。

## 🔧 配置说明

### 代理配置

开发环境下，Vite会将 `/api` 请求代理到后端服务器 `http://localhost:8080`。

如需修改后端地址，编辑 `vite.config.ts`:

```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 修改为你的后端地址
      changeOrigin: true
    }
  }
}
```

### 自动导入配置

项目配置了自动导入：
- Vue API (ref, reactive, computed等)
- Vue Router API
- Pinia API
- Ant Design Vue 组件

无需手动导入即可使用。

## 📚 技术栈

- **框架**: Vue 3.4
- **语言**: TypeScript 5.3
- **UI库**: Ant Design Vue 4.1
- **路由**: Vue Router 4.2
- **状态管理**: Pinia 2.1
- **HTTP**: Axios 1.6
- **图表**: ECharts 5.4
- **构建**: Vite 5.0

## 🎨 UI组件

项目使用 Ant Design Vue，主要组件：

- **布局**: Layout, Menu, Header, Sider, Content
- **表格**: Table, ProTable
- **表单**: Form, Input, Select, DatePicker
- **反馈**: Message, Modal, Popconfirm
- **数据展示**: Card, Tag, Statistic, Descriptions
- **导航**: Menu, Breadcrumb
- **其他**: Button, Space, Divider

## 🔗 相关链接

- [Vue 3 文档](https://cn.vuejs.org/)
- [Ant Design Vue](https://antdv.com/)
- [Vite 文档](https://cn.vitejs.dev/)
- [TypeScript 文档](https://www.typescriptlang.org/)
- [Pinia 文档](https://pinia.vuejs.org/zh/)
- [Vue Router 文档](https://router.vuejs.org/zh/)

## 📞 获取帮助

如有问题，请查阅：
1. `FRONTEND_IMPLEMENTATION.md` - 完整实现文档
2. `../FINAL_PROJECT_REPORT.md` - 项目总报告
3. `../QUICK_START.md` - 快速启动指南

---

**开发愉快！** 🎉
