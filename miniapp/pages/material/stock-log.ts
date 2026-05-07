import { get } from '../../utils/request'
import { formatDateTime } from '../../utils/helpers'

Page({
  data: {
    logs: [] as any[],
    loading: false,
    loadingMore: false,
    noMore: false,
    current: 1,
    size: 20
  },

  onLoad() { this.loadLogs() },
  onPullDownRefresh() { this.resetAndLoad().then(() => wx.stopPullDownRefresh()) },
  onReachBottom() { if (!this.data.noMore && !this.data.loadingMore) this.loadMore() },

  buildParams() {
    return { current: this.data.current, size: this.data.size }
  },

  resetAndLoad() {
    this.setData({ current: 1, logs: [], noMore: false })
    return this.loadLogs()
  },

  async loadLogs() {
    this.setData({ loading: true })
    try {
      const res = await get<any>('/material/stock-log', this.buildParams())
      const records = (res.records || res || []).map((l: any) => ({
        ...l,
        typeLabel: l.type === 'in' ? '入库' : '出库',
        isIncome: l.type === 'in',
        quantityStr: l.type === 'in' ? `+${l.quantity}` : `-${l.quantity}`,
        createTime: formatDateTime(l.createTime)
      }))
      this.setData({ logs: records, noMore: records.length < this.data.size })
    } catch (e) { console.error(e) }
    finally { this.setData({ loading: false }) }
  },

  async loadMore() {
    this.setData({ loadingMore: true, current: this.data.current + 1 })
    try {
      const res = await get<any>('/material/stock-log', this.buildParams())
      const records = (res.records || res || []).map((l: any) => ({ ...l }))
      this.setData({ logs: [...this.data.logs, ...records], noMore: records.length < this.data.size })
    } catch (e) { console.error(e) }
    finally { this.setData({ loadingMore: false }) }
  }
})
