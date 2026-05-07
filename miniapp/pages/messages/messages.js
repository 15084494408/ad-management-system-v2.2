"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../utils/request");
const helpers_1 = require("../../utils/helpers");
Page({
    data: {
        activeTab: 'todo',
        todos: [],
        notices: [],
        loading: false
    },
    onLoad() { this.loadData(); },
    onShow() { this.loadData(); },
    onTabChange(e) {
        this.setData({ activeTab: e.detail.value });
        this.loadData();
    },
    async loadData() {
        this.setData({ loading: true });
        if (this.data.activeTab === 'todo') {
            await this.loadTodos();
        }
        else {
            await this.loadNotices();
        }
        this.setData({ loading: false });
    },
    /** 加载待办：GET /todo/list */
    async loadTodos() {
        try {
            const res = await (0, request_1.get)('/todo/list', null, { showLoading: false });
            // 后端返回 [{ items: [...], statusCount: {...} }]
            const items = (Array.isArray(res) && res.length > 0) ? res[0].items || [] : res.items || [];
            this.setData({
                todos: items.map((t) => ({
                    id: t.id,
                    title: t.requirements || t.customerName || '待办事项',
                    content: t.requirements || '',
                    status: t.status, // 1=新收集 2=分析中 3=待确认 4=已转订单
                    statusLabel: t.statusLabel || '',
                    priority: t.priority || 0,
                    customerName: t.customerName || '',
                    contactPhone: t.contactPhone || '',
                    source: t.source || '',
                    createTime: (0, helpers_1.timeAgo)(t.createTime)
                }))
            });
        }
        catch (e) {
            console.error(e);
        }
    },
    /** 加载公告：GET /notice */
    async loadNotices() {
        try {
            const res = await (0, request_1.get)('/notice', { current: 1, size: 50 }, { showLoading: false });
            this.setData({
                notices: (res.records || []).map((n) => ({
                    id: n.id,
                    title: n.title,
                    content: n.content,
                    type: n.type,
                    createTime: (0, helpers_1.timeAgo)(n.createTime)
                }))
            });
        }
        catch (e) {
            console.error(e);
        }
    },
    /** 完成待办：PATCH /todo/{id}/status */
    toggleTodo(e) {
        const id = e.currentTarget.dataset.id;
        wx.showModal({
            title: '确认完成',
            content: '确认此待办已完成？',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await (0, request_1.patch)(`/todo/${id}/status`, { status: 4 });
                        wx.showToast({ title: '已完成', icon: 'success' });
                        this.loadTodos();
                    }
                    catch (e) {
                        console.error(e);
                    }
                }
            }
        });
    },
    /** 查看公告详情 */
    readNotice(e) {
        const item = e.currentTarget.dataset.item;
        if (item.content) {
            wx.showModal({
                title: item.title,
                content: item.content,
                showCancel: false
            });
        }
    }
});
