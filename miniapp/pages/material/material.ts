import { get, post, put, del } from '../../utils/request'
import { formatMoney, formatDate } from '../../utils/helpers'

Page({
  data: {
    keyword: '',
    activeTab: 'all',
    materials: [] as any[],
    categories: [] as any[],
    loading: false,
    loadingMore: false,
    noMore: false,
    current: 1,
    size: 20
  },

  onLoad() {
    this.loadCategories()
    this.loadMaterials()
  },

  onShow() { this.resetAndLoad() },
  onPullDownRefresh() { this.resetAndLoad().then(() => wx.stopPullDownRefresh()) },
  onReachBottom() { if (!this.data.noMore && !this.data.loadingMore) this.loadMore() },

  onSearch(e: any) { this.setData({ keyword: e.detail.value }) },
  doSearch() { this.resetAndLoad() },

  onTabChange(e: any) {
    this.setData({ activeTab: e.detail.value, current: 1, materials: [], noMore: false })
    this.loadMaterials()
  },

  async loadCategories() {
    try {
      const res = await get<any>('/material/category', null, { showLoading: false })
      this.setData({ categories: Array.isArray(res) ? res : (res.records || []) })
    } catch (e) { console.error(e) }
  },

  buildParams() {
    const params: any = { current: this.data.current, size: this.data.size }
    if (this.data.keyword) params.keyword = this.data.keyword
    if (this.data.activeTab !== 'all') params.categoryId = this.data.activeTab
    return params
  },

  resetAndLoad() {
    this.setData({ current: 1, materials: [], noMore: false })
    return this.loadMaterials()
  },

  async loadMaterials() {
    this.setData({ loading: true })
    try {
      const res = await get<any>('/material', this.buildParams())
      const records = (res.records || res || []).map((m: any) => ({
        ...m,
        unitPriceStr: formatMoney(m.unitPrice || 0),
        unitCostStr: formatMoney(m.unitCost || 0),
        stockStr: m.stock || 0,
        isWarning: (m.stock || 0) <= (m.warningStock || 0) && !m.noStock,
        pricingLabel: m.pricingType === 1 ? '按面积' : '按数量',
        noStock: m.noStock || 0,
        createTime: formatDate(m.createTime)
      }))
      this.setData({ materials: records, noMore: records.length < this.data.size })
    } catch (e) { console.error(e) }
    finally { this.setData({ loading: false }) }
  },

  async loadMore() {
    this.setData({ loadingMore: true, current: this.data.current + 1 })
    try {
      const res = await get<any>('/material', this.buildParams())
      const records = (res.records || res || []).map((m: any) => ({ ...m }))
      this.setData({ materials: [...this.data.materials, ...records], noMore: records.length < this.data.size })
    } catch (e) { console.error(e) }
    finally { this.setData({ loadingMore: false }) }
  },

  goStockIn() { wx.navigateTo({ url: '/pages/material/stock-in' }) },
  goStockOut() { wx.navigateTo({ url: '/pages/material/stock-out' }) },
  goStockLog() { wx.navigateTo({ url: '/pages/material/stock-log' }) },
  goCategories() { wx.navigateTo({ url: '/pages/material/category' }) },

  // 添加物料（简化版弹窗）
  showAddDialog() { wx.navigateTo({ url: '/pages/material/material-edit' }) },

  editMaterial(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/pages/material/material-edit?id=${e.currentTarget.dataset.id}` })
  },

  deleteMaterial(e: any) {
    const id = e.currentTarget.dataset.id
    const name = e.currentTarget.dataset.name
    wx.showModal({
      title: '确认删除',
      content: `确定删除物料"${name}"？`,
      confirmColor: '#E34D59',
      success: async (res) => {
        if (res.confirm) {
          try {
            await del(`/material/${id}`)
            wx.showToast({ title: '已删除', icon: 'success' })
            this.resetAndLoad()
          } catch (e) { /* handled */ }
        }
      }
    })
  }
})
