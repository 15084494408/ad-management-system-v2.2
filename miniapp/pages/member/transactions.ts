import { get } from '../../utils/request'
import { formatMoney, formatDateTime } from '../../utils/helpers'

Page({
  data: {
    customerId: 0,
    customerName: '',
    activeTab: 'all',
    transactions: [] as any[],
    loading: false
  },

  onLoad(options: any) {
    this.setData({
      customerId: parseInt(options.id),
      customerName: decodeURIComponent(options.name || '')
    })
    wx.setNavigationBarTitle({ title: this.data.customerName ? `${this.data.customerName} - 交易记录` : '交易记录' })
    this.loadData()
  },

  onTabChange(e: any) {
    this.setData({ activeTab: e.detail.value })
    this.loadData()
  },

  async loadData() {
    this.setData({ loading: true })
    try {
      const tab = this.data.activeTab
      let url = ''
      if (tab === 'recharge') {
        url = `/customers/${this.data.customerId}/transactions`
      } else if (tab === 'consume') {
        url = `/customers/${this.data.customerId}/transactions`
      } else {
        url = `/customers/${this.data.customerId}/transactions`
      }
      const res = await get<any>(url, null, { showLoading: false })

      let items = Array.isArray(res) ? res : (res.records || res.items || [])
      if (tab === 'recharge') {
        items = items.filter((t: any) => t.type === 'recharge')
      } else if (tab === 'consume') {
        items = items.filter((t: any) => t.type === 'consume' || t.type === 'refund')
      }

      this.setData({
        transactions: items.map((t: any) => ({
          ...t,
          amountStr: (t.type === 'consume' || t.type === 'refund') ? `-${formatMoney(t.amount)}` : `+${formatMoney(t.amount)}`,
          isIncome: t.type === 'recharge',
          typeLabel: ({ recharge: '充值', consume: '消费', refund: '退款' } as Record<string, string>)[t.type] || t.type || '未知',
          createTime: formatDateTime(t.createTime)
        }))
      })
    } catch (e) { console.error(e) }
    finally { this.setData({ loading: false }) }
  }
})
