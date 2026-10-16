# 家庭智慧药箱管理系统 - 前端

基于 **Vue 3 + TypeScript + ant-design-vue（非 Pro）+ Vite** 的前端工程，已实现全部页面文件。

## 快速开始

```bash
npm install          # 首次安装依赖
npm run dev          # 启动开发服务器 → http://localhost:3000
npm run build        # 生产构建（vue-tsc 类型检查 + vite build）
npm run preview      # 预览构建产物
```

## 技术栈（以 `package.json` 为准）

| 类别 | 依赖 |
|---|---|
| 框架 | vue `^3.4.0` |
| UI | ant-design-vue `^4.1.0`、@ant-design/icons-vue `^7.0.1` |
| 路由 | vue-router `^4.2.5` |
| HTTP | axios `^1.6.2` |
| 构建 | vite `^5.0.0`、typescript `^5.3.0`、vue-tsc `^3.1.8` |
| 工程化 | unplugin-vue-components（AntDesignVueResolver 按需自动导入组件）、unplugin-auto-import（vue / vue-router API 自动导入） |

> 说明：本项目**未使用** Pinia、图表库（ECharts 等）或 ant-design-vue 的 Pro 组件（ProTable / ProList 等）——`package.json` 中不存在相应依赖。

## 目录结构（实际现有文件）

```
frontend/
├── src/
│   ├── api/            # 接口封装：medicine.ts / shopping.ts / category.ts
│   ├── router/index.ts # 路由配置（见下表）
│   ├── types/index.ts  # TS 类型与枚举（大写：DosageForm / MedicineStatus / ShoppingReason / ShoppingStatus）
│   ├── utils/          # request.ts（Axios 封装）/ dict.ts（枚举中文映射）
│   ├── views/          # Dashboard.vue / MedicineList.vue / ShoppingList.vue / CategoryManagement.vue / NotFound.vue
│   ├── App.vue / main.ts / style.css
│   └── auto-imports.d.ts / vite-env.d.ts
├── index.html
├── package.json / tsconfig.json / vite.config.ts
└── README.md（本文件）
```

## 页面与路由（`src/router/index.ts`）

| 路由 | 页面 | 说明 |
|---|---|---|
| `/` | — | 重定向到 `/dashboard` |
| `/dashboard` | Dashboard.vue | 统计卡片（图表功能待实现） |
| `/medicines` | MedicineList.vue | 列表、增删改、记录用药（弹窗）、补货 |
| `/shopping` | ShoppingList.vue | 待购买清单、标记已购买、删除 |
| `/categories` | CategoryManagement.vue | 类别列表、添加类别、查看类别下药品 |
| `/:pathMatch(.*)*` | NotFound.vue | 404 兜底 |

## 开发代理

开发环境下 Vite 将 `/api` 请求代理到后端 `http://localhost:8080`（见 `vite.config.ts` 的 `server.proxy`），并监听 `3000` 端口。

## 实现状态说明

- 页面文件均已实际存在（不存在"待创建文件"清单）；旧版主文档的"文档化实现方案/待创建"描述已作废。
- 接口与页面的实际实现偏差（如未实现的图表、数据导出、类别编辑等）见 `.kiro/specs/family-medicine-cabinet/tasks.md` 的「实现偏差记录」。
- 历史前端实现文档已归档：`../docs/archive/FRONTEND_IMPLEMENTATION.md`（内容与现状有出入，仅供追溯）。

## 相关文档

- 项目入口：[../README.md](../README.md)
- 运行手册：[../docs/RUNBOOK.md](../docs/RUNBOOK.md)
