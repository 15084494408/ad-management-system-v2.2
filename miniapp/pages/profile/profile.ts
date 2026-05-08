import { getUserInfo, isLoggedIn, logout, bindWx } from '../../utils/auth'

Page({
  data: {
    userInfo: {} as any,
    roleText: '',
    wxBound: false
  },

  onShow() {
    if (!isLoggedIn()) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    const info = getUserInfo() || {}
    const roles = (info.roles || [])
    const roleText = roles.includes('SUPER_ADMIN') ? '超级管理员' :
                     roles.includes('ADMIN') ? '管理员' :
                     roles.includes('FINANCE') ? '财务' :
                     roles.includes('DESIGNER') ? '设计师' :
                     roles.includes('OPERATOR') ? '操作员' :
                     roles.includes('VIEWER') ? '观察者' :
                     roles.length > 0 ? roles[0] : '普通用户'
    this.setData({
      userInfo: info,
      roleText,
      wxBound: !!(info as any).wxBound
    })
  },

  // ===== 业务模块导航 =====
  goTodo() { wx.navigateTo({ url: '/pages/todo/todo' }) },
  goOrderQuote() { wx.navigateTo({ url: '/pages/orders/quote/quote' }) },
  goCustomerBills() { wx.navigateTo({ url: '/pages/customer/bills/customer-bills' }) },
  goFactoryBills() { wx.navigateTo({ url: '/pages/customer/bills/factory-bills' }) },
  goMember() { wx.navigateTo({ url: '/pages/member/member' }) },
  goMaterial() { wx.navigateTo({ url: '/pages/material/material' }) },
  goUsers() { wx.navigateTo({ url: '/pages/system/users' }) },
  goCompany() { wx.navigateTo({ url: '/pages/system/company' }) },

  // ===== 账户功能 =====
  changePassword() {
    wx.navigateTo({ url: '/pages/profile/password/password' })
  },

  async bindWxAction() {
    if (this.data.wxBound) {
      wx.showToast({ title: '已绑定微信', icon: 'none' })
      return
    }
    try {
      await bindWx()
      this.setData({ wxBound: true })
    } catch (e) { /* handled */ }
  },

  checkUpdate() {
    wx.showToast({ title: '已是最新版本', icon: 'none' })
  },

  clearCache() {
    wx.showModal({
      title: '确认清除',
      content: '将清除本地缓存数据，不影响账号信息',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('userInfo')
          wx.showToast({ title: '缓存已清除', icon: 'success' })
        }
      }
    })
  },

  about() {
    wx.showModal({
      title: '关于',
      content: '企业广告管理系统 v2.0\n微信小程序版\n数据与Web端实时同步',
      showCancel: false
    })
  },

  doLogout() {
    wx.showModal({
      title: '提示',
      content: '确认退出登录？',
      success: (res) => {
        if (res.confirm) { logout() }
      }
    })
  }
})
