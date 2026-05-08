"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../utils/request");
const helpers_1 = require("../../utils/helpers");
Page({
    data: {
        activeTab: 'all',
        allItems: [],
        displayItems: [],
        loading: false,
        counts: { todo: 0, order: 0, bill: 0 }
    },
    onLoad() { this.loadData(); },
    onShow() { this.loadData(); },
    onPullDownRefresh() { this.loadData().then(() => wx.stopPullDownRefresh()); },
    onTabChange(e) {
        var _a, _b, _c;
        const value = ((_a = e.detail) === null || _a === void 0 ? void 0 : _a.value) || ((_c = (_b = e.currentTarget) === null || _b === void 0 ? void 0 : _b.dataset) === null || _c === void 0 ? void 0 : _c.value) || 'all';
        this.setData({ activeTab: value });
        this.filterItems();
    },
    async loadData() {
        this.setData({ loading: true });
        try {
            const [todoItems, orderItems, billItems] = await Promise.all([
                this.loadTodos(),
                this.loadPendingOrders(),
                this.loadPendingBills()
            ]);
            const all = [...todoItems, ...orderItems, ...billItems];
            // 按优先级排序：非常紧急 > 紧急 > 普通，同优先级按时间倒序
            all.sort((a, b) => {
                const pa = (4 - (a.priority || 1)) * 10000 + (a.rawData.createTime ? new Date(a.rawData.createTime).getTime() : 0);
                const pb = (4 - (b.priority || 1)) * 10000 + (b.rawData.createTime ? new Date(b.rawData.createTime).getTime() : 0);
                return pb - pa;
            });
            this.setData({
                allItems: all,
                counts: {
                    todo: todoItems.length,
                    order: orderItems.length,
                    bill: billItems.length
                }
            });
            this.filterItems();
        }
        catch (e) {
            console.error(e);
        }
        finally {
            this.setData({ loading: false });
        }
    },
    filterItems() {
        const tab = this.data.activeTab;
        const items = tab === 'all'
            ? this.data.allItems
            : this.data.allItems.filter((i) => i.type === tab);
        this.setData({ displayItems: items });
    },
    /** 需求待办：GET /todo/list */
    async loadTodos() {
        try {
            const res = await (0, request_1.get)('/todo/list', null, { showLoading: false });
            const data = Array.isArray(res) && res.length > 0 ? res[0] : res;
            const items = data.items || [];
            return items
                .filter((t) => t.status < 4) // 排除已完成的
                .map((t) => {
                const statusMap = { 1: '新收集', 2: '分析中', 3: '待确认' };
                const priorityMap = { 1: '普通', 2: '紧急', 3: '非常紧急' };
                return {
                    id: t.id,
                    type: 'todo',
                    title: t.requirements || '待办事项',
                    subtitle: [t.customerName, t.contactPhone].filter(Boolean).join(' · ') || t.source || '',
                    amount: '',
                    status: t.status,
                    statusLabel: statusMap[t.status] || '未知',
                    priority: t.priority || 1,
                    priorityLabel: priorityMap[t.priority] || '普通',
                    tags: ['需求'],
                    createTime: (0, helpers_1.formatDateTime)(t.createTime),
                    rawData: t
                };
            });
        }
        catch (e) {
            console.error(e);
            return [];
        }
    },
    /** 待处理订单：GET /orders?status=1 */
    async loadPendingOrders() {
        try {
            const res = await (0, request_1.get)('/orders', { status: 1, current: 1, size: 20 }, { showLoading: false });
            const records = res.records || [];
            return records.map((o) => {
                const priorityMap = { 1: '普通', 2: '紧急', 3: '加急' };
                return {
                    id: o.id,
                    type: 'order',
                    title: o.title || o.orderNo,
                    subtitle: o.customerName || '散客',
                    amount: '¥' + (o.totalAmount || 0),
                    status: o.status,
                    statusLabel: '待处理',
                    priority: o.priority || 1,
                    priorityLabel: priorityMap[o.priority] || '普通',
                    tags: ['订单'],
                    createTime: (0, helpers_1.formatDateTime)(o.createTime),
                    rawData: o
                };
            });
        }
        catch (e) {
            console.error(e);
            return [];
        }
    },
    /** 未对账账单：GET /factory/bills?status=1 */
    async loadPendingBills() {
        try {
            const res = await (0, request_1.get)('/factory/bills', { status: 1, current: 1, size: 20 }, { showLoading: false });
            const records = res.records || [];
            return records.map((b) => ({
                id: b.id,
                type: 'bill',
                title: b.billNo || '账单',
                subtitle: (b.billType === 1 ? '工厂' : '客户') + '账单 · ' + (b.factoryName || ''),
                amount: '¥' + (b.totalAmount || 0),
                status: b.status,
                statusLabel: '未对账',
                priority: 1,
                priorityLabel: '普通',
                tags: ['账单', b.billType === 1 ? '工厂' : '客户'],
                createTime: (0, helpers_1.formatDateTime)(b.createTime),
                rawData: b
            }));
        }
        catch (e) {
            console.error(e);
            return [];
        }
    },
    goCreate() {
        wx.navigateTo({ url: '/pages/todo-create/todo-create' });
    },
    /** 需求待办操作：推进 */
    updateTodoStatus(e) {
        const id = e.currentTarget.dataset.id;
        wx.showActionSheet({
            itemList: ['标记为分析中', '标记为待确认', '标记为已转订单'],
            success: async (res) => {
                const statuses = [2, 3, 4];
                const labels = ['分析中', '待确认', '已转订单'];
                try {
                    await (0, request_1.put)(`/todo/${id}`, { status: statuses[res.tapIndex] });
                    wx.showToast({ title: `已标记为${labels[res.tapIndex]}`, icon: 'success' });
                    this.loadData();
                }
                catch (e) { /* handled */ }
            }
        });
    },
    /** 需求待办：完成 */
    completeTodo(e) {
        const id = e.currentTarget.dataset.id;
        wx.showModal({
            title: '确认',
            content: '确认标记为已转订单？',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await (0, request_1.put)(`/todo/${id}`, { status: 4 });
                        wx.showToast({ title: '操作成功', icon: 'success' });
                        this.loadData();
                    }
                    catch (e) { /* handled */ }
                }
            }
        });
    },
    /** 需求待办：删除 */
    deleteTodo(e) {
        const id = e.currentTarget.dataset.id;
        wx.showModal({
            title: '确认删除',
            content: '确定要删除此待办事项？',
            confirmColor: '#E34D59',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await (0, request_1.del)(`/todo/${id}`);
                        wx.showToast({ title: '已删除', icon: 'success' });
                        this.loadData();
                    }
                    catch (e) { /* handled */ }
                }
            }
        });
    },
    /** 订单：进入详情 */
    goOrderDetail(e) {
        const id = e.currentTarget.dataset.id;
        wx.navigateTo({ url: `/pages/orders/detail/detail?id=${id}` });
    },
    /** 账单：进入详情 */
    goBillDetail(e) {
        const id = e.currentTarget.dataset.id;
        const billType = e.currentTarget.dataset.billtype;
        const page = billType === 1 ? 'factory-bills' : 'customer-bills';
        wx.navigateTo({ url: `/pages/customer/bills/bill-detail?id=${id}&type=${page}` });
    }
});
