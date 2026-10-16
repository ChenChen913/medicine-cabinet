<template>
  <a-card :bordered="false">
    <a-result
      status="404"
      title="404"
      :sub-title="`页面不存在：${route.fullPath}`"
    >
      <template #extra>
        <a-button type="primary" @click="router.push('/dashboard')">
          返回数据看板
        </a-button>
      </template>
    </a-result>
  </a-card>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
</script>
