import request from '@/api/request'

export const designFileApi = {
  /** 设计文件列表（分页） */
  getList(params?: Record<string, any>) {
    return request.get('/design/file', { params })
  },
  /** 按订单查询设计文件（不分页） */
  getByOrderId(orderId: number) {
    return request.get('/design/file', { params: { orderId, current: 1, size: 50 } })
  },
  /** 文件详情 */
  getById(id: number) {
    return request.get(`/design/file/${id}`)
  },
  /** 文件版本历史 */
  getVersions(id: number) {
    return request.get(`/design/file/${id}/versions`)
  },
  /** 更新文件信息 */
  update(id: number, data: any) {
    return request.put(`/design/file/${id}`, data)
  },
  /** 审核文件 */
  updateStatus(id: number, status: number, remark?: string) {
    return request.put(`/design/file/${id}/status`, null, { params: { status, remark } })
  },
  /** 删除文件 */
  delete(id: number) {
    return request.delete(`/design/file/${id}`)
  },
  /** 上传文件（multipart/form-data） */
  uploadUrl: '/api/design/file/upload',
}
