import { get, post } from '../../utils/request'
import { formatMoney } from '../../utils/helpers'

Page({
  data: {
    materials: [] as any[],
    selectedIndex: -1,
    quantity: '',
    remark: '',
    submitting: false
  },

  onLoad() { this.loadMaterials() },

  async loadMaterials() {
    try {
      const res = await get<any>('/material/all', null, { showLoading: false })
      const list = Array.isArray(res) ? res : (res.records || [])
      this.setData({ materials: list })
    } catch (e) { console.error(e) }
  },

  pickMaterial() {
    const names = this.data.materials.map((m: any) => `${m.name} (库存:${m.stock || 0}${m.unit || ''})`)
    if (names.length === 0) { wx.showToast({ title: '暂无物料', icon: 'none' }); return }
    wx.showActionSheet({
      itemList: names,
      success: (res) => { this.setData({ selectedIndex: res.tapIndex }) }
    })
  },

  onFieldChange(e: any) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value })
  },

  async submit() {
    if (this.data.selectedIndex < 0) { wx.showToast({ title: '请选择物料', icon: 'none' }); return }
    const qty = parseFloat(this.data.quantity)
    if (!qty || qty <= 0) { wx.showToast({ title: '请输入有效数量', icon: 'none' }); return }
    const m = this.data.materials[this.data.selectedIndex]
    if (qty > (m.stock || 0)) { wx.showToast({ title: '库存不足', icon: 'none' }); return }
    this.setData({ submitting: true })
    try {
      await post('/material/stock-out', {
        materialId: m.id,
        quantity: qty,
        remark: this.data.remark || null
      })
      wx.showToast({ title: '出库成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    } catch (e) { /* handled */ }
    finally { this.setData({ submitting: false }) }
  }
})
