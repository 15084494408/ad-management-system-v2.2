import { get, post } from '../../../utils/request'
import { formatMoney } from '../../../utils/helpers'

const BLACK_TIERS: number[][] = [[0,100,0.5],[100,500,0.4],[500,1000,0.3],[1000,Infinity,0.2]]
const COLOR_TIERS: number[][] = [[0,50,1.0],[50,200,0.8],[200,500,0.7],[500,1000,0.6],[1000,Infinity,0.5]]

function getPriceByTier(count: number, tiers: number[][]): number {
  // ★ 修复 P1-金额：count <= 0 时返回第一档起步价，防止匹配到最后一档
  if (count <= 0) return tiers[0][2]
  for (const [min, max, price] of tiers) {
    if (count > min && count <= max) return price
  }
  return tiers[tiers.length - 1][2]
}

Page({
  data: {
    // 客户
    customers: [] as any[],
    selectedCustomer: null as any,

    // 报价模式
    quoteMode: 'label',

    // 标签印刷
    label: { paperType: '铜版纸-128g', paperPrice: '1.2', length: '', width: '', sheets: '', totalLabels: '', printPrice: '0.1', linePrice: '', copies: '1', ratio: '100' },
    labelResult: null as any,

    // 打印报价
    print: { fileName: '', totalPages: '', blackPages: '', colorPages: '', bwCopies: '1', colorCopies: '1' },
    printResult: null as any,

    // 订单信息
    remark: '',
    discountAmount: '',
    totalAmount: '0.00',
    finalAmount: '0.00',
    submitting: false
  },

  onLoad() {
    this.loadCustomers()
  },

  async loadCustomers() {
    try {
      const res = await get<any>('/customers', { current: 1, size: 999 }, { showLoading: false })
      this.setData({ customers: res.records || [] })
    } catch (e) { console.error(e) }
  },

  pickCustomer() {
    const names = this.data.customers.map((c: any) => c.name || c.customerName)
    if (names.length === 0) {
      wx.showToast({ title: '请先创建客户', icon: 'none' }); return
    }
    wx.showActionSheet({
      itemList: names,
      success: (res) => {
        const customer = this.data.customers[res.tapIndex]
        this.setData({ selectedCustomer: customer })
      }
    })
  },

  onQuoteModeChange(e: any) { this.setData({ quoteMode: e.detail.value }) },

  onLabelChange(e: any) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`label.${field}`]: e.detail.value })
  },

  onPrintChange(e: any) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`print.${field}`]: e.detail.value })
  },

  onRemarkChange(e: any) { this.setData({ remark: e.detail.value }) },
  onDiscountChange(e: any) {
    this.setData({ discountAmount: e.detail.value })
    this.recalcTotal()
  },

  calcLabel() {
    const l = this.data.label
    const sheets = parseFloat(l.sheets) || 0
    const paperPrice = parseFloat(l.paperPrice) || 0
    const printPrice = parseFloat(l.printPrice) || 0
    const linePrice = parseFloat(l.linePrice) || 0
    const copies = parseInt(l.copies) || 1
    const ratio = (parseFloat(l.ratio) || 100) / 100

    const paperCost = parseFloat((sheets * paperPrice).toFixed(2))
    const printCost = parseFloat((sheets * printPrice * copies).toFixed(2))
    const lineCost = parseFloat((sheets * linePrice).toFixed(2))
    const total = parseFloat(((paperCost + printCost + lineCost) * ratio).toFixed(2))

    this.setData({
      labelResult: { paperCost, printCost, lineCost, total },
      totalAmount: formatMoney(total),
      finalAmount: formatMoney(Math.max(total - (parseFloat(this.data.discountAmount) || 0), 0))
    })
  },

  calcPrint() {
    const p = this.data.print
    const blackPages = parseInt(p.blackPages) || 0
    const colorPages = parseInt(p.colorPages) || 0
    const bwCopies = parseInt(p.bwCopies) || 1
    const colorCopies = parseInt(p.colorCopies) || 1

    const totalBw = blackPages * bwCopies
    const totalColor = colorPages * colorCopies
    const bwUnitPrice = getPriceByTier(totalBw, BLACK_TIERS)
    const colorUnitPrice = getPriceByTier(totalColor, COLOR_TIERS)

    const bwCost = parseFloat((totalBw * bwUnitPrice).toFixed(2))
    const colorCost = parseFloat((totalColor * colorUnitPrice).toFixed(2))
    const total = parseFloat((bwCost + colorCost).toFixed(2))

    this.setData({
      printResult: { bwCost, colorCost, totalBw, totalColor, bwUnitPrice, colorUnitPrice, total },
      totalAmount: formatMoney(total),
      finalAmount: formatMoney(Math.max(total - (parseFloat(this.data.discountAmount) || 0), 0))
    })
  },

  recalcTotal() {
    let total = 0
    if (this.data.quoteMode === 'label' && this.data.labelResult) {
      total = this.data.labelResult.total
    } else if (this.data.quoteMode === 'print' && this.data.printResult) {
      total = this.data.printResult.total
    }
    const discount = parseFloat(this.data.discountAmount) || 0
    this.setData({ finalAmount: formatMoney(Math.max(total - discount, 0)) })
  },

  async submit() {
    if (!this.data.selectedCustomer) { wx.showToast({ title: '请选择客户', icon: 'none' }); return }
    if (this.data.quoteMode === 'label' && !this.data.labelResult) { wx.showToast({ title: '请先计算报价', icon: 'none' }); return }
    if (this.data.quoteMode === 'print' && !this.data.printResult) { wx.showToast({ title: '请先计算报价', icon: 'none' }); return }

    this.setData({ submitting: true })
    try {
      let materials: any[] = []
      let totalAmount = 0

      if (this.data.quoteMode === 'label') {
        const r = this.data.labelResult
        totalAmount = r.total
        materials = [
          { materialName: `纸张(${this.data.label.paperType})`, quantity: this.data.label.sheets || 1, unitPrice: parseFloat(this.data.label.paperPrice) || 0, amount: r.paperCost },
          { materialName: '印刷费', quantity: (parseInt(this.data.label.sheets) || 1) * (parseInt(this.data.label.copies) || 1), unitPrice: parseFloat(this.data.label.printPrice) || 0, amount: r.printCost },
        ]
        if (r.lineCost > 0) materials.push({ materialName: '划线费', quantity: this.data.label.sheets || 1, unitPrice: parseFloat(this.data.label.linePrice) || 0, amount: r.lineCost })
      } else {
        const r = this.data.printResult
        totalAmount = r.total
        if (r.bwCost > 0) materials.push({ materialName: '黑白打印', quantity: r.totalBw, unitPrice: r.bwUnitPrice, amount: r.bwCost })
        if (r.colorCost > 0) materials.push({ materialName: '彩色打印', quantity: r.totalColor, unitPrice: r.colorUnitPrice, amount: r.colorCost })
      }

      await post('/orders', {
        customerId: this.data.selectedCustomer.id,
        customerName: this.data.selectedCustomer.name,
        title: `${this.data.selectedCustomer.name} - ${this.data.quoteMode === 'label' ? '标签印刷' : '打印'}订单`,
        remark: this.data.remark || null,
        discountAmount: parseFloat(this.data.discountAmount) || 0,
        totalAmount,
        materials
      })
      wx.showToast({ title: '下单成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    } catch (e) { /* handled */ }
    finally { this.setData({ submitting: false }) }
  }
})
