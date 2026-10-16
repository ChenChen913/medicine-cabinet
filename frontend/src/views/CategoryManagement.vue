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
                <a-button type="link" size="small">查看药品</a-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { categoryApi } from '@/api/category'
import type { Category } from '@/types'

const loading = ref(false)
const categories = ref<Category[]>([])
const modalVisible = ref(false)
const formData = reactive<Partial<Category>>({})

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
