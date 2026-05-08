import { get, put } from '../../../utils/request'
import { formatMoney, formatDate } from '../../../utils/helpers'

Page({
  data: {
    billId: 0,
    bill: null as any,
    details: [] as any[],
    loading: true
  },

  onLoad(options: any) {
    this.setData({ billId: parseInt(options.id) })
    this.loadBill()
    this.loadDetails()
  },

  async loadBill() {
    try {
      const bill = await get<any>(`/factory/bills/${this.data.billId}`)
      const statusText = ({ 0: '未付', 1: '部分付', 2: '已付清', 3: '已取消' } as Record<number, string>)[bill.paymentStatus] || '未知'
      this.setData({
        bill: {
          ...bill,
          amountStr: formatMoney(bill.totalAmount || bill.amount || 0),
          paidStr: formatMoney(bill.paidAmount || 0),
          unpaidStr: formatMoney(Math.max((bill.totalAmount || bill.amount || 0) - (bill.paidAmount || 0), 0)),
          statusText,
          createTime: formatDate(bill.createTime)
        }
      })
    } catch (e) { console.error(e) }
    finally { this.setData({ loading: false }) }
  },

  async loadDetails() {
    try {
      const res = await get<any>(`/factory/bills/${this.data.billId}/details`)
      this.setData({
        details: (res || []).map((d: any) => ({
          ...d,
          amountStr: formatMoney(d.amount || d.unitPrice * d.quantity || 0),
          unitPriceStr: formatMoney(d.unitPrice || 0)
        }))
      })
    } catch (e) { console.error(e) }
  },

  addPayment() {
    const unpaid = (this.data.bill?.totalAmount || 0) - (this.data.bill?.paidAmount || 0)
    if (unpaid <= 0) { wx.showToast({ title: '无待付金额', icon: 'none' }); return }
    wx.showModal({
      title: '录入付款',
      content: `待付金额：¥${formatMoney(unpaid)}`,
      editable: true,
      placeholderText: '请输入付款金额',
      success: async (res) => {
        if (res.confirm && res.content) {
          const amount = parseFloat(res.content)
          if (isNaN(amount) || amount <= 0) { wx.showToast({ title: '请输入有效金额', icon: 'none' }); return }
          // ★ 修复 P1-金额：禁止超额付款
          if (amount > unpaid) { wx.showToast({ title: `金额超出待付金额 ¥${formatMoney(unpaid)}`, icon: 'none' }); return }
          try {
            await put(`/factory/bills/${this.data.billId}/paid`, { amount })
            wx.showToast({ title: '付款成功', icon: 'success' })
            this.loadBill()
          } catch (e) { /* handled */ }
        }
      }
    })
  }
})
