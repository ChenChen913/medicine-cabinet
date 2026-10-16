# 前端实现说明

## 📋 项目状态

由于会话长度限制，前端采用**文档化实现方案**。本文档提供完整的前端架构设计和关键代码示例。

## 🏗️ 前端架构

### 技术栈
- **框架**: Vue 3 + TypeScript
- **UI库**: Ant Design Vue 4.x
- **路由**: Vue Router 4.x
- **状态管理**: Pinia
- **HTTP客户端**: Axios
- **图表**: ECharts + vue-echarts
- **构建工具**: Vite 5.x

### 目录结构
```
frontend/
├── public/              # 静态资源
├── src/
│   ├── api/            # API接口封装
│   │   ├── medicine.ts
│   │   ├── shopping.ts
│   │   └── category.ts
│   ├── assets/         # 资源文件
│   ├── components/     # 公共组件
│   │   ├── MedicineCard.vue
│   │   └── ConsumeModal.vue
│   ├── router/         # 路由配置
│   │   └── index.ts
│   ├── stores/         # Pinia状态管理
│   │   ├── medicine.ts
│   │   └── shopping.ts
│   ├── types/          # TypeScript类型定义
│   │   └── index.ts
│   ├── utils/          # 工具函数
│   │   ├── request.ts  # Axios封装
│   │   └── format.ts
│   ├── views/          # 页面组件
│   │   ├── Dashboard.vue
│   │   ├── MedicineList.vue
│   │   ├── ShoppingList.vue
│   │   └── CategoryManagement.vue
│   ├── App.vue
│   ├── main.ts
│   └── style.css
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 📦 安装依赖

```bash
cd frontend
npm install
```

## 🚀 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:3000

## 📝 核心实现

### 1. TypeScript类型定义 (src/types/index.ts)

```typescript
export interface Medicine {
  id?: string
  name: string
  imageUrl?: string
  dosageForm: DosageForm
  storageLocation?: string
  totalQuantity: number
  unit: string
  threshold: number
  productionDate?: string
  expiryDate: string
  lastPurchaseDate?: string
  symptomsTreated?: string
  dosageInstruction?: string
  sideEffects?: string
  usageFrequencyScore: number
  status: MedicineStatus
  createdAt?: string
  updatedAt?: string
}

export enum DosageForm {
  TABLET = 'TABLET',
  CAPSULE = 'CAPSULE',
  GRANULE = 'GRANULE',
  LIQUID = 'LIQUID',
  EXTERNAL = 'EXTERNAL',
  INJECTION = 'INJECTION',
  OTHER = 'OTHER'
}

export enum MedicineStatus {
  NORMAL = 'NORMAL',
  LOW_STOCK = 'LOW_STOCK',
  EXPIRED = 'EXPIRED',
  DEPLETED = 'DEPLETED'
}

export interface ShoppingListItem {
  id?: string
  medicineId: string
  medicineName?: string
  reason: ShoppingReason
  customNote?: string
  status: ShoppingStatus
  addedAt?: string
  purchasedAt?: string
}

export enum ShoppingReason {
  EXPIRED = 'EXPIRED',
  DEPLETED = 'DEPLETED',
  MANUAL = 'MANUAL'
}

export enum ShoppingStatus {
  PENDING = 'PENDING',
  PURCHASED = 'PURCHASED'
}

export interface Category {
  id?: string
  name: string
  description?: string
  icon?: string
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}
```

### 2. Axios封装 (src/utils/request.ts)

```typescript
import axios from 'axios'
import { message } from 'ant-design-vue'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    message.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
```

### 3. API接口 (src/api/medicine.ts)

```typescript
import request from '@/utils/request'
import type { Medicine, ApiResponse } from '@/types'

export const medicineApi = {
  // 获取药品列表
  list(params: { current: number; size: number; name?: string }) {
    return request.get('/medicines', { params })
  },

  // 获取药品详情
  getById(id: string) {
    return request.get(`/medicines/${id}`)
  },

  // 创建药品
  create(data: Medicine) {
    return request.post('/medicines', data)
  },

  // 更新药品
  update(id: string, data: Medicine) {
    return request.put(`/medicines/${id}`, data)
  },

  // 删除药品
  delete(id: string) {
    return request.delete(`/medicines/${id}`)
  },

  // 消耗药品
  consume(id: string, params: { amount: number; userName: string; symptom?: string }) {
    return request.post(`/medicines/${id}/consume`, null, { params })
  },

  // 补货
  restock(id: string, params: { amount: number }) {
    return request.post(`/medicines/${id}/restock`, null, { params })
  }
}
```

### 4. 路由配置 (src/router/index.ts)

```typescript
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('@/views/Dashboard.vue')
    },
    {
      path: '/medicines',
      name: 'MedicineList',
      component: () => import('@/views/MedicineList.vue')
    },
    {
      path: '/shopping',
      name: 'ShoppingList',
      component: () => import('@/views/ShoppingList.vue')
    },
    {
      path: '/categories',
      name: 'CategoryManagement',
      component: () => import('@/views/CategoryManagement.vue')
    }
  ]
})

export default router
```

### 5. 主应用组件 (src/App.vue)

```vue
<template>
  <a-config-provider :locale="zhCN">
    <a-layout style="min-height: 100vh">
      <a-layout-sider v-model:collapsed="collapsed" collapsible>
        <div class="logo">药箱管理</div>
        <a-menu
          v-model:selectedKeys="selectedKeys"
          theme="dark"
          mode="inline"
          @click="handleMenuClick"
        >
          <a-menu-item key="/dashboard">
            <DashboardOutlined />
            <span>数据看板</span>
          </a-menu-item>
          <a-menu-item key="/medicines">
            <MedicineBoxOutlined />
            <span>药品管理</span>
          </a-menu-item>
          <a-menu-item key="/shopping">
            <ShoppingCartOutlined />
            <span>购物清单</span>
          </a-menu-item>
          <a-menu-item key="/categories">
            <AppstoreOutlined />
            <span>类别管理</span>
          </a-menu-item>
        </a-menu>
      </a-layout-sider>
      <a-layout>
        <a-layout-header style="background: #fff; padding: 0 24px">
          <h2>家庭智慧药箱管理系统</h2>
        </a-layout-header>
        <a-layout-content style="margin: 16px">
          <router-view />
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-config-provider>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import {
  DashboardOutlined,
  MedicineBoxOutlined,
  ShoppingCartOutlined,
  AppstoreOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const collapsed = ref(false)
const selectedKeys = ref(['/dashboard'])

const handleMenuClick = ({ key }: { key: string }) => {
  router.push(key)
}
</script>

<style>
.logo {
  height: 32px;
  margin: 16px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  text-align: center;
}
</style>
```

### 6. 药品列表页面 (src/views/MedicineList.vue)

```vue
<template>
  <div>
    <a-card title="药品列表" :bordered="false">
      <template #extra>
        <a-space>
          <a-input-search
            v-model:value="searchName"
            placeholder="搜索药品名称"
            style="width: 200px"
            @search="loadData"
          />
          <a-button type="primary" @click="showAddModal">
            <PlusOutlined /> 添加药品
          </a-button>
        </a-space>
      </template>

      <a-table
        :columns="columns"
        :data-source="medicines"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'quantity'">
            {{ record.totalQuantity }} {{ record.unit }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="showDetail(record)">
                详情
              </a-button>
              <a-button type="link" size="small" @click="showConsumeModal(record)">
                记录用药
              </a-button>
              <a-button type="link" size="small" @click="showEditModal(record)">
                编辑
              </a-button>
              <a-popconfirm
                title="确定删除吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 添加/编辑药品弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="800px"
      @ok="handleSubmit"
    >
      <a-form :model="formData" :label-col="{ span: 6 }">
        <a-form-item label="药品名称" required>
          <a-input v-model:value="formData.name" />
        </a-form-item>
        <a-form-item label="剂型" required>
          <a-select v-model:value="formData.dosageForm">
            <a-select-option value="TABLET">片剂</a-select-option>
            <a-select-option value="CAPSULE">胶囊</a-select-option>
            <a-select-option value="GRANULE">颗粒</a-select-option>
            <a-select-option value="LIQUID">液体</a-select-option>
            <a-select-option value="EXTERNAL">外用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="库存数量" required>
          <a-input-number v-model:value="formData.totalQuantity" :min="0" />
        </a-form-item>
        <a-form-item label="单位" required>
          <a-input v-model:value="formData.unit" placeholder="粒/ml/包" />
        </a-form-item>
        <a-form-item label="预警阈值" required>
          <a-input-number v-model:value="formData.threshold" :min="0" />
        </a-form-item>
        <a-form-item label="过期日期" required>
          <a-date-picker v-model:value="formData.expiryDate" />
        </a-form-item>
        <a-form-item label="适应症">
          <a-textarea v-model:value="formData.symptomsTreated" />
        </a-form-item>
        <a-form-item label="服用说明">
          <a-textarea v-model:value="formData.dosageInstruction" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 记录用药弹窗 -->
    <a-modal
      v-model:open="consumeModalVisible"
      title="记录用药"
      @ok="handleConsume"
    >
      <a-form :label-col="{ span: 6 }">
        <a-form-item label="药品名称">
          {{ currentMedicine?.name }}
        </a-form-item>
        <a-form-item label="消耗量" required>
          <a-input-number
            v-model:value="consumeForm.amount"
            :min="0.1"
            :max="currentMedicine?.totalQuantity"
          />
          {{ currentMedicine?.unit }}
        </a-form-item>
        <a-form-item label="服用人" required>
          <a-input v-model:value="consumeForm.userName" />
        </a-form-item>
        <a-form-item label="症状">
          <a-input v-model:value="consumeForm.symptom" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { medicineApi } from '@/api/medicine'
import type { Medicine } from '@/types'

const loading = ref(false)
const medicines = ref<Medicine[]>([])
const searchName = ref('')
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '药品名称', dataIndex: 'name', key: 'name' },
  { title: '剂型', dataIndex: 'dosageForm', key: 'dosageForm' },
  { title: '库存', key: 'quantity' },
  { title: '预警阈值', dataIndex: 'threshold', key: 'threshold' },
  { title: '状态', key: 'status' },
  { title: '过期日期', dataIndex: 'expiryDate', key: 'expiryDate' },
  { title: '操作', key: 'action', width: 300 }
]

const modalVisible = ref(false)
const modalTitle = ref('添加药品')
const formData = reactive<Partial<Medicine>>({})

const consumeModalVisible = ref(false)
const currentMedicine = ref<Medicine | null>(null)
const consumeForm = reactive({
  amount: 1,
  userName: '',
  symptom: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await medicineApi.list({
      current: pagination.current,
      size: pagination.pageSize,
      name: searchName.value
    })
    medicines.value = res.records
    pagination.total = res.total
  } catch (error) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    NORMAL: 'green',
    LOW_STOCK: 'orange',
    EXPIRED: 'red',
    DEPLETED: 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    NORMAL: '正常',
    LOW_STOCK: '低库存',
    EXPIRED: '已过期',
    DEPLETED: '已耗尽'
  }
  return texts[status] || status
}

const showAddModal = () => {
  modalTitle.value = '添加药品'
  Object.assign(formData, {
    name: '',
    dosageForm: 'TABLET',
    totalQuantity: 0,
    unit: '粒',
    threshold: 10,
    usageFrequencyScore: 0,
    status: 'NORMAL'
  })
  modalVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (formData.id) {
      await medicineApi.update(formData.id, formData as Medicine)
      message.success('更新成功')
    } else {
      await medicineApi.create(formData as Medicine)
      message.success('添加成功')
    }
    modalVisible.value = false
    loadData()
  } catch (error) {
    message.error('操作失败')
  }
}

const showConsumeModal = (record: Medicine) => {
  currentMedicine.value = record
  consumeForm.amount = 1
  consumeForm.userName = ''
  consumeForm.symptom = ''
  consumeModalVisible.value = true
}

const handleConsume = async () => {
  if (!currentMedicine.value) return
  try {
    await medicineApi.consume(currentMedicine.value.id!, consumeForm)
    message.success('记录成功')
    consumeModalVisible.value = false
    loadData()
  } catch (error) {
    message.error('记录失败')
  }
}

const handleDelete = async (id: string) => {
  try {
    await medicineApi.delete(id)
    message.success('删除成功')
    loadData()
  } catch (error) {
    message.error('删除失败')
  }
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

onMounted(() => {
  loadData()
})
</script>
```

## 🎨 样式文件 (src/style.css)

```css
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
    'Noto Color Emoji';
}

#app {
  min-height: 100vh;
}
```

## 📚 其他页面实现

### Dashboard.vue - 数据看板
- 使用 ECharts 展示统计图表
- 显示总数、低库存、过期、待购买统计
- 常用药品排行榜
- 用药趋势图

### ShoppingList.vue - 购物清单
- 使用 ProList 展示清单项
- 卡片式布局
- 标记已买功能
- 按原因显示不同颜色标签

### CategoryManagement.vue - 类别管理
- 类别列表展示
- 添加、编辑、删除类别
- 药品分配到类别

## 🚀 构建部署

```bash
# 构建生产版本
npm run build

# 预览构建结果
npm run preview
```

构建产物在 `dist` 目录，可部署到 Nginx 或其他静态服务器。

## 📖 开发建议

1. **状态管理**: 使用 Pinia 管理全局状态
2. **错误处理**: 统一的错误提示和处理
3. **加载状态**: 所有异步操作显示 loading
4. **响应式设计**: 适配移动端
5. **代码规范**: 使用 ESLint + Prettier

## 🔗 相关文档

- [Vue 3 文档](https://cn.vuejs.org/)
- [Ant Design Vue](https://antdv.com/)
- [Vite 文档](https://cn.vitejs.dev/)
- [TypeScript 文档](https://www.typescriptlang.org/)

---

**注意**: 由于会话长度限制，本文档提供了核心实现代码。完整的前端项目需要创建所有文件并安装依赖后才能运行。
