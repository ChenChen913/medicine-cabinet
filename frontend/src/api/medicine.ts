import request from '@/utils/request'
import type { Medicine, PageResult } from '@/types'

// 排序字段白名单（与后端 EMP-1 约定，逐字一致：驼峰、区分大小写，见 agreed-E2.md 第 1 条）
export type MedicineSortField = 'name' | 'totalQuantity' | 'expiryDate' | 'createdAt'

// 排序方向枚举（仅小写 asc/desc，见 agreed-E2.md 第 1 条）
export type MedicineSortOrder = 'asc' | 'desc'

export interface MedicineListParams {
  current: number
  size: number
  name?: string
  sortBy?: MedicineSortField
  sortOrder?: MedicineSortOrder
}

export const medicineApi = {
  // 获取药品列表
  // 约定：不传 sortBy 时后端不排序；size 合法区间 [1,100]，超限返回 body.code=400（见 agreed-E2.md 第 2/4 条）
  list(params: MedicineListParams): Promise<PageResult<Medicine>> {
    return request.get<any, PageResult<Medicine>>('/medicines', { params })
  },

  // 获取药品详情
  getById(id: string): Promise<Medicine> {
    return request.get<any, Medicine>(`/medicines/${id}`)
  },

  // 创建药品
  create(data: Medicine): Promise<Medicine> {
    return request.post<any, Medicine>('/medicines', data)
  },

  // 更新药品
  update(id: string, data: Medicine): Promise<Medicine> {
    return request.put<any, Medicine>(`/medicines/${id}`, data)
  },

  // 删除药品
  delete(id: string): Promise<void> {
    return request.delete<any, void>(`/medicines/${id}`)
  },

  // 消耗药品
  consume(id: string, params: { amount: number; userName: string; symptom?: string }): Promise<any> {
    return request.post(`/medicines/${id}/consume`, null, { params })
  },

  // 补货
  restock(id: string, params: { amount: number }): Promise<Medicine> {
    return request.post<any, Medicine>(`/medicines/${id}/restock`, null, { params })
  }
}
