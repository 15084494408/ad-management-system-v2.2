import { get, post, patch } from '../../../utils/request'
import { formatMoney, formatDate, timeAgo } from '../../../utils/helpers'

Page({
  data: {
    orderId: 0,
    order: null as any,
    loading: true,
    statusList: [
      { value: 1, label: '待处理' },
      { value: 2, label: '进行中' },
      { value: 3, label: '已完成' },
      { value: 4, label: '已取消' }
    ],
    showStatusPicker: false,
    showPaymentDialog: false,
    paymentAmount: '',
    paymentMethod: '现金',
    paymentMethods: ['现金', '微信', '支付宝', '转账', '其他']
  },

  onLoad(options: any) {
    this.setData({ orderId: parseInt(options.id) })
    this.loadOrder()
  },

  onShow() {
    if (this.data.orderId) this.loadOrder()
  },

  async loadOrder() {
    this.setData({ loading: true })
    try {
      const res = await get<any>(`/orders/${this.data.orderId}`, null, { showLoading: false })
      const order = res.order || {}
      const materials = res.materials || []
      const priorityMap: Record<number, string> = { 1: '普通', 2: '紧急', 3: '加急' }
      const orderTypeMap: Record<number, string> = { 1: '印刷', 2: '广告', 3: '设计' }
      const paymentMap: Record<number, string> = { 1: '未付', 2: '部分', 3: '已付清', 4: '抹零' }

      const unpaid = Math.max(
        (order.totalAmount || 0) - (order.paidAmount || 0), 0
      )

      this.setData({
        order: {
          ...order,
          statusText: ({ 1: '待处理', 2: '进行中', 3: '已完成', 4: '已取消' } as Record<string, string>)[order.status] || '未知',
          statusTheme: ({ 1: 'warning', 2: 'primary', 3: 'success', 4: 'default' } as Record<string, string>)[order.status] || 'default',
          paymentStatusText: paymentMap[order.paymentStatus] || '未知',
          priorityText: priorityMap[order.priority || 1] || '普通',
          orderTypeText: orderTypeMap[order.orderType || 1] || '印刷',
          totalAmount: formatMoney(order.totalAmount),
          paidAmount: formatMoney(order.paidAmount),
          discountAmount: order.discountAmount || 0,
          roundingAmount: order.roundingAmount || 0,
          unpaidAmount: formatMoney(unpaid),
          rawUnpaid: unpaid,
          createTime: formatDate(order.createTime),
          updateTime: order.updateTime ? formatDate(order.updateTime) : '',
          deliveryDate: order.deliveryDate ? formatDate(order.deliveryDate) : '-',
          materials: materials.map((m: any) => ({
            ...m,
            amount: formatMoney(m.amount || (m.quantity || 0) * (m.unitPrice || 0)),
            unitPrice: formatMoney(m.unitPrice)
          }))
        }
      })
    } catch (e) {
      console.error('加载订单失败', e)
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  // ==================== 状态变更 ====================
  showStatusPickerFn() {
    this.setData({ showStatusPicker: true })
  },

  hideStatusPicker() {
    this.setData({ showStatusPicker: false })
  },

  pickStatus(e: WechatMiniprogram.TouchEvent) {
    const idx = e.currentTarget.dataset.index
    const newStatus = this.data.statusList[idx].value
    const label = this.data.statusList[idx].label

    if (newStatus === this.data.order.status) {
      this.setData({ showStatusPicker: false })
      return
    }

    wx.showModal({
      title: '确认状态变更',
      content: `将订单状态从"${this.data.order.statusText}"改为"${label}"？`,
      confirmColor: newStatus === 4 ? '#E34D59' : '#0052D9',
      success: async (res) => {
        if (res.confirm) {
          try {
            await patch(`/orders/${this.data.orderId}/status`, { status: newStatus })
            wx.showToast({ title: '状态已更新', icon: 'success' })
            this.setData({ showStatusPicker: false })
            this.loadOrder()
          } catch (e) { /* handled */ }
        } else {
          this.setData({ showStatusPicker: false })
        }
      }
    })
  },

  // ==================== 收款 ====================
  showPaymentFn() {
    if (!this.data.order || this.data.order.rawUnpaid <= 0) {
      wx.showToast({ title: '无待收金额', icon: 'none' })
      return
    }
    this.setData({
      showPaymentDialog: true,
      paymentAmount: String(this.data.order.rawUnpaid),
      paymentMethod: '现金'
    })
  },

  hidePaymentDialog() {
    this.setData({ showPaymentDialog: false })
  },

  onPaymentAmountChange(e: any) {
    this.setData({ paymentAmount: e.detail.value })
  },

  pickPaymentMethod(e: any) {
    this.setData({ paymentMethod: this.data.paymentMethods[e.currentTarget.dataset.index] })
  },

  async confirmPayment() {
    const amount = parseFloat(this.data.paymentAmount)
    if (isNaN(amount) || amount <= 0) {
      wx.showToast({ title: '请输入有效金额', icon: 'none' })
      return
    }
    if (amount > this.data.order.rawUnpaid) {
      wx.showToast({ title: '金额超过待收金额', icon: 'none' })
      return
    }

    try {
      await post(`/orders/${this.data.orderId}/payment`, {
        amount,
        paymentMethod: this.data.paymentMethod
      })
      wx.showToast({ title: '收款成功', icon: 'success' })
      this.setData({ showPaymentDialog: false })
      this.loadOrder()
    } catch (e) { /* handled */ }
  },

  // ==================== 取消订单 ====================
  cancelOrder() {
    wx.showModal({
      title: '确认取消',
      content: '确定要取消此订单吗？取消后将退回会员余额。',
      confirmColor: '#E34D59',
      success: async (res) => {
        if (res.confirm) {
          try {
            await patch(`/orders/${this.data.orderId}/status`, { status: 4 })
            wx.showToast({ title: '已取消', icon: 'success' })
            this.loadOrder()
          } catch (e) { /* handled */ }
        }
      }
    })
  },

  // ==================== 抹零 ====================
  roundOff() {
    const unpaid = this.data.order.rawUnpaid
    if (!unpaid || unpaid <= 0) {
      wx.showToast({ title: '无待收金额', icon: 'none' })
      return
    }
    wx.showModal({
      title: '确认抹零',
      content: `将待收金额 ¥${formatMoney(unpaid)} 全部抹零？`,
      success: async (res) => {
        if (res.confirm) {
          try {
            await post(`/orders/${this.data.orderId}/payment`, {
              amount: unpaid,
              paymentMethod: '抹零'
            })
            wx.showToast({ title: '已抹零', icon: 'success' })
            this.loadOrder()
          } catch (e) { /* handled */ }
        }
      }
    })
  },

  // ==================== 联系客户 ====================
  callCustomer() {
    const phone = this.data.order?.contactPhone
    if (phone) {
      wx.makePhoneCall({ phoneNumber: phone })
    } else {
      wx.showToast({ title: '暂无联系电话', icon: 'none' })
    }
  }
})
