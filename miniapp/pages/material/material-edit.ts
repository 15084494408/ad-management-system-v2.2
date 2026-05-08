import { get, post, put } from '../../utils/request'

Page({
  data: {
    editId: 0,
    form: { name: '', categoryId: '', unit: '', unitPrice: '', unitCost: '', stock: '', warningStock: '', pricingType: 0, noStock: 0, remark: '' },
    categories: [] as any[],
    submitting: false
  },

  onLoad(options: any) {
    if (options.id) {
      this.setData({ editId: parseInt(options.id) })
      this.loadMaterial()
    }
    this.loadCategories()
  },

  async loadCategories() {
    try {
      const res = await get<any>('/material/category', null, { showLoading: false })
      this.setData({ categories: Array.isArray(res) ? res : (res.records || []) })
    } catch (e) { console.error(e) }
  },

  async loadMaterial() {
    try {
      const res = await get<any>(`/material/all`, null, { showLoading: false })
      const list = Array.isArray(res) ? res : (res.records || [])
      const m = list.find((item: any) => item.id === this.data.editId)
      if (m) {
        // 后端返回的是 no_stock (snake_case)，小程序用 noStock (camelCase)
        const noStock = m.noStock != null ? m.noStock : (m.no_stock || 0)
        this.setData({
          form: {
            name: m.name || '',
            categoryId: m.categoryId || '',
            unit: m.unit || '',
            unitPrice: String(m.unitPrice || ''),
            unitCost: String(m.unitCost || ''),
            stock: String(m.stock || ''),
            warningStock: String(m.warningStock || ''),
            pricingType: m.pricingType || 0,
            noStock: noStock,
            remark: m.remark || ''
          }
        })
      }
    } catch (e) { console.error(e) }
  },

  onChange(e: any) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  pickCategory() {
    const names = this.data.categories.map((c: any) => c.name)
    names.unshift('无分类')
    wx.showActionSheet({
      itemList: names,
      success: (res) => {
        if (res.tapIndex === 0) {
          this.setData({ 'form.categoryId': '' })
        } else {
          this.setData({ 'form.categoryId': this.data.categories[res.tapIndex - 1].id })
        }
      }
    })
  },

  onPricingTypeChange(e: any) {
    this.setData({ 'form.pricingType': parseInt(e.detail.value) || 0 })
  },

  onNoStockChange(e: any) {
    this.setData({ 'form.noStock': e.detail.value ? 1 : 0 })
  },

  async submit() {
    const f = this.data.form
    if (!f.name.trim()) { wx.showToast({ title: '请输入物料名称', icon: 'none' }); return }
    this.setData({ submitting: true })
    try {
      const data = {
        name: f.name.trim(),
        categoryId: f.categoryId || null,
        unit: f.unit || null,
        unitPrice: parseFloat(f.unitPrice) || 0,
        unitCost: parseFloat(f.unitCost) || 0,
        stock: parseFloat(f.stock) || 0,
        warningStock: parseFloat(f.warningStock) || 0,
        pricingType: f.pricingType,
        noStock: f.noStock || 0,
        remark: f.remark || null
      }
      if (this.data.editId) {
        await put(`/material/${this.data.editId}`, data)
      } else {
        await post('/material', data)
      }
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    } catch (e) { /* handled */ }
    finally { this.setData({ submitting: false }) }
  }
})
