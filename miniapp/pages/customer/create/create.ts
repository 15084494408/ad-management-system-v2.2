import { post, get, put } from '../../../utils/request'

Page({
  data: {
    mode: 'create', // create 或 edit
    customerId: 0,
    form: {
      name: '',
      phone: '',
      address: '',
      customerType: 1,
      contactPerson: '',
      remark: ''
    },
    typeOptions: [
      { value: 1, label: '普通客户' },
      { value: 2, label: '工厂客户' }
    ],
    submitting: false,
    pageLoading: false
  },

  onLoad(options: any) {
    if (options.id && options.mode === 'edit') {
      this.setData({ mode: 'edit', customerId: parseInt(options.id) })
      wx.setNavigationBarTitle({ title: '编辑客户' })
      this.loadCustomer()
    }
  },

  async loadCustomer() {
    this.setData({ pageLoading: true })
    try {
      const c = await get<any>(`/customers/${this.data.customerId}`)
      this.setData({
        form: {
          name: c.name || c.customerName || '',
          phone: c.phone || '',
          address: c.address || '',
          customerType: c.customerType || 1,
          contactPerson: c.contactPerson || '',
          remark: c.remark || ''
        }
      })
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
    } finally {
      this.setData({ pageLoading: false })
    }
  },

  onChange(e: any) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  onTypeChange(e: any) {
    const idx = e.currentTarget.dataset.index
    this.setData({ 'form.customerType': this.data.typeOptions[idx].value })
  },

  async submit() {
    const f = this.data.form
    if (!f.name.trim()) {
      wx.showToast({ title: '请输入客户名称', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const data = {
        name: f.name.trim(),
        phone: f.phone || null,
        address: f.address || null,
        customerType: f.customerType,
        contactPerson: f.contactPerson || null,
        remark: f.remark || null
      }
      if (this.data.mode === 'edit') {
        await put(`/customers/${this.data.customerId}`, data)
      } else {
        await post('/customers', data)
      }
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    } catch (e) { /* handled */ }
    finally { this.setData({ submitting: false }) }
  }
})
