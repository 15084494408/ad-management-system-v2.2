import request from '@/api/request'
import type { ApiResponse, PageResult, FinanceRecord } from '@/types'

export const financeApi = {
  /** 收支统计摘要 — 对齐后端 GET /finance/summary */
  getOverview(params?: { startDate?: string; endDate?: string }) {
    return request.get<any, ApiResponse>('/finance/summary', { params })
  },
  /** 财务流水列表 — 对齐后端 GET /finance/records */
  getRecords(params?: Record<string, any>) {
    return request.get<any, ApiResponse<PageResult<FinanceRecord>>>('/finance/records', { params })
  },
  /** 新增财务记录（快速记账） — 对齐后端 POST /finance/records */
  createRecord(data: Partial<FinanceRecord>) {
    return request.post<any, ApiResponse<number>>('/finance/records', data)
  },
  /** 删除财务记录 — 对齐后端 DELETE /finance/records/{id} */
  deleteRecord(id: number) {
    return request.delete<any, ApiResponse>(`/finance/records/${id}`)
  },
  /** 快捷记账列表（最近10条） — 对齐后端 GET /finance/quick-records */
  getQuickRecords() {
    return request.get<any, ApiResponse<FinanceRecord[]>>('/finance/quick-records')
  },
  /** 快速记账 — 对齐后端 POST /finance/records */
  createQuickRecord(data: Record<string, any>) {
    return request.post<any, ApiResponse<number>>('/finance/records', data)
  },
  /** 统一流水列表（聚合所有收支来源） — 对齐后端 GET /finance/all-flow */
  getAllFlow(params?: Record<string, any>) {
    return request.get<any, ApiResponse>('/finance/all-flow', { params })
  },

  // ── 设计师提成 ──
  /** 提成列表 */
  getCommissionList(params?: Record<string, any>) {
    return request.get<any, ApiResponse>('/finance/designer-commission', { params })
  },
  /** 提成汇总（按设计师分组） */
  getCommissionSummary(params?: { status?: number }) {
    return request.get<any, ApiResponse>('/finance/designer-commission/summary', { params })
  },
  /** 提成总览 */
  getCommissionOverview() {
    return request.get<any, ApiResponse>('/finance/designer-commission/overview')
  },
  /** 新建提成记录 */
  createCommission(data: Record<string, any>) {
    return request.post<any, ApiResponse<number>>('/finance/designer-commission', data)
  },
  /** 更新提成记录 */
  updateCommission(id: number, data: Record<string, any>) {
    return request.put<any, ApiResponse>(`/finance/designer-commission/${id}`, data)
  },
  /** 更新提成状态 */
  updateCommissionStatus(id: number, status: number) {
    return request.put<any, ApiResponse>(`/finance/designer-commission/${id}/status`, null, { params: { status } })
  },
  /** 删除提成记录 */
  deleteCommission(id: number) {
    return request.delete<any, ApiResponse>(`/finance/designer-commission/${id}`)
  },
}
