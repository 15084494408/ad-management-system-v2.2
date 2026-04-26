import request from '@/api/request'
import type { ApiResponse, PageResult, Customer } from '@/types'

export const customerApi = {
  /** 客户列表 — 对齐后端 GET /customers */
  getList(params?: Record<string, any>) {
    return request.get<any, ApiResponse<PageResult<Customer>>>('/customers', { params })
  },
  /** 客户详情 — 对齐后端 GET /customers/{id} */
  getDetail(id: number) {
    return request.get<any, ApiResponse<Customer>>(`/customers/${id}`)
  },
  /** 新建客户 — 对齐后端 POST /customers */
  create(data: any) {
    return request.post<any, ApiResponse<number>>('/customers', data)
  },
  /** 更新客户 — 对齐后端 PUT /customers/{id} */
  update(id: number, data: Partial<Customer>) {
    return request.put<any, ApiResponse>(`/customers/${id}`, data)
  },
  /** 删除客户 — 对齐后端 DELETE /customers/{id} */
  delete(id: number) {
    return request.delete<any, ApiResponse>(`/customers/${id}`)
  },
}
