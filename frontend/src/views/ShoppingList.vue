<template>
  <div>
    <a-card title="购物清单" :bordered="false">
      <a-list
        :loading="loading"
        :data-source="items"
        item-layout="horizontal"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <template #actions>
              <a-button type="primary" size="small" @click="showPurchaseModal(item)">
                标记已买
              </a-button>
              <a-popconfirm title="确定删除吗？" @confirm="handleDelete(item.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </template>
            <a-list-item-meta>
              <template #title>
                {{ item.medicineName }}
                <a-tag :color="getReasonColor(item.reason)" style="margin-left: 8px">
                  {{ getReasonText(item.reason) }}
                </a-tag>
              </template>
              <template #description>
                {{ item.customNote || '无备注' }}
                <br />
                添加时间: {{ item.addedAt }}
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-card>

    <!-- 标记已买弹窗：填写实际购买数量 -->
    <a-modal
      v-model:open="purchaseModalVisible"
      title="标记已买"
      @ok="handleMarkPurchased"
    >
      <a-form :label-col="{ span: 6 }">
        <a-form-item label="药品名称">
          {{ currentItem?.medicineName }}
        </a-form-item>
        <a-form-item label="购买数量" required>
          <a-input-number
            v-model:value="purchaseForm.quantity"
            :min="0.01"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { shoppingApi } from '@/api/shopping'
import type { ShoppingListItem } from '@/types'

const loading = ref(false)
const items = ref<ShoppingListItem[]>([])

const purchaseModalVisible = ref(false)
const currentItem = ref<ShoppingListItem | null>(null)
const purchaseForm = reactive({
  quantity: 1
})

const loadData = async () => {
  loading.value = true
  try {
    items.value = await shoppingApi.getPending()
  } catch (error) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const getReasonColor = (reason: string) => {
  const colors: Record<string, string> = {
    EXPIRED: 'red',
    DEPLETED: 'orange',
    MANUAL: 'blue'
  }
  return colors[reason] || 'default'
}

const getReasonText = (reason: string) => {
  const texts: Record<string, string> = {
    EXPIRED: '已过期',
    DEPLETED: '已耗尽',
    MANUAL: '手动添加'
  }
  return texts[reason] || reason
}

const showPurchaseModal = (item: ShoppingListItem) => {
  currentItem.value = item
  purchaseForm.quantity = 1
  purchaseModalVisible.value = true
}

// 后端 quantity 为必填参数（需 > 0），必须随请求传入
const handleMarkPurchased = async () => {
  if (!currentItem.value?.id) return
  try {
    await shoppingApi.markPurchased(currentItem.value.id, purchaseForm.quantity)
    message.success('标记成功')
    purchaseModalVisible.value = false
    loadData()
  } catch (error) {
    message.error('操作失败')
  }
}

const handleDelete = async (id: string) => {
  try {
    await shoppingApi.delete(id)
    message.success('删除成功')
    loadData()
  } catch (error) {
    message.error('删除失败')
  }
}

onMounted(() => {
  loadData()
})
</script>
