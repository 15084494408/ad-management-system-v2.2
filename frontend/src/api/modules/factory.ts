import request from '@/api/request'
import type { ApiResponse, PageResult, FactoryBill, Factory } from '@/types'

export const factoryApi = {
  /** 工厂列表 — 对齐后端 GET /factory/list */
  getFactories() {
    return request.get<any, ApiResponse<Factory[]>>('/factory/list')
  },
  /** 工厂账单列表 — 对齐后端 GET /factory/bills */
  getBills(params?: Record<string, any>) {
    return request.get<any, ApiResponse<PageResult<FactoryBill>>>('/factory/bills', { params })
  },
  /** 账单详情 — 对齐后端 GET /factory/bills/{id} */
  getBillDetail(id: number) {
    return request.get<any, ApiResponse<FactoryBill>>(`/factory/bills/${id}`)
  },
  /** 新建账单 — 对齐后端 POST /factory/bills */
  createBill(data: Partial<FactoryBill>) {
    return request.post<any, ApiResponse<number>>('/factory/bills', data)
  },
  /** 更新账单 — 对齐后端 PUT /factory/bills/{id} */
  updateBill(id: number, data: Partial<FactoryBill>) {
    return request.put<any, ApiResponse>(`/factory/bills/${id}`, data)
  },
  /** 更新账单状态（含对账/结算） — 对齐后端 PUT /factory/bills/{id}/status */
  changeBillStatus(id: number, status: number) {
    return request.put<any, ApiResponse>(`/factory/bills/${id}/status`, null, { params: { status } })
  },
  /** 删除账单 — 对齐后端 DELETE /factory/bills/{id} */
  deleteBill(id: number) {
    return request.delete<any, ApiResponse>(`/factory/bills/${id}`)
  },
  /** 对账 — 对齐后端 PUT /factory/bills/{id}/status (status=2) */
  reconcile(id: number) {
    return request.put<any, ApiResponse>(`/factory/bills/${id}/status`, null, { params: { status: 2 } })
  },
  /** 结算 — 对齐后端 PUT /factory/bills/{id}/status (status=3) */
  settle(id: number, data: { amount: number; method?: string }) {
    return request.put<any, ApiResponse>(`/factory/bills/${id}/status`, null, { params: { status: 3 } })
  },
}
