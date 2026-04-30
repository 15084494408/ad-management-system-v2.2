import request from '@/api/request'
import type { ApiResponse, PageResult, Customer } from '@/types'

export const customerApi = {
  /** 客户列表 — GET /customers */
  getList(params?: Record<string, any>) {
    return request.get<any, ApiResponse<PageResult<Customer>>>('/customers', { params })
  },
  /** 工厂客户列表（customerType=2）— 用于业务员所属工厂选择 */
  getFactoryCustomers() {
    return request.get<any, ApiResponse<Customer[]>>('/customers/factories')
  },
  /** 全部客户列表（不分页，下拉选择用） */
  getAll() {
    return request.get<any, ApiResponse<Customer[]>>('/customers/all')
  },
  /** 获取系统级零售客户（公共客户，用于零散订单） */
  getRetailCustomer() {
    return request.get<any, ApiResponse<Customer>>('/customers/retail')
  },
  /** 客户详情 — GET /customers/{id} */
  getDetail(id: number) {
    return request.get<any, ApiResponse<Customer>>(`/customers/${id}`)
  },
  /** 新建客户 — POST /customers */
  create(data: any) {
    return request.post<any, ApiResponse<number>>('/customers', data)
  },
  /** 更新客户 — PUT /customers/{id} */
  update(id: number, data: Partial<Customer>) {
    return request.put<any, ApiResponse>(`/customers/${id}`, data)
  },
  /** 删除客户 — DELETE /customers/{id} */
  delete(id: number) {
    return request.delete<any, ApiResponse>(`/customers/${id}`)
  },

  // ========== 会员能力接口（基于客户表） ==========

  /** 会员客户列表 — GET /customers/members */
  getMemberList(params?: Record<string, any>) {
    return request.get<any, any>('/customers/members', { params })
  },
  /** 根据手机号匹配会员客户 — GET /customers/match?phone=xxx */
  matchMemberByPhone(phone: string) {
    return request.get<any, any>('/customers/match', { params: { phone } })
  },
  /** 升级客户为会员 — POST /customers/{id}/upgrade-member */
  upgradeToMember(id: number, data?: { level?: string; balance?: number }) {
    return request.post<any, any>(`/customers/${id}/upgrade-member`, data || {})
  },
  /** 会员充值 — POST /customers/{id}/recharge */
  recharge(id: number, data: { amount: number; remark?: string }) {
    return request.post<any, any>(`/customers/${id}/recharge`, data)
  },
  /** 会员消费 — POST /customers/{id}/consume */
  consume(id: number, data: { amount: number; orderId?: number; remark?: string }) {
    return request.post<any, any>(`/customers/${id}/consume`, data)
  },
  /** 更新会员等级 — PUT /customers/{id}/member-level */
  updateMemberLevel(id: number, data: { level: string }) {
    return request.put<any, any>(`/customers/${id}/member-level`, data)
  },
  /** 客户会员流水记录 — GET /customers/{id}/transactions */
  getTransactions(id: number) {
    return request.get<any, any>(`/customers/${id}/transactions`)
  },
  /** 充值记录列表 — GET /customers/transactions/recharge */
  getRechargeRecords(params?: Record<string, any>) {
    return request.get<any, any>('/customers/transactions/recharge', { params })
  },
  /** 消费记录列表 — GET /customers/transactions/consume */
  getConsumeRecords(params?: Record<string, any>) {
    return request.get<any, any>('/customers/transactions/consume', { params })
  },
}
