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
    }
  ]
})

export default router
