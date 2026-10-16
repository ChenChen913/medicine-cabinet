<template>
  <div>
    <a-card title="类别管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="showAddModal">
          <PlusOutlined /> 添加类别
        </a-button>
      </template>

      <a-list
        :loading="loading"
        :data-source="categories"
        :grid="{ gutter: 16, column: 4 }"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-card :title="item.name">
              <p>{{ item.description || '无描述' }}</p>
              <template #actions>
                <a-button type="link" size="small" @click="showMedicines(item)">
                  查看药品
                </a-button>
              </template>
            </a-card>
          </a-list-item>
        </template>
      </a-list>
    </a-card>

    <a-modal v-model:open="modalVisible" title="添加类别" @ok="handleSubmit">
      <a-form :model="formData" :label-col="{ span: 6 }">
        <a-form-item label="类别名称" required>
          <a-input v-model:value="formData.name" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="formData.description" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 查看该分类下的药品 -->
    <a-drawer v-model:open="drawerVisible" :title="drawerTitle" width="480">
      <a-list
        :loading="medicinesLoading"
        :data-source="categoryMedicines"
        :locale="{ emptyText: '该分类下暂无药品' }"
        size="small"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>{{ item.name }}</template>
              <template #description>
                {{ getDosageFormText(item.dosageForm) }} · 库存
                {{ item.totalQuantity }} {{ item.unit }}
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { categoryApi } from '@/api/category'
import type { Category, Medicine } from '@/types'
import { getDosageFormText } from '@/utils/dict'

const loading = ref(false)
const categories = ref<Category[]>([])
const modalVisible = ref(false)
const formData = reactive<Partial<Category>>({})

const drawerVisible = ref(false)
const currentCategory = ref<Category | null>(null)
const categoryMedicines = ref<Medicine[]>([])
const medicinesLoading = ref(false)

const drawerTitle = computed(() =>
  currentCategory.value ? `${currentCategory.value.name} 下的药品` : '分类药品'
)

const loadData = async () => {
  loading.value = true
  try {
    categories.value = await categoryApi.list()
  } catch (error) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const showAddModal = () => {
  Object.assign(formData, { name: '', description: '' })
  modalVisible.value = true
}

// 打开抽屉并拉取该分类下的药品（后端按用药频次智能排序）
const showMedicines = async (category: Category) => {
  currentCategory.value = category
  categoryMedicines.value = []
  drawerVisible.value = true
  if (!category.id) return
  medicinesLoading.value = true
  try {
    const list = await categoryApi.getMedicines(category.id)
    // 快速连点分类时丢弃过期响应
    if (currentCategory.value?.id === category.id) {
      categoryMedicines.value = list
    }
  } catch (error) {
    message.error('加载药品失败')
  } finally {
    medicinesLoading.value = false
  }
}

const handleSubmit = async () => {
  try {
    await categoryApi.create(formData as Category)
    message.success('添加成功')
    modalVisible.value = false
    loadData()
  } catch (error) {
    message.error('操作失败')
  }
}

onMounted(() => {
  loadData()
})
</script>
