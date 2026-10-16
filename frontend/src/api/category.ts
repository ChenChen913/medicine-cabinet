import request from '@/utils/request'
import type { Category, Medicine } from '@/types'

export const categoryApi = {
  // 获取所有类别
  list(): Promise<Category[]> {
    return request.get<any, Category[]>('/categories')
  },

  // 创建类别
  create(data: Category): Promise<Category> {
    return request.post<any, Category>('/categories', data)
  },

  // 获取类别下的药品
  getMedicines(id: string): Promise<Medicine[]> {
    return request.get<any, Medicine[]>(`/categories/${id}/medicines`)
  },

  // 分配药品到类别（后端为 @RequestParam，必须走查询参数；
  // categoryIds 为 List<String>，用 indexes:null 序列化为重复参数 categoryIds=a&categoryIds=b）
  assign(data: { medicineId: string; categoryIds: string[] }): Promise<void> {
    return request.post<any, void>('/categories/assign', null, {
      params: data,
      paramsSerializer: { indexes: null }
    })
  }
}
