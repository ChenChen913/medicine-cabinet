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
    },
    {
      // 兜底：未匹配的路径统一进 404 页，避免整页空白（必须放在最后）
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFound.vue')
    }
  ]
})

export default router
