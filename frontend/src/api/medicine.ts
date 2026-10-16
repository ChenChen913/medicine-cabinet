import request from '@/utils/request'
import type { Medicine, PageResult } from '@/types'

export const medicineApi = {
  // 获取药品列表
  list(params: { current: number; size: number; name?: string }): Promise<PageResult<Medicine>> {
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
