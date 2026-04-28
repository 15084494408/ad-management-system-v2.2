import request from '@/api/request'

export const orderApi = {
  /** 订单列表 — 对齐后端 GET /orders */
  getList(params: Record<string, any>) {
    return request.get('/orders', { params })
  },
  /** 订单详情 — 对齐后端 GET /orders/{id}，返回 { order, materials, materialTotal } */
  getDetail(id: number) {
    return request.get(`/orders/${id}`)
  },
  /** 新建订单 — 对齐后端 POST /orders */
  create(data: any) {
    return request.post('/orders', data)
  },
  /** 更新订单 — 对齐后端 PUT /orders/{id} */
  update(id: number, data: any) {
    return request.put(`/orders/${id}`, data)
  },
  /** 删除订单 — 对齐后端 DELETE /orders/{id} */
  delete(id: number) {
    return request.delete(`/orders/${id}`)
  },
  /** 订单物料明细 — 对齐后端 GET /orders/{id}/materials */
  getMaterials(id: number) {
    return request.get(`/orders/${id}/materials`)
  },
  /** 添加物料明细 — 对齐后端 POST /orders/{id}/materials */
  addMaterial(id: number, data: any) {
    return request.post(`/orders/${id}/materials`, data)
  },
  /** 更新物料明细（管理员可修改成本）— 对齐后端 PUT /orders/{id}/materials/{materialId} */
  updateMaterial(id: number, materialId: number, data: any) {
    return request.put(`/orders/${id}/materials/${materialId}`, data)
  },
  /** 删除物料明细 — 对齐后端 DELETE /orders/{id}/materials/{materialId} */
  removeMaterial(id: number, materialId: number) {
    return request.delete(`/orders/${id}/materials/${materialId}`)
  },
  /** 更新订单状态 — 对齐后端 PATCH /orders/{id}/status，body: { status: number } */
  updateStatus(id: number, status: number) {
    return request.patch(`/orders/${id}/status`, { status })
  },
  /** 登记收款 — 对齐后端 POST /orders/{id}/payment */
  addPayment(id: number, data: { amount: number; method?: string; payer?: string; remark?: string }) {
    return request.post(`/orders/${id}/payment`, data)
  },
  /** 订单统计 — 对齐后端 GET /orders/statistics */
  getStatistics(params?: { period?: string; startDate?: string; endDate?: string }) {
    return request.get('/orders/statistics', { params })
  },
}
