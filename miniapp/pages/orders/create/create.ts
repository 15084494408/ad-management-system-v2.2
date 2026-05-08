import { post, get } from '../../../utils/request'
import { formatMoney } from '../../../utils/helpers'

interface SelectedMaterial {
  materialId?: number
  materialName: string
  spec: string
  unit: string
  quantity: number
  unitPrice: number
  unitCost: number
  amount: number
  pricingType: number
  width?: number
  height?: number
  isCustom: boolean
}

Page({
  data: {
    // 表单
    form: {
      customerId: 0,
      customerName: '',
      title: '',
      orderType: 1,
      contactPerson: '',
      contactPhone: '',
      deliveryDate: '',
      deliveryAddress: '',
      priority: 1,
      designerId: null as number | null,
      remark: '',
      discountAmount: ''
    },
    // 客户列表
    customers: [] as any[],
    customerSearch: '',
    filteredCustomers: [] as any[],
    showCustomerPicker: false,
    // 设计师列表
    designers: [] as any[],
    // 物料库
    allMaterials: [] as any[],
    filteredMaterials: [] as any[],
    materialKeyword: '',
    materialCategoryFilter: 0,
    categories: [] as any[],
    showMaterialPicker: false,
    // 已选物料
    materials: [] as SelectedMaterial[],
    totalAmount: '0.00',
    finalAmount: '0.00',
    // 提交
    submitting: false
  },

  onLoad() {
    this.loadData()
  },

  async loadData() {
    await Promise.all([
      this.loadCustomers(),
      this.loadDesigners(),
      this.loadMaterials(),
      this.loadCategories()
    ])
  },

  // ==================== 客户选择 ====================
  async loadCustomers() {
    try {
      const res = await get<any>('/customers', { current: 1, size: 999 }, { showLoading: false })
      const list = (res.records || []).map((c: any) => ({
        ...c,
        displayName: c.name || c.customerName || '',
        typeLabel: c.customerType === 2 ? '工厂' : c.customerType === 3 ? '零售' : '普通'
      }))
      this.setData({ customers: list, filteredCustomers: list })
    } catch (e) {
      console.error('加载客户列表失败', e)
    }
  },

  async loadDesigners() {
    try {
      const res = await get<any>('/system/users', { current: 1, size: 999, designer: 1 }, { showLoading: false })
      this.setData({ designers: (res.records || []).map((u: any) => ({
        id: u.id,
        displayName: u.realName || u.username
      })) })
    } catch (e) { console.error(e) }
  },

  async loadMaterials() {
    try {
      const res = await get<any>('/material/all', null, { showLoading: false })
      const list = Array.isArray(res) ? res : (res.records || [])
      this.setData({ allMaterials: list, filteredMaterials: list })
    } catch (e) {
      console.error('加载物料列表失败', e)
    }
  },

  async loadCategories() {
    try {
      const res = await get<any>('/material/category', null, { showLoading: false })
      this.setData({
        categories: [{ id: 0, name: '全部' }, ...(Array.isArray(res) ? res : [])]
      })
    } catch (e) { console.error(e) }
  },

  // ==================== 客户搜索/选择 ====================
  onCustomerSearch(e: any) {
    const keyword = e.detail.value.toLowerCase()
    this.setData({ customerSearch: keyword })
    if (!keyword) {
      this.setData({ filteredCustomers: this.data.customers })
      return
    }
    this.setData({
      filteredCustomers: this.data.customers.filter((c: any) =>
        (c.displayName || '').toLowerCase().includes(keyword) ||
        (c.phone || '').includes(keyword)
      )
    })
  },

  showCustomerPickerFn() {
    this.setData({ showCustomerPicker: true, customerSearch: '', filteredCustomers: this.data.customers })
  },

  hideCustomerPicker() {
    this.setData({ showCustomerPicker: false })
  },

  selectCustomer(e: WechatMiniprogram.TouchEvent) {
    const idx = e.currentTarget.dataset.index
    const customer = this.data.filteredCustomers[idx]
    this.setData({
      'form.customerId': customer.id,
      'form.customerName': customer.displayName,
      'form.title': `${customer.displayName} - 新订单`,
      'form.contactPerson': customer.contactPerson || '',
      'form.contactPhone': customer.phone || '',
      showCustomerPicker: false
    })
  },

  // ==================== 物料搜索/选择 ====================
  onMaterialSearch(e: any) {
    const keyword = e.detail.value.toLowerCase()
    this.setData({ materialKeyword: keyword })
    this.filterMaterials()
  },

  onCategoryFilter(e: any) {
    this.setData({ materialCategoryFilter: e.currentTarget.dataset.id })
    this.filterMaterials()
  },

  filterMaterials() {
    const { allMaterials, materialKeyword, materialCategoryFilter } = this.data
    let list = allMaterials
    if (materialKeyword) {
      const kw = materialKeyword.toLowerCase()
      list = list.filter((m: any) =>
        (m.name || '').toLowerCase().includes(kw) ||
        (m.code || '').toLowerCase().includes(kw) ||
        (m.spec || '').toLowerCase().includes(kw)
      )
    }
    if (materialCategoryFilter > 0) {
      list = list.filter((m: any) => m.categoryId === materialCategoryFilter)
    }
    this.setData({ filteredMaterials: list })
  },

  showMaterialPickerFn() {
    this.setData({
      showMaterialPicker: true,
      materialKeyword: '',
      materialCategoryFilter: 0,
      filteredMaterials: this.data.allMaterials
    })
  },

  hideMaterialPicker() {
    this.setData({ showMaterialPicker: false })
  },

  /** 从物料库添加物料 */
  addFromLibrary(e: WechatMiniprogram.TouchEvent) {
    const idx = e.currentTarget.dataset.index
    const m = this.data.filteredMaterials[idx]
    // 检查是否已添加
    if (this.data.materials.find((s: any) => s.materialId === m.id && !s.isCustom)) {
      wx.showToast({ title: '该物料已添加', icon: 'none' })
      return
    }
    const newItem: SelectedMaterial = {
      materialId: m.id,
      materialName: m.name,
      spec: m.spec || '',
      unit: m.unit || '个',
      quantity: 1,
      unitPrice: m.price || 0,
      unitCost: m.unitCost || 0,
      amount: m.price || 0,
      pricingType: m.pricingType || 0,
      width: 0,
      height: 0,
      isCustom: false
    }
    this.setData({
      materials: [...this.data.materials, newItem],
      showMaterialPicker: false
    })
    this.recalcTotal()
  },

  /** 手动添加自定义物料 */
  addCustomMaterial() {
    const newItem: SelectedMaterial = {
      materialId: undefined,
      materialName: '',
      spec: '',
      unit: '个',
      quantity: 1,
      unitPrice: 0,
      unitCost: 0,
      amount: 0,
      pricingType: 0,
      isCustom: true
    }
    this.setData({ materials: [...this.data.materials, newItem] })
  },

  removeMaterial(e: WechatMiniprogram.TouchEvent) {
    const idx = e.currentTarget.dataset.index
    const materials = [...this.data.materials]
    materials.splice(idx, 1)
    this.setData({ materials })
    this.recalcTotal()
  },

  onMaterialFieldChange(e: any) {
    const { index, field } = e.currentTarget.dataset
    let value: any = e.detail.value
    if (['quantity', 'unitPrice', 'unitCost', 'width', 'height'].includes(field)) {
      value = parseFloat(value) || 0
    }
    const key = `materials[${index}].${field}`
    this.setData({ [key]: value })
    if (['quantity', 'unitPrice', 'width', 'height'].includes(field)) {
      this.recalcLine(index)
    }
  },

  recalcLine(index: number) {
    const materials = this.data.materials
    const m = materials[index]
    if (m.pricingType === 1) {
      // 按面积计价：数量 × (宽cm/100 × 高cm/100) × 单价
      const qty = Math.max(1, m.quantity || 1)
      const w = Math.max(0, m.width || 0) / 100
      const h = Math.max(0, m.height || 0) / 100
      const area = w * h * qty
      m.amount = parseFloat((area * m.unitPrice).toFixed(2))
    } else {
      m.amount = parseFloat((Math.max(1, m.quantity || 1) * m.unitPrice).toFixed(2))
    }
    const key = `materials[${index}].amount`
    this.setData({ [key]: m.amount, materials })
    this.recalcTotal()
  },

  // ==================== 数量增减 ====================
  qtyDec(e: WechatMiniprogram.TouchEvent) {
    const idx = e.currentTarget.dataset.index
    const key = `materials[${idx}].quantity`
    this.setData({ [key]: Math.max(1, this.data.materials[idx].quantity - 1) })
    this.recalcLine(idx)
  },

  qtyInc(e: WechatMiniprogram.TouchEvent) {
    const idx = e.currentTarget.dataset.index
    const key = `materials[${idx}].quantity`
    this.setData({ [key]: this.data.materials[idx].quantity + 1 })
    this.recalcLine(idx)
  },

  // ==================== 金额计算 ====================
  onDiscountChange(e: any) {
    this.setData({ 'form.discountAmount': e.detail.value })
    this.recalcTotal()
  },

  recalcTotal() {
    let total = 0
    this.data.materials.forEach(m => { total += m.amount })
    const discount = parseFloat(this.data.form.discountAmount) || 0
    const finalVal = Math.max(total - discount, 0)
    this.setData({
      totalAmount: formatMoney(total),
      finalAmount: formatMoney(finalVal)
    })
  },

  // ==================== 表单字段 ====================
  onFieldChange(e: any) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  onOrderTypeChange(e: any) {
    this.setData({ 'form.orderType': parseInt(e.detail.value) || 1 })
  },

  onPriorityChange(e: any) {
    this.setData({ 'form.priority': parseInt(e.detail.value) || 1 })
  },

  onDesignerChange(e: any) {
    const idx = e.currentTarget.dataset.index
    if (idx >= 0 && this.data.designers[idx]) {
      this.setData({ 'form.designerId': this.data.designers[idx].id })
    }
  },

  onDateChange(e: any) {
    this.setData({ 'form.deliveryDate': e.detail.value })
  },

  // ==================== 提交订单 ====================
  async submit() {
    const { form, materials } = this.data
    if (!form.customerId) { wx.showToast({ title: '请选择客户', icon: 'none' }); return }
    if (!form.title.trim()) { wx.showToast({ title: '请输入订单标题', icon: 'none' }); return }
    if (materials.length === 0) { wx.showToast({ title: '请添加物料', icon: 'none' }); return }
    const invalidMat = materials.find(m => !m.materialName.trim())
    if (invalidMat) { wx.showToast({ title: '请填写物料名称', icon: 'none' }); return }

    this.setData({ submitting: true })
    try {
      let total = 0
      materials.forEach(m => { total += m.amount })

      await post('/orders', {
        customerId: form.customerId,
        customerName: form.customerName,
        title: form.title.trim(),
        orderType: form.orderType,
        contactPerson: form.contactPerson || null,
        contactPhone: form.contactPhone || null,
        deliveryDate: form.deliveryDate || null,
        deliveryAddress: form.deliveryAddress || null,
        priority: form.priority,
        designerId: form.designerId || null,
        remark: form.remark || null,
        discountAmount: parseFloat(form.discountAmount) || 0,
        totalAmount: total,
        materials: materials.map(m => ({
          materialId: m.materialId || null,
          materialName: m.materialName,
          spec: m.spec || null,
          unit: m.unit || '个',
          quantity: m.quantity,
          unitPrice: m.unitPrice,
          unitCost: m.unitCost || 0,
          amount: m.amount
        }))
      })
      wx.showToast({ title: '创建成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    } catch (e) { /* handled */ }
    finally { this.setData({ submitting: false }) }
  }
})
