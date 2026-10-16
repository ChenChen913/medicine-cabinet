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
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import {
  DashboardOutlined,
  MedicineBoxOutlined,
  ShoppingCartOutlined,
  AppstoreOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const selectedKeys = ref<string[]>([])

const MENU_KEYS = ['/dashboard', '/medicines', '/shopping', '/categories']

// 高亮跟随路由：刷新、前进后退、直接访问子路径时都能高亮到正确菜单；
// 未匹配任何菜单的路径（如 404 页）不高亮。
watch(
  () => route.path,
  (path) => {
    const matched = MENU_KEYS.find(key => path === key || path.startsWith(`${key}/`))
    selectedKeys.value = matched ? [matched] : []
  },
  { immediate: true }
)

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
