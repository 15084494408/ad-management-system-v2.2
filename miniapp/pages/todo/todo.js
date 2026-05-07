"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../utils/request");
const helpers_1 = require("../../utils/helpers");
Page({
    data: {
        activeTab: 'all',
        todos: [],
        loading: false,
        showCreate: false,
        newTodo: { requirements: '', customerName: '', contactPhone: '', source: '', priority: 1 }
    },
    onLoad() { this.loadTodos(); },
    onShow() { this.loadTodos(); },
    onPullDownRefresh() { this.loadTodos().then(() => wx.stopPullDownRefresh()); },
    onTabChange(e) {
        this.setData({ activeTab: e.detail.value });
        this.loadTodos();
    },
    async loadTodos() {
        this.setData({ loading: true });
        try {
            const res = await (0, request_1.get)('/todo/list', null, { showLoading: false });
            // 后端返回 [{items: [...], statusCount: {...}}]
            const data = Array.isArray(res) && res.length > 0 ? res[0] : res;
            let items = data.items || [];
            // 按tab筛选
            const tab = this.data.activeTab;
            if (tab === 'pending')
                items = items.filter((t) => t.status === 1 || t.status === 2);
            else if (tab === 'done')
                items = items.filter((t) => t.status === 4);
            this.setData({
                todos: items.map((t) => (Object.assign(Object.assign({}, t), { statusLabel: { 1: '新收集', 2: '分析中', 3: '待确认', 4: '已转订单' }[t.status] || '未知', priorityLabel: { 1: '普通', 2: '紧急', 3: '非常紧急' }[t.priority] || '普通', createTime: (0, helpers_1.formatDateTime)(t.createTime) })))
            });
        }
        catch (e) {
            console.error(e);
        }
        finally {
            this.setData({ loading: false });
        }
    },
    showCreateDialog() { this.setData({ showCreate: true }); },
    hideCreateDialog() { this.setData({ showCreate: false }); },
    onNewFieldChange(e) {
        const field = e.currentTarget.dataset.field;
        this.setData({ [`newTodo.${field}`]: e.detail.value });
    },
    onPriorityChange(e) {
        this.setData({ 'newTodo.priority': parseInt(e.detail.value) || 1 });
    },
    async createTodo() {
        var _a;
        const t = this.data.newTodo;
        if (!((_a = t.requirements) === null || _a === void 0 ? void 0 : _a.trim())) {
            wx.showToast({ title: '请输入需求描述', icon: 'none' });
            return;
        }
        try {
            await (0, request_1.post)('/todo', {
                requirements: t.requirements.trim(),
                customerName: t.customerName || null,
                contactPhone: t.contactPhone || null,
                source: t.source || null,
                priority: t.priority || 1
            });
            wx.showToast({ title: '创建成功', icon: 'success' });
            this.setData({ showCreate: false, newTodo: { requirements: '', customerName: '', contactPhone: '', source: '', priority: 1 } });
            this.loadTodos();
        }
        catch (e) { /* handled */ }
    },
    updateStatus(e) {
        const id = e.currentTarget.dataset.id;
        const newStatus = e.currentTarget.dataset.status;
        const label = e.currentTarget.dataset.label;
        if (newStatus === 4) {
            wx.showModal({
                title: '确认',
                content: `确认标记为"${label}"？`,
                success: async (res) => {
                    if (res.confirm) {
                        try {
                            await (0, request_1.put)(`/todo/${id}`, { status: newStatus });
                            wx.showToast({ title: '操作成功', icon: 'success' });
                            this.loadTodos();
                        }
                        catch (e) { /* handled */ }
                    }
                }
            });
        }
        else {
            wx.showActionSheet({
                itemList: ['标记为分析中', '标记为待确认', '标记为已转订单'],
                success: async (res) => {
                    const statuses = [2, 3, 4];
                    const labels = ['分析中', '待确认', '已转订单'];
                    try {
                        await (0, request_1.put)(`/todo/${id}`, { status: statuses[res.tapIndex] });
                        wx.showToast({ title: `已标记为${labels[res.tapIndex]}`, icon: 'success' });
                        this.loadTodos();
                    }
                    catch (e) { /* handled */ }
                }
            });
        }
    },
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
                        this.loadTodos();
                    }
                    catch (e) { /* handled */ }
                }
            }
        });
    }
});
