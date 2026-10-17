<template>
  <div>
    <a-card title="药品列表" :bordered="false">
      <template #extra>
        <a-space>
          <a-input-search
            v-model:value="searchName"
            placeholder="搜索药品名称"
            style="width: 200px"
            @search="handleSearch"
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
          <template v-if="column.key === 'dosageForm'">
            {{ getDosageFormText(record.dosageForm) }}
          </template>
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
            <a-select-option :value="DosageForm.TABLET">片剂</a-select-option>
            <a-select-option :value="DosageForm.CAPSULE">胶囊</a-select-option>
            <a-select-option :value="DosageForm.GRANULE">颗粒</a-select-option>
            <a-select-option :value="DosageForm.LIQUID">液体</a-select-option>
            <a-select-option :value="DosageForm.EXTERNAL">外用</a-select-option>
            <a-select-option :value="DosageForm.INJECTION">注射剂</a-select-option>
            <a-select-option :value="DosageForm.OTHER">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="库存数量" required>
          <a-input-number v-model:value="formData.totalQuantity" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="单位" required>
          <a-input v-model:value="formData.unit" placeholder="粒/ml/包" />
        </a-form-item>
        <a-form-item label="预警阈值" required>
          <a-input-number v-model:value="formData.threshold" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="过期日期" required>
          <a-input v-model:value="formData.expiryDate" type="date" />
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
            style="width: 100%"
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
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TableColumnsType, TableProps } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { medicineApi, type MedicineListParams } from '@/api/medicine'
import { DosageForm, type Medicine } from '@/types'
import { getDosageFormText } from '@/utils/dict'

const loading = ref(false)
const medicines = ref<Medicine[]>([])
const searchName = ref('')
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  // 后端 size 上限 100（agreed-E2.md 第 4 条）：分页选项一律不得超上限
  pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条`
})

// antd 列 key → 后端 sortBy 白名单（agreed-E2.md 第 1/9 条：仅 name / totalQuantity / expiryDate）
const SORT_FIELD_MAP = {
  name: 'name',
  quantity: 'totalQuantity',
  expiryDate: 'expiryDate'
} as const

type SortColumnKey = keyof typeof SORT_FIELD_MAP
// 前端以 antd 语义保存排序状态，发请求时再映射为后端枚举
type SortState = { columnKey: SortColumnKey; order: 'ascend' | 'descend' }
const sortState = ref<SortState | null>(null)

// 受控排序指示器：sortOrder 绑定 sortState，400 回滚时列头指示能同步复原
const columns = computed<TableColumnsType>(() => [
  {
    title: '药品名称',
    dataIndex: 'name',
    key: 'name',
    sorter: true,
    sortOrder: sortState.value?.columnKey === 'name' ? sortState.value.order : null
  },
  { title: '剂型', dataIndex: 'dosageForm', key: 'dosageForm' },
  {
    title: '库存',
    key: 'quantity',
    sorter: true,
    sortOrder: sortState.value?.columnKey === 'quantity' ? sortState.value.order : null
  },
  { title: '预警阈值', dataIndex: 'threshold', key: 'threshold' },
  { title: '状态', key: 'status' },
  {
    title: '过期日期',
    dataIndex: 'expiryDate',
    key: 'expiryDate',
    sorter: true,
    sortOrder: sortState.value?.columnKey === 'expiryDate' ? sortState.value.order : null
  },
  { title: '操作', key: 'action', width: 300 }
])

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

// 组装查询参数：按白名单把列 key 映射为后端 sortBy，antd 方向映射为 asc/desc。
// 非法值在前端直接拦截、绝不发出（agreed-E2.md 第 3 条：后端对非法值返回 body.code=400，不静默回落）
const buildQuery = (): MedicineListParams => {
  const query: MedicineListParams = {
    current: pagination.current,
    size: pagination.pageSize
  }
  if (searchName.value) {
    query.name = searchName.value
  }
  if (sortState.value) {
    query.sortBy = SORT_FIELD_MAP[sortState.value.columnKey]
    query.sortOrder = sortState.value.order === 'ascend' ? 'asc' : 'desc'
  }
  return query
}

const loadData = async (allowPageFix = true): Promise<boolean> => {
  loading.value = true
  try {
    const res = await medicineApi.list(buildQuery())
    medicines.value = res.records
    pagination.total = res.total
    // 越界兜底：后端对越界 current 返回空 records 且不夹紧（agreed-E2.md 第 6 条），
    // 这里回跳最后一页重查（仅兜底一次），避免出现无提示的空表
    const pages = Math.max(1, Math.ceil(res.total / pagination.pageSize))
    if (allowPageFix && res.records.length === 0 && res.total > 0 && pagination.current > pages) {
      pagination.current = pages
      return await loadData(false)
    }
    return true
  } catch {
    // body.code!=200 与网络错误已由 request.ts 拦截器统一 toast，这里不重复提示
    return false
  } finally {
    loading.value = false
  }
}

// 搜索时重置到第一页再查询（保留当前排序：排序是查询条件的一部分）
const handleSearch = () => {
  pagination.current = 1
  loadData()
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
  // 重置表单，避免残留上一次编辑的字段（尤其 id 会导致新增误走更新分支）
  const staleKeys = Object.keys(formData) as Array<keyof Medicine>
  staleKeys.forEach((key) => {
    delete formData[key]
  })
  Object.assign(formData, {
    name: '',
    dosageForm: DosageForm.TABLET,
    totalQuantity: 0,
    unit: '粒',
    threshold: 10,
    expiryDate: '',
    symptomsTreated: '',
    dosageInstruction: ''
  })
  modalVisible.value = true
}

const showEditModal = (record: Medicine) => {
  modalTitle.value = '编辑药品'
  Object.assign(formData, record)
  modalVisible.value = true
}

// 构造白名单 payload：仅提交表单可编辑字段，
// status / usageFrequencyScore / createdAt / updatedAt 等后端字段一律不回传，避免覆盖后端值
const buildPayload = (): Partial<Medicine> => {
  const payload: Partial<Medicine> = {
    name: formData.name,
    dosageForm: formData.dosageForm,
    totalQuantity: formData.totalQuantity,
    unit: formData.unit,
    threshold: formData.threshold,
    expiryDate: formData.expiryDate,
    symptomsTreated: formData.symptomsTreated,
    dosageInstruction: formData.dosageInstruction
  }
  // 剔除空字符串字段（如未填写的 expiryDate: ''），避免后端 DATE NOT NULL 列踩坑
  const keys = Object.keys(payload) as Array<keyof Medicine>
  keys.forEach((key) => {
    const value = payload[key]
    if (value === '' || value === null || value === undefined) {
      delete payload[key]
    }
  })
  return payload
}

const handleSubmit = async () => {
  if (!formData.name) {
    message.warning('请填写药品名称')
    return
  }
  if (!formData.expiryDate) {
    message.warning('请选择过期日期')
    return
  }
  try {
    const payload = buildPayload()
    if (formData.id) {
      await medicineApi.update(formData.id, payload as Medicine)
      message.success('更新成功')
    } else {
      await medicineApi.create(payload as Medicine)
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

type TableChange = NonNullable<TableProps['onChange']>

// 白名单校验用 hasOwnProperty 而非 in：in 会命中 Object.prototype 上的键
const isSortColumnKey = (key: string): key is SortColumnKey =>
  Object.prototype.hasOwnProperty.call(SORT_FIELD_MAP, key)

// 从 change 事件中读取目标排序状态；白名单外的值一律视为无排序（防御性，绝不发非法参数）
const readSortFromEvent = (sorter: Parameters<TableChange>[2]): SortState | null => {
  const single = Array.isArray(sorter) ? sorter[0] : sorter
  const order = single?.order
  if (!order) return null
  const columnKey = String(single?.columnKey ?? single?.field ?? '')
  if (!isSortColumnKey(columnKey)) return null
  return { columnKey, order }
}

const isSameSort = (a: SortState | null, b: SortState | null) =>
  a?.columnKey === b?.columnKey && a?.order === b?.order

// 排序变化：回到第 1 页重查；若仍被后端判 400（理论上被前端白名单拦住），
// 回滚排序指示与页码并恢复上一可用状态（agreed-E2.md 第 3 条）
const applySortChange = async (next: SortState | null) => {
  const prevSort = sortState.value
  const prevCurrent = pagination.current
  sortState.value = next
  pagination.current = 1
  const ok = await loadData()
  if (!ok) {
    sortState.value = prevSort
    pagination.current = prevCurrent
    await loadData()
  }
}

// a-table 的 change 事件同时携带分页/筛选/排序三类变化，必须区分「是谁变了」：
// 翻页不能被误判成排序而重置页码，排序也不能被吞掉而漏发排序参数
const handleTableChange: TableChange = (pag, _filters, sorter) => {
  const nextSort = readSortFromEvent(sorter)
  if (!isSameSort(nextSort, sortState.value)) {
    applySortChange(nextSort)
    return
  }
  // 本次是分页变化（本项目无筛选列）：保留排序条件，仅更新页码/页大小
  pagination.current = pag.current ?? 1
  pagination.pageSize = pag.pageSize ?? pagination.pageSize
  loadData()
}

onMounted(() => {
  loadData()
})
</script>
