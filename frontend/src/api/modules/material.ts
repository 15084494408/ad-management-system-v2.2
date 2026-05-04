import request from '@/api/request'

export const materialApi = {
  /** 所有可用物料（下拉框用） — 对齐后端 GET /material/all */
  listAll() {
    return request.get<any, any[]>('/material/all')
  },
  /** 物料分类列表 — 对齐后端 GET /material/category */
  getCategories() {
    return request.get<any, any[]>('/material/category')
  },
  /** 物料列表（分页）— 对齐后端 GET /material */
  getList(params?: Record<string, any>) {
    return request.get('/material', { params })
  },
  /** 物料库存预警列表 — 对齐后端 GET /material/warning */
  getWarning() {
    return request.get<any, any[]>('/material/warning')
  },
}
