/** API 基础配置 */
const config = {
  // 后端地址 - 开发环境用本地，生产环境换成实际域名
  // 后端配置了 context-path: /api，所以加上 /api 前缀
  baseUrl: 'http://localhost:8080/api',

  // 接口超时时间 (ms)
  timeout: 15000,

  // 微信小程序 AppID
  appId: '',

  // 版本号
  version: '1.0.0'
}

export default config
