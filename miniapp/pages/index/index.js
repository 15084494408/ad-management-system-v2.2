"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../utils/request");
const auth_1 = require("../../utils/auth");
const helpers_1 = require("../../utils/helpers");
Page({
    data: {
        userInfo: {},
        todayStr: '',
        stats: null,
        orders: [],
        todos: [],
        orderLoading: false
    },
    onLoad() {
        if (!(0, auth_1.requireAuth)())
            return;
        this.setData({
            userInfo: (0, auth_1.getUserInfo)() || {},
            todayStr: this.getTodayStr()
        });
        this.loadData();
    },
    onShow() {
        if ((0, auth_1.requireAuth)()) {
            this.loadData();
        }
    },
    onPullDownRefresh() {
        this.loadData().then(() => wx.stopPullDownRefresh());
    },
    async loadData() {
        await Promise.all([
            this.loadStats(),
            this.loadOrders(),
            this.loadTodos()
        ]);
    },
    /** 加载统计数据：调用 /dashboard/stats 和 /dashboard/board */
    async loadStats() {
        var _a;
        try {
            const [stats, board] = await Promise.all([
                (0, request_1.get)('/dashboard/stats', null, { showLoading: false }),
                (0, request_1.get)('/dashboard/board', null, { showLoading: false })
            ]);
            // 从 stats 取今日数据，从 board 取待处理/待收
            this.setData({
                stats: {
                    todayOrders: stats.todayOrders || 0,
                    todayRevenue: stats.todayRevenue || 0,
                    pendingOrders: board.unfinishedOrders ? board.unfinishedOrders.length : 0,
                    unpaidAmount: ((_a = board.kpi) === null || _a === void 0 ? void 0 : _a.unpaidAmount) || 0
                }
            });
        }
        catch (e) {
            console.error('加载统计数据失败', e);
            wx.showToast({ title: '加载统计失败', icon: 'none' });
        }
    },
    /** 加载最近订单 */
    async loadOrders() {
        this.setData({ orderLoading: true });
        try {
            const res = await (0, request_1.get)('/orders', { current: 1, size: 5 }, { showLoading: false });
            const records = (res.records || []).map((item) => (Object.assign(Object.assign({}, item), { totalAmount: '¥' + (0, helpers_1.formatMoney)(item.totalAmount), unpaidAmount: Math.max((item.totalAmount || 0) - (item.paidAmount || 0) - (item.discountAmount || 0) - (item.roundingAmount || 0), 0), statusText: helpers_1.ORDER_STATUS[item.status] || '未知', statusColor: helpers_1.ORDER_STATUS_COLOR[item.status] || 'default', createTime: (0, helpers_1.timeAgo)(item.createTime) })));
            this.setData({ orders: records });
        }
        catch (e) {
            console.error('加载订单失败', e);
        }
        finally {
            this.setData({ orderLoading: false });
        }
    },
    /** 加载待办事项：调用 /todo/list */
    async loadTodos() {
        try {
            const res = await (0, request_1.get)('/todo/list', null, { showLoading: false });
            // 后端返回 [{items: [...], statusCount: {...}}]
            const items = (Array.isArray(res) && res.length > 0) ? res[0].items || [] : [];
            const todos = items.slice(0, 5).map((item) => ({
                id: item.id,
                title: item.requirements || item.customerName || '待办事项',
                content: item.requirements || '',
                done: item.status === 4,
                createTime: (0, helpers_1.timeAgo)(item.createTime)
            }));
            this.setData({ todos });
        }
        catch (e) {
            console.error('加载待办失败', e);
        }
    },
    getTodayStr() {
        const d = new Date();
        const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
        return `${d.getMonth() + 1}月${d.getDate()}日 星期${weekDays[d.getDay()]}`;
    },
    createOrder() { wx.navigateTo({ url: '/pages/orders/create/create' }); },
    createQuote() { wx.navigateTo({ url: '/pages/orders/quote/quote' }); },
    addCustomer() { wx.navigateTo({ url: '/pages/customer/create/create' }); },
    goOrders() { wx.switchTab({ url: '/pages/orders/orders' }); },
    goOrderDetail(e) {
        const id = e.currentTarget.dataset.id;
        wx.navigateTo({ url: `/pages/orders/detail/detail?id=${id}` });
    },
    goMessages() { wx.switchTab({ url: '/pages/messages/messages' }); },
    goTodo() { wx.navigateTo({ url: '/pages/todo/todo' }); },
    onTodo() { wx.navigateTo({ url: '/pages/todo/todo' }); },
    goMemberList() { wx.navigateTo({ url: '/pages/member/member' }); },
    goMaterial() { wx.navigateTo({ url: '/pages/material/material' }); },
    goStockIn() { wx.navigateTo({ url: '/pages/material/stock-in' }); },
    goStockOut() { wx.navigateTo({ url: '/pages/material/stock-out' }); },
    goCustomerBills() { wx.navigateTo({ url: '/pages/customer/bills/customer-bills' }); },
    goFactoryBills() { wx.navigateTo({ url: '/pages/customer/bills/factory-bills' }); },
    goUsers() { wx.navigateTo({ url: '/pages/system/users' }); },
    goCompany() { wx.navigateTo({ url: '/pages/system/company' }); }
});
