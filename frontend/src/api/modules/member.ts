import request from '@/api/request'

export const memberApi = {
  /** 会员列表 — GET /members */
  getList(params?: Record<string, any>) {
    return request.get<any, any>('/members', { params })
  },
  /** 会员详情 — GET /members/{id} */
  getDetail(id: number) {
    return request.get<any, any>(`/members/${id}`)
  },
  /** 新建会员 — POST /members */
  create(data: Record<string, any>) {
    return request.post<any, any>('/members', data)
  },
  /** 更新会员 — PUT /members/{id} */
  update(id: number, data: Record<string, any>) {
    return request.put<any, any>(`/members/${id}`, data)
  },
  /** 删除会员 — DELETE /members/{id} */
  delete(id: number) {
    return request.delete<any, any>(`/members/${id}`)
  },
  /** 会员充值 — POST /members/{id}/recharge (JSON Body) */
  recharge(id: number, data: { amount: number; remark?: string }) {
    return request.post<any, any>(`/members/${id}/recharge`, data)
  },
  /** 会员流水记录 — GET /members/{id}/transactions */
  getTransactions(id: number) {
    return request.get<any, any>(`/members/${id}/transactions`)
  },
  /** 充值记录列表 — GET /members/transactions/recharge */
  getRechargeRecords(params?: Record<string, any>) {
    return request.get<any, any>('/members/transactions/recharge', { params })
  },
  /** 消费记录列表 — GET /members/transactions/consume */
  getConsumeRecords(params?: Record<string, any>) {
    return request.get<any, any>('/members/transactions/consume', { params })
  },
  /** 等级列表 — 前端本地维护 */
  getLevels() {
    return Promise.resolve({ code: 200, data: [] })
  },
  /** 更新等级 — PUT /members/{id}/level */
  updateLevel(id: number, data: any) {
    return request.put<any, any>(`/members/${id}/level`, data)
  },
}
