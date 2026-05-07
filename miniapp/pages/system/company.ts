import { get, post, put, del } from '../../utils/request'
import { formatDate } from '../../utils/helpers'

Page({
  data: {
    companies: [] as any[],
    loading: false,
    showEdit: false,
    editId: 0,
    form: { name: '', contactPerson: '', contactPhone: '', address: '', remark: '' },
    submitting: false
  },

  onLoad() { this.loadCompanies() },
  onShow() { this.loadCompanies() },

  async loadCompanies() {
    this.setData({ loading: true })
    try {
      const res = await get<any>('/system/companies', null, { showLoading: false })
      const list = Array.isArray(res) ? res : (res.records || [])
      this.setData({
        companies: list.map((c: any) => ({
          ...c,
          createTime: formatDate(c.createTime)
        }))
      })
    } catch (e) { console.error(e) }
    finally { this.setData({ loading: false }) }
  },

  showCreateDialog() {
    this.setData({ showEdit: true, editId: 0, form: { name: '', contactPerson: '', contactPhone: '', address: '', remark: '' } })
  },

  showEditDialog(e: any) {
    const c = e.currentTarget.dataset.item
    this.setData({
      showEdit: true,
      editId: c.id,
      form: { name: c.name || '', contactPerson: c.contactPerson || '', contactPhone: c.contactPhone || '', address: c.address || '', remark: c.remark || '' }
    })
  },

  hideDialog() { this.setData({ showEdit: false }) },

  onFormChange(e: any) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  async submit() {
    const f = this.data.form
    if (!f.name.trim()) { wx.showToast({ title: '请输入公司名称', icon: 'none' }); return }
    this.setData({ submitting: true })
    try {
      const data = {
        name: f.name.trim(),
        contactPerson: f.contactPerson || null,
        contactPhone: f.contactPhone || null,
        address: f.address || null,
        remark: f.remark || null
      }
      if (this.data.editId) {
        await put(`/system/companies/${this.data.editId}`, data)
      } else {
        await post('/system/companies', data)
      }
      wx.showToast({ title: '保存成功', icon: 'success' })
      this.setData({ showEdit: false })
      this.loadCompanies()
    } catch (e) { /* handled */ }
    finally { this.setData({ submitting: false }) }
  },

  deleteCompany(e: any) {
    const item = e.currentTarget.dataset.item
    wx.showModal({
      title: '确认删除',
      content: `确定删除公司"${item.name}"？`,
      confirmColor: '#E34D59',
      success: async (res) => {
        if (res.confirm) {
          try {
            await del(`/system/companies/${item.id}`)
            wx.showToast({ title: '已删除', icon: 'success' })
            this.loadCompanies()
          } catch (e) { /* handled */ }
        }
      }
    })
  }
})
