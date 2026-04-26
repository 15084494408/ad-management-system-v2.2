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
}
