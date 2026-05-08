import { get } from '../../utils/request'
import { getUserInfo, requireAuth } from '../../utils/auth'
import { ORDER_STATUS, ORDER_STATUS_COLOR, formatMoney, formatDate, timeAgo } from '../../utils/helpers'

interface Stats {
  todayOrders: number
  todayRevenue: number
  pendingOrders: number
  unpaidAmount: number
}

interface OrderItem {
  id: number
  orderNo: string
  customerName: string
  totalAmount: number
  paidAmount: number
  status: number
  createTime: string
  statusText: string
  statusColor: string
  unpaidAmount: number
}

interface TodoItem {
  id: number
  title: string
  content: string
  done: boolean
  createTime: string
}

interface NoticeItem {
  id: number
  title: string
  content: string
  type: number
  createTime: string
}

Page({
  data: {
    userInfo: {} as any,
    todayStr: '',
    loading: true,
    stats: null as Stats | null,
    orders: [] as OrderItem[],
    todos: [] as TodoItem[],
    notices: [] as NoticeItem[],
    orderLoading: false,
    unreadCount: 0
  },

  onLoad() {
    if (!requireAuth()) return
    this.setData({
      userInfo: getUserInfo() || {},
      todayStr: this.getTodayStr()
    })
    this.loadData()
  },

  onShow() {
    if (requireAuth()) {
      this.loadData()
    }
  },

  onPullDownRefresh() {
    this.loadData().then(() => wx.stopPullDownRefresh())
  },

  async loadData() {
    this.setData({ loading: true })
    await Promise.all([
      this.loadStats(),
      this.loadOrders(),
      this.loadTodos(),
      this.loadNotices(),
      this.loadUnreadCount()
    ])
    this.setData({ loading: false })
  },

  /** 加载未读数 */
  async loadUnreadCount() {
    try {
      const res = await get<any>('/notice/unread-count', null, { showLoading: false })
      this.setData({ unreadCount: res.count || 0 })
    } catch (e) { /* ignore */ }
  },

  /** 加载统计数据 */
  async loadStats() {
    try {
      const [stats, board] = await Promise.all([
        get<any>('/dashboard/stats', null, { showLoading: false }),
        get<any>('/dashboard/board', null, { showLoading: false })
      ])
      // 尝试加载财务概览
      let financeData: any = {}
      try {
        financeData = await get<any>('/finance/overview', null, { showLoading: false })
      } catch (e) { /* finance module may not exist */ }

      this.setData({
        stats: {
          todayOrders: stats.todayOrders || 0,
          todayRevenue: stats.todayRevenue || 0,
          pendingOrders: board.unfinishedOrders ? board.unfinishedOrders.length : 0,
          unpaidAmount: board.kpi?.unpaidAmount || financeData?.unpaidAmount || 0
        }
      })
    } catch (e) {
      console.error('加载统计数据失败', e)
      wx.showToast({ title: '加载统计失败', icon: 'none' })
    }
  },

  /** 加载最近订单 */
  async loadOrders() {
    this.setData({ orderLoading: true })
    try {
      const res = await get<any>('/orders', { current: 1, size: 5 }, { showLoading: false })
      const records = (res.records || []).map((item: any) => ({
        ...item,
        totalAmount: '¥' + formatMoney(item.totalAmount),
        unpaidAmount: Math.max((item.totalAmount || 0) - (item.paidAmount || 0) - (item.discountAmount || 0) - (item.roundingAmount || 0), 0),
        statusText: ORDER_STATUS[item.status] || '未知',
        statusColor: ORDER_STATUS_COLOR[item.status] || 'default',
        createTime: timeAgo(item.createTime)
      }))
      this.setData({ orders: records })
    } catch (e) {
      console.error('加载订单失败', e)
    } finally {
      this.setData({ orderLoading: false })
    }
  },

  /** 加载待办概览 */
  async loadTodos() {
    try {
      const [todoRes, orderRes, billRes] = await Promise.all([
        get<any>('/todo/list', null, { showLoading: false }),
        get<any>('/orders', { status: 1, current: 1, size: 3 }, { showLoading: false }),
        get<any>('/factory/bills', { status: 1, current: 1, size: 3 }, { showLoading: false })
      ])
      const todoItems = (Array.isArray(todoRes) && todoRes.length > 0 ? todoRes[0].items || [] : [])
        .filter((t: any) => t.status < 4)
        .slice(0, 3)
        .map((item: any) => ({
          id: item.id,
          type: 'todo',
          title: item.requirements || '待办事项',
          subtitle: item.customerName || '',
          createTime: timeAgo(item.createTime)
        }))
      const orderItems = (orderRes.records || []).slice(0, 3).map((o: any) => ({
        id: o.id,
        type: 'order',
        title: o.title || o.orderNo,
        subtitle: o.customerName || '散客',
        amount: '¥' + (o.totalAmount || 0),
        createTime: timeAgo(o.createTime)
      }))
      const billItems = (billRes.records || []).slice(0, 3).map((b: any) => ({
        id: b.id,
        type: 'bill',
        title: b.billNo || '账单',
        subtitle: (b.billType === 1 ? '工厂' : '客户') + '账单',
        amount: '¥' + (b.totalAmount || 0),
        createTime: timeAgo(b.createTime)
      }))
      this.setData({ todos: [...todoItems, ...orderItems, ...billItems].slice(0, 5) })
    } catch (e) {
      console.error('加载待办失败', e)
    }
  },

  /** 加载系统公告 */
  async loadNotices() {
    try {
      const res = await get<any>('/notice', { current: 1, size: 5 }, { showLoading: false })
      this.setData({
        notices: (res.records || []).map((n: any) => ({
          id: n.id,
          title: n.title,
          content: n.content,
          type: n.type,
          createTime: timeAgo(n.createTime)
        }))
      })
    } catch (e) {
      console.error('加载公告失败', e)
    }
  },

  readNotice(e: WechatMiniprogram.TouchEvent) {
    const item = e.currentTarget.dataset.item
    if (item.content) {
      wx.showModal({
        title: item.title,
        content: item.content,
        showCancel: false
      })
    }
  },

  getTodayStr(): string {
    const d = new Date()
    const weekDays = ['日', '一', '二', '三', '四', '五', '六']
    return `${d.getMonth() + 1}月${d.getDate()}日 星期${weekDays[d.getDay()]}`
  },

  createOrder() { wx.navigateTo({ url: '/pages/orders/create/create' }) },
  createQuote() { wx.navigateTo({ url: '/pages/orders/quote/quote' }) },
  addCustomer() { wx.navigateTo({ url: '/pages/customer/create/create' }) },
  goOrders() { wx.switchTab({ url: '/pages/orders/orders' }) },

  goOrderDetail(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/orders/detail/detail?id=${id}` })
  },

  goMessages() { wx.navigateTo({ url: '/pages/messages/messages' }) },
  goTodo() { wx.navigateTo({ url: '/pages/todo/todo' }) },
  onTodo() { wx.navigateTo({ url: '/pages/todo/todo' }) },
  goMemberList() { wx.navigateTo({ url: '/pages/member/member' }) },
  goMaterial() { wx.navigateTo({ url: '/pages/material/material' }) },
  goStockIn() { wx.navigateTo({ url: '/pages/material/stock-in' }) },
  goStockOut() { wx.navigateTo({ url: '/pages/material/stock-out' }) },
  goCustomerBills() { wx.navigateTo({ url: '/pages/customer/bills/customer-bills' }) },
  goFactoryBills() { wx.navigateTo({ url: '/pages/customer/bills/factory-bills' }) },
  goFinance() { wx.showToast({ title: '即将开放，敬请期待', icon: 'none' }) },
  goDesignFiles() { wx.showToast({ title: '即将开放，敬请期待', icon: 'none' }) },
  goUsers() { wx.navigateTo({ url: '/pages/system/users' }) },
  goCompany() { wx.navigateTo({ url: '/pages/system/company' }) }
})
