import { post } from '../../utils/request'

Page({
  data: {
    requirements: '',
    customerName: '',
    contactPhone: '',
    source: '',
    priority: 1,
    priorityOptions: [
      { value: 1, label: '普通', color: '#10b981' },
      { value: 2, label: '紧急', color: '#f59e0b' },
      { value: 3, label: '非常紧急', color: '#ef4444' }
    ]
  },

  onInputChange(e: any) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value })
  },

  onPrioritySelect(e: any) {
    const priority = e.currentTarget.dataset.value
    this.setData({ priority })
  },

  validate(): boolean {
    const { requirements } = this.data
    if (!requirements?.trim()) {
      wx.showToast({ title: '请输入需求描述', icon: 'none' })
      return false
    }
    return true
  },

  async submit() {
    if (!this.validate()) return

    const { requirements, customerName, contactPhone, source, priority } = this.data

    try {
      await post('/todo', {
        requirements: requirements.trim(),
        customerName: customerName || null,
        contactPhone: contactPhone || null,
        source: source || null,
        priority
      })

      wx.showToast({ title: '创建成功', icon: 'success' })

      // 通知上一页刷新
      const pages = getCurrentPages()
      const prevPage = pages[pages.length - 2]
      if (prevPage && prevPage.loadTodos) {
        prevPage.loadTodos()
      }

      setTimeout(() => {
        wx.navigateBack()
      }, 800)
    } catch (e) {
      // handled by request
    }
  },

  cancel() {
    wx.navigateBack()
  }
})
