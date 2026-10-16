<template>
  <div>
    <a-row :gutter="16">
      <a-col :span="6">
        <a-card>
          <a-statistic title="药品总数" :value="stats.total" suffix="种" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="低库存" :value="stats.lowStock" suffix="种" value-style="color: #faad14" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="已过期" :value="stats.expired" suffix="种" value-style="color: #f5222d" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="待购买" :value="stats.pending" suffix="项" value-style="color: #1890ff" />
        </a-card>
      </a-col>
    </a-row>

    <a-card title="数据看板" style="margin-top: 16px">
      <a-empty description="图表功能待实现" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { medicineApi } from '@/api/medicine'
import { shoppingApi } from '@/api/shopping'

const stats = ref({
  total: 0,
  lowStock: 0,
  expired: 0,
  pending: 0
})

const loadStats = async () => {
  try {
    const medicines = await medicineApi.list({ current: 1, size: 1000 })
    stats.value.total = medicines.total
    stats.value.lowStock = medicines.records.filter(m => m.status === 'LOW_STOCK').length
    stats.value.expired = medicines.records.filter(m => m.status === 'EXPIRED').length
    
    const shopping = await shoppingApi.getPending()
    stats.value.pending = shopping.length
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

onMounted(() => {
  loadStats()
})
</script>
