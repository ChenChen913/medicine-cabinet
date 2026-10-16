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
    expiryDate: '',
    usageFrequencyScore: 0,
    status: 'NORMAL'
  })
  modalVisible.value = true
}

const showEditModal = (record: Medicine) => {
  modalTitle.value = '编辑药品'
  Object.assign(formData, record)
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
