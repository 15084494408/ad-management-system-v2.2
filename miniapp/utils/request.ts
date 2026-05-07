import config from './config'

/** 请求选项 */
interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  data?: any
  header?: Record<string, string>
  showLoading?: boolean
  showError?: boolean
}

/** 后端统一响应格式 */
interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/** 分页响应 */
interface PageResult<T = any> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/** 统一请求封装 */
function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token')

    if (options.showLoading !== false) {
      wx.showLoading({ title: '加载中...', mask: true })
    }

    wx.request({
      url: config.baseUrl + options.url,
      method: options.method as any || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header
      },
      timeout: (options as any).timeout || config.timeout,
      success: (res) => {
        wx.hideLoading()

        if (res.statusCode === 200) {
          const body = res.data as ApiResponse<T>
          if (body.code === 200 || body.code === 0) {
            resolve(body.data)
          } else if (body.code === 401) {
            // Token 过期，跳转登录
            wx.removeStorageSync('token')
            wx.showToast({ title: '登录已过期', icon: 'none' })
            setTimeout(() => {
              wx.navigateTo({ url: '/pages/login/login' })
            }, 1500)
            reject(new Error(body.message || '未授权'))
          } else {
            if (options.showError !== false) {
              wx.showToast({ title: body.message || '请求失败', icon: 'none' })
            }
            reject(new Error(body.message || '请求失败'))
          }
        } else if (res.statusCode === 401) {
          wx.removeStorageSync('token')
          wx.showToast({ title: '登录已过期', icon: 'none' })
          setTimeout(() => {
            wx.navigateTo({ url: '/pages/login/login' })
          }, 1500)
          reject(new Error('未授权'))
        } else {
          if (options.showError !== false) {
            wx.showToast({ title: `请求失败(${res.statusCode})`, icon: 'none' })
          }
          reject(new Error(`HTTP ${res.statusCode}`))
        }
      },
      fail: (err) => {
        wx.hideLoading()
        wx.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

/** GET 请求 */
export function get<T = any>(url: string, data?: any, options?: Partial<RequestOptions>): Promise<T> {
  return request<T>({ url, method: 'GET', data, ...options })
}

/** POST 请求 */
export function post<T = any>(url: string, data?: any, options?: Partial<RequestOptions>): Promise<T> {
  return request<T>({ url, method: 'POST', data, ...options })
}

/** PUT 请求 */
export function put<T = any>(url: string, data?: any, options?: Partial<RequestOptions>): Promise<T> {
  return request<T>({ url, method: 'PUT', data, ...options })
}

/** DELETE 请求 */
export function del<T = any>(url: string, data?: any, options?: Partial<RequestOptions>): Promise<T> {
  return request<T>({ url, method: 'DELETE', data, ...options })
}

/** PATCH 请求 */
export function patch<T = any>(url: string, data?: any, options?: Partial<RequestOptions>): Promise<T> {
  return request<T>({ url, method: 'PATCH', data, ...options })
}

/** 文件上传 */
export function upload(url: string, filePath: string, name = 'file'): Promise<any> {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token')
    wx.uploadFile({
      url: config.baseUrl + url,
      filePath,
      name,
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          const body = JSON.parse(res.data)
          if (body.code === 200 || body.code === 0) {
            resolve(body.data)
          } else {
            reject(new Error(body.message || '上传失败'))
          }
        } else {
          reject(new Error(`上传失败(${res.statusCode})`))
        }
      },
      fail: reject
    })
  })
}

export type { ApiResponse, PageResult, RequestOptions }
