import request from '@/api/request'

export const designerApi = {
  /** 设计师提成配置列表 */
  getCommissionList() {
    return request.get('/designer/commission/list')
  },
  /** 设置/更新设计师提成比例 */
  configCommission(data: { designerId: number; commissionRate: number; enabled?: boolean }) {
    return request.put('/designer/commission/config', data)
  },
  /** 获取设计师提成比例 */
  getCommissionRate(designerId: number) {
    return request.get(`/designer/commission/rate/${designerId}`)
  },
}
