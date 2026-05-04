import request from '@/api/request'
import type { ApiResponse, LoginDTO, LoginVO, User } from '@/types'

export const authApi = {
  /** 用户登录 */
  login(data: LoginDTO) {
    return request.post<any, ApiResponse<LoginVO>>('/auth/login', data)
  },
  /** 用户登出 */
  logout() {
    return request.post<any, ApiResponse>('/auth/logout')
  },
  /** 获取当前用户信息 — 对齐后端 GET /auth/me */
  getUserInfo() {
    return request.get<any, ApiResponse<User>>('/auth/me')
  },
}
