import { get } from '../../utils/request'
import { formatMoney, formatDate } from '../../utils/helpers'

Page({
  data: {
    keyword: '',
    members: [] as any[],
    loading: false,
    loadingMore: false,
    noMore: false,
    current: 1,
    size: 20
  },

  onLoad() { this.loadMembers() },
  onShow() { this.resetAndLoad() },
  onPullDownRefresh() { this.resetAndLoad().then(() => wx.stopPullDownRefresh()) },
  onReachBottom() { if (!this.data.noMore && !this.data.loadingMore) this.loadMore() },

  onSearch(e: any) { this.setData({ keyword: e.detail.value }) },
  doSearch() { this.resetAndLoad() },

  buildParams() {
    const params: any = { current: this.data.current, size: this.data.size, isMember: 1 }
    if (this.data.keyword) params.keyword = this.data.keyword
    return params
  },

  resetAndLoad() {
    this.setData({ current: 1, members: [], noMore: false })
    return this.loadMembers()
  },

  async loadMembers() {
    this.setData({ loading: true })
    try {
      const res = await get<any>('/customers', this.buildParams())
      const levelName: Record<number, string> = { 1: '普通会员', 2: 'VIP会员', 3: '战略会员' }
      const records = (res.records || []).map((c: any) => ({
        ...c,
        levelName: levelName[c.memberLevel || c.level] || '普通会员',
        balance: formatMoney(c.balance || 0),
        totalRecharge: formatMoney(c.totalRecharge || 0),
        totalConsume: formatMoney(c.totalConsume || 0),
        createTime: formatDate(c.createTime)
      }))
      this.setData({ members: records, noMore: records.length < this.data.size })
    } catch (e) { console.error(e) }
    finally { this.setData({ loading: false }) }
  },

  async loadMore() {
    this.setData({ loadingMore: true, current: this.data.current + 1 })
    try {
      const res = await get<any>('/customers', this.buildParams())
      const records = (res.records || []).map((c: any) => ({ ...c }))
      this.setData({ members: [...this.data.members, ...records], noMore: records.length < this.data.size })
    } catch (e) { console.error(e) }
    finally { this.setData({ loadingMore: false }) }
  },

  goDetail(e: any) {
    wx.navigateTo({ url: `/pages/customer/detail/detail?id=${e.currentTarget.dataset.id}` })
  },

  goRecharge(e: any) {
    wx.navigateTo({ url: `/pages/customer/recharge/recharge?id=${e.currentTarget.dataset.id}` })
  },

  goTransactions(e: any) {
    wx.navigateTo({ url: `/pages/member/transactions?id=${e.currentTarget.dataset.id}&name=${encodeURIComponent(e.currentTarget.dataset.name)}` })
  }
})
