import { get, post } from '../../../utils/request'
import { formatMoney, formatDate, timeAgo } from '../../../utils/helpers'

Page({
  data: {
    customerId: 0,
    customer: null as any,
    loading: true,
    activeTab: 'info',
    orders: [] as any[],
    ordersLoading: false
  },

  onLoad(options: any) {
    this.setData({ customerId: parseInt(options.id) })
    this.loadCustomer()
  },

  onShow() { if (this.data.customerId) this.loadCustomer() },
  onPullDownRefresh() { this.loadCustomer().then(() => wx.stopPullDownRefresh()) },

  onTabChange(e: any) {
    const tab = e.detail.value
    this.setData({ activeTab: tab })
    if (tab === 'orders' && this.data.orders.length === 0) {
      this.loadOrders()
    }
  },

  async loadCustomer() {
    this.setData({ loading: true })
    try {
      const c = await get<any>(`/customers/${this.data.customerId}`)
      const memberLevelName: Record<number, string> = { 1: '普通会员', 2: 'VIP会员', 3: '战略会员' }
      const typeLabel: Record<number, string> = { 1: '普通', 2: '工厂', 3: '零售' }
      this.setData({
        customer: {
          ...c,
          displayName: c.name || c.customerName || '',
          typeLabel: typeLabel[c.customerType] || '普通',
          memberLevelName: memberLevelName[c.memberLevel] || '普通会员',
          balance: formatMoney(c.balance || 0),
          totalRecharge: formatMoney(c.totalRecharge || 0),
          totalConsume: formatMoney(c.totalConsume || 0),
          totalAmount: formatMoney(c.totalAmount || 0),
          orderCount: c.orderCount || 0,
          isMember: c.isMember === 1,
          createTime: formatDate(c.createTime)
        }
      })
    } catch (e) { console.error(e) }
    finally { this.setData({ loading: false }) }
  },

  async loadOrders() {
    this.setData({ ordersLoading: true })
    try {
      const res = await get<any>('/orders', { customerId: this.data.customerId, current: 1, size: 50 }, { showLoading: false })
      const statusMap: Record<number, string> = { 1: '待处理', 2: '进行中', 3: '已完成', 4: '已取消' }
      const statusColorMap: Record<number, string> = { 1: 'warning', 2: 'primary', 3: 'success', 4: 'default' }
      this.setData({
        orders: (res.records || []).map((o: any) => ({
          ...o,
          statusText: statusMap[o.status] || '未知',
          statusColor: statusColorMap[o.status] || 'default',
          totalAmount: formatMoney(o.totalAmount),
          createTime: timeAgo(o.createTime)
        }))
      })
    } catch (e) { console.error(e) }
    finally { this.setData({ ordersLoading: false }) }
  },

  recharge() {
    wx.navigateTo({ url: `/pages/customer/recharge/recharge?id=${this.data.customerId}` })
  },

  async upgradeMember() {
    wx.showModal({
      title: '升级为会员',
      content: '确认将此客户升级为会员？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await post(`/customers/${this.data.customerId}/upgrade-member`)
            wx.showToast({ title: '升级成功', icon: 'success' })
            this.loadCustomer()
          } catch (e) { /* handled */ }
        }
      }
    })
  },

  callPhone() {
    const phone = this.data.customer?.phone
    if (phone) wx.makePhoneCall({ phoneNumber: phone })
  },

  editCustomer() {
    wx.navigateTo({ url: `/pages/customer/create/create?id=${this.data.customerId}&mode=edit` })
  },

  goTransactions() {
    wx.navigateTo({ url: `/pages/member/transactions?id=${this.data.customerId}&name=${encodeURIComponent(this.data.customer?.displayName || '')}` })
  },

  goOrderDetail(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/pages/orders/detail/detail?id=${e.currentTarget.dataset.id}` })
  }
})
