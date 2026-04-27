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
  /** 获取账单明细列表（每日登记记录）— 对齐后端 GET /factory/bills/{id}/details */
  getBillDetails(id: number) {
    return request.get<any, ApiResponse<any[]>>(`/factory/bills/${id}/details`)
  },
  /** 新增账单明细（每日登记）— 对齐后端 POST /factory/bills/{id}/details */
  addBillDetail(id: number, data: any) {
    return request.post<any, ApiResponse<number>>(`/factory/bills/${id}/details`, data)
  },
  /** 批量新增账单明细 — 对齐后端 POST /factory/bills/{id}/details/batch */
  addBillDetailsBatch(id: number, dataList: any[]) {
    return request.post<any, ApiResponse<number[]>>(`/factory/bills/${id}/details/batch`, dataList)
  },
  /** 更新账单明细 — 对齐后端 PUT /factory/bills/{id}/details/{detailId} */
  updateBillDetail(billId: number, detailId: number, data: any) {
    return request.put<any, ApiResponse>(`/factory/bills/${billId}/details/${detailId}`, data)
  },
  /** 删除账单明细 — 对齐后端 DELETE /factory/bills/{id}/details/{detailId} */
  deleteBillDetail(billId: number, detailId: number) {
    return request.delete<any, ApiResponse>(`/factory/bills/${billId}/details/${detailId}`)
  },
  /** 更新账单实际付款金额（支持抹零）— 对齐后端 PUT /factory/bills/{id}/paid */
  updatePaidAmount(id: number, paidAmount: number) {
    return request.put<any, ApiResponse>(`/factory/bills/${id}/paid`, null, { params: { paidAmount } })
  },

  // ========== 业务员管理 ==========
  /** 业务员列表 — 对齐后端 GET /factory/salesman */
  getSalesmen(params?: Record<string, any>) {
    return request.get<any, ApiResponse<any[]>>('/factory/salesman', { params })
  },
  /** 业务员详情 — 对齐后端 GET /factory/salesman/{id} */
  getSalesmanDetail(id: number) {
    return request.get<any, ApiResponse<any>>(`/factory/salesman/${id}`)
  },
  /** 新增业务员 — 对齐后端 POST /factory/salesman */
  createSalesman(data: any) {
    return request.post<any, ApiResponse<number>>('/factory/salesman', data)
  },
  /** 更新业务员 — 对齐后端 PUT /factory/salesman/{id} */
  updateSalesman(id: number, data: any) {
    return request.put<any, ApiResponse>(`/factory/salesman/${id}`, data)
  },
  /** 删除业务员 — 对齐后端 DELETE /factory/salesman/{id} */
  deleteSalesman(id: number) {
    return request.delete<any, ApiResponse>(`/factory/salesman/${id}`)
  },
  /** 获取业务员下拉选项（按工厂筛选）— 对齐后端 GET /factory/salesman/options?factoryId=xxx */
  getSalesmanOptions(factoryId: number) {
    return request.get<any, ApiResponse<any[]>>('/factory/salesman/options', { params: { factoryId } })
  },
}
