import { get, put } from '../../utils/request'
import { timeAgo } from '../../utils/helpers'

Page({
  data: {
    list: [] as any[],
    unreadCount: 0,
    loading: false,
    page: 1,
    noMore: false
  },

  onLoad() {
    this.loadData()
    this.loadUnreadCount()
  },

  onShow() {
    this.loadData()
    this.loadUnreadCount()
  },

  onPullDownRefresh() {
    this.setData({ page: 1, noMore: false })
    Promise.all([this.loadData(), this.loadUnreadCount()])
      .then(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (!this.data.noMore && !this.data.loading) {
      this.setData({ page: this.data.page + 1 })
      this.loadData(true)
    }
  },

  async loadData(append = false) {
    this.setData({ loading: true })
    try {
      const res = await get<any>('/notice', { current: this.data.page, size: 20 }, { showLoading: false })
      const records = (res.records || []).map((n: any) => ({
        id: n.id,
        title: n.title,
        content: n.content,
        type: n.type || 1,
        read: n.read || false,
        createTime: timeAgo(n.createTime)
      }))
      this.setData({
        list: append ? [...this.data.list, ...records] : records,
        noMore: !res.records || res.records.length < 20
      })
    } catch (e) { console.error('加载消息失败', e) }
    finally { this.setData({ loading: false }) }
  },

  async loadUnreadCount() {
    try {
      const res = await get<any>('/notice/unread-count', null, { showLoading: false })
      this.setData({ unreadCount: res.count || 0 })
    } catch (e) { /* ignore */ }
  },

  async markAsRead(e: any) {
    const id = e.currentTarget.dataset.id
    const item = this.data.list.find((n: any) => n.id === id)
    if (item?.read) return
    try {
      await put(`/notice/${id}/read`, null, { showLoading: false })
      // 更新本地状态
      const list = this.data.list.map((n: any) => n.id === id ? { ...n, read: true } : n)
      this.setData({
        list,
        unreadCount: Math.max(0, this.data.unreadCount - 1)
      })
      // 更新全局未读数
      const app = getApp() as any
      if (app.updateUnreadCount) app.updateUnreadCount()
    } catch (e) { /* handled */ }
  },

  async markAllRead() {
    wx.showLoading({ title: '处理中...' })
    try {
      await put('/notice/read-all', null, { showLoading: false })
      const list = this.data.list.map((n: any) => ({ ...n, read: true }))
      this.setData({ list, unreadCount: 0 })
      const app = getApp() as any
      if (app.updateUnreadCount) app.updateUnreadCount()
      wx.showToast({ title: '全部已读', icon: 'success' })
    } catch (e) { /* handled */ }
    finally { wx.hideLoading() }
  }
})
