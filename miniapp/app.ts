App({
  globalData: {
    userInfo: null as WechatMiniprogram.UserInfo | null,
    token: '' as string,
    baseUrl: 'http://localhost:8080'
  },

  onLaunch() {
    const token = wx.getStorageSync('token')
    // 验证 token 格式：有效 JWT 应包含 2 个点号
    if (token && token.split('.').length === 3) {
      this.globalData.token = token
    } else {
      // 清除无效的 token（如演示留下的 demo-token）
      wx.removeStorageSync('token')
      wx.removeStorageSync('userInfo')
    }
  },

  // 检查是否已登录
  checkLogin(): boolean {
    return !!this.globalData.token
  },

  // 跳转登录页
  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  }
})
