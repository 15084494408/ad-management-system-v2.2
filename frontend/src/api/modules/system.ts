import request from '@/api/request'

export interface SysCompany {
  id?: number
  companyName: string
  address?: string
  phone?: string
  fax?: string
  email?: string
  bankName?: string
  bankAccount?: string
  taxNo?: string
  logoUrl?: string
  isDefault?: number   // 1=默认公司
  status?: number      // 0=禁用 1=启用
  createTime?: string
  updateTime?: string
}

export const systemApi = {
  // ========== 公司管理 ==========
  getCompanyList() {
    return request.get<any, any>('/system/companies')
  },
  createCompany(data: Partial<SysCompany>) {
    return request.post<any, any>('/system/companies', data)
  },
  updateCompany(id: number, data: Partial<SysCompany>) {
    return request.put<any, any>(`/system/companies/${id}`, data)
  },
  deleteCompany(id: number) {
    return request.delete<any, any>(`/system/companies/${id}`)
  },
}
