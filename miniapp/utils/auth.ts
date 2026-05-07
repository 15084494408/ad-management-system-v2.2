import { post } from './request'

/** 登录结果 */
interface LoginResult {
  token: string
  userInfo: {
    id: number
    username: string
    realName: string
    avatar: string
    roles: string[]
    permissions?: string[]
  }
}

/** 微信登录（后端需实现 /auth/wx-login） */
export async function wxLogin(): Promise<LoginResult> {
  const { code } = await wx.login()
  const result = await post<LoginResult>('/auth/wx-login', { code }, { showLoading: true })
  wx.setStorageSync('token', result.token)
  wx.setStorageSync('userInfo', result.userInfo)
  return result
}

/** 绑定微信（已登录用户） */
export async function bindWx(): Promise<void> {
  const { code } = await wx.login()
  await post('/auth/wx-bind', { code }, { showLoading: true })
  wx.showToast({ title: '绑定成功', icon: 'success' })
}

/** 账号密码登录 */
export async function passwordLogin(username: string, password: string): Promise<LoginResult> {
  const result = await post<LoginResult>('/auth/login', { username, password }, { showLoading: true })
  wx.setStorageSync('token', result.token)
  wx.setStorageSync('userInfo', result.userInfo)
  return result
}

/** 获取当前用户信息 */
export function getUserInfo() {
  return wx.getStorageSync('userInfo') || null
}

/** 获取 token */
export function getToken(): string {
  return wx.getStorageSync('token') || ''
}

/** 是否已登录 */
export function isLoggedIn(): boolean {
  return !!getToken()
}

/** 退出登录 */
export function logout() {
  wx.removeStorageSync('token')
  wx.removeStorageSync('userInfo')
  wx.reLaunch({ url: '/pages/login/login' })
}

/** 检查登录状态，未登录跳转登录页 */
export function requireAuth(): boolean {
  if (isLoggedIn()) return true
  wx.navigateTo({ url: '/pages/login/login' })
  return false
}
