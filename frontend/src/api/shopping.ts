import request from '@/utils/request'
import type { ShoppingListItem } from '@/types'

export const shoppingApi = {
  // 获取待购买清单
  getPending(): Promise<ShoppingListItem[]> {
    return request.get<any, ShoppingListItem[]>('/shopping-list/pending')
  },

  // 添加到购物清单（后端为 @RequestParam，必须走查询参数）
  add(data: { medicineId: string; reason: string; customNote?: string }): Promise<ShoppingListItem> {
    return request.post<any, ShoppingListItem>('/shopping-list', null, { params: data })
  },

  // 标记为已购买（后端 quantity 为必填 @RequestParam）
  markPurchased(id: string, quantity: number): Promise<void> {
    return request.post<any, void>(`/shopping-list/${id}/purchased`, null, { params: { quantity } })
  },

  // 删除清单项
  delete(id: string): Promise<void> {
    return request.delete<any, void>(`/shopping-list/${id}`)
  }
}
