import { passwordLogin, wxLogin } from '../../utils/auth'

Page({
  data: {
    username: '',
    password: '',
    loading: false
  },

  onUsernameChange(e: any) { this.setData({ username: e.detail.value }) },
  onPasswordChange(e: any) { this.setData({ password: e.detail.value }) },

  async login() {
    if (!this.data.username) { wx.showToast({ title: '请输入账号', icon: 'none' }); return }
    if (!this.data.password) { wx.showToast({ title: '请输入密码', icon: 'none' }); return }
    this.setData({ loading: true })
    try {
      await passwordLogin(this.data.username, this.data.password)
      wx.switchTab({ url: '/pages/index/index' })
    } catch (e) { /* handled in auth */ }
    finally { this.setData({ loading: false }) }
  },

  async wxLogin() {
    this.setData({ loading: true })
    try {
      await wxLogin()
      wx.switchTab({ url: '/pages/index/index' })
    } catch (e: any) {
      const msg = e?.message || ''
      if (msg.includes('未配置')) {
        wx.showToast({ title: '微信登录未配置', icon: 'none' })
      } else if (msg.includes('尚未绑定')) {
        wx.showModal({
          title: '提示',
          content: '该微信尚未绑定系统账号，请先使用账号密码登录',
          showCancel: false
        })
      } else {
        wx.showToast({ title: msg || '微信登录失败', icon: 'none' })
      }
    }
    finally { this.setData({ loading: false }) }
  }
})
