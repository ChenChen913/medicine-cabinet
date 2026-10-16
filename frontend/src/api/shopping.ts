import request from '@/utils/request'
import type { ShoppingListItem } from '@/types'

export const shoppingApi = {
  // 获取待购买清单
  getPending(): Promise<ShoppingListItem[]> {
    return request.get('/shopping-list/pending')
  },

  // 添加到购物清单
  add(data: { medicineId: string; reason: string; customNote?: string }): Promise<ShoppingListItem> {
    return request.post('/shopping-list', data)
  },

  // 标记为已购买
  markPurchased(id: string): Promise<void> {
    return request.post(`/shopping-list/${id}/purchased`)
  },

  // 删除清单项
  delete(id: string): Promise<void> {
    return request.delete(`/shopping-list/${id}`)
  }
}
