import request from '@/api/request'

export const todoApi = {
  /** 待办列表 */
  getList(status?: number) {
    return request.get('/todo/list', { params: status != null ? { status } : {} })
  },
  /** 新增待办 */
  create(data: any) {
    return request.post('/todo', data)
  },
  /** 更新待办 */
  update(id: number, data: any) {
    return request.put(`/todo/${id}`, data)
  },
  /** 更新状态 */
  updateStatus(id: number, data: { status: number }) {
    return request.patch(`/todo/${id}/status`, data)
  },
  /** 删除待办 */
  delete(id: number) {
    return request.delete(`/todo/${id}`)
  },
}
