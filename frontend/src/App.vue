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
