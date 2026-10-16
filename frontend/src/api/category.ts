import request from '@/utils/request'
import type { Category, Medicine } from '@/types'

export const categoryApi = {
  // 获取所有类别
  list(): Promise<Category[]> {
    return request.get('/categories')
  },

  // 创建类别
  create(data: Category): Promise<Category> {
    return request.post('/categories', data)
  },

  // 获取类别下的药品
  getMedicines(id: string): Promise<Medicine[]> {
    return request.get(`/categories/${id}/medicines`)
  },

  // 分配药品到类别
  assign(data: { medicineId: string; categoryIds: string[] }): Promise<void> {
    return request.post('/categories/assign', data)
  }
}
