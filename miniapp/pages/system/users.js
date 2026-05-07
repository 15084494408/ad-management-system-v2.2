"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../utils/request");
const helpers_1 = require("../../utils/helpers");
Page({
    data: {
        keyword: '',
        users: [],
        loading: false,
        loadingMore: false,
        noMore: false,
        current: 1,
        size: 20
    },
    onLoad() { this.loadUsers(); },
    onShow() { this.resetAndLoad(); },
    onPullDownRefresh() { this.resetAndLoad().then(() => wx.stopPullDownRefresh()); },
    onReachBottom() { if (!this.data.noMore && !this.data.loadingMore)
        this.loadMore(); },
    onSearch(e) { this.setData({ keyword: e.detail.value }); },
    doSearch() { this.resetAndLoad(); },
    buildParams() {
        const params = { current: this.data.current, size: this.data.size };
        if (this.data.keyword)
            params.keyword = this.data.keyword;
        return params;
    },
    resetAndLoad() {
        this.setData({ current: 1, users: [], noMore: false });
        return this.loadUsers();
    },
    async loadUsers() {
        this.setData({ loading: true });
        try {
            const res = await (0, request_1.get)('/system/users', this.buildParams());
            const records = (res.records || res || []).map((u) => (Object.assign(Object.assign({}, u), { statusLabel: u.status === 1 ? '启用' : '禁用', roleNames: (u.roles || u.roleNames || []).join(', ') || '未分配', createTime: (0, helpers_1.formatDate)(u.createTime) })));
            this.setData({ users: records, noMore: records.length < this.data.size });
        }
        catch (e) {
            console.error(e);
        }
        finally {
            this.setData({ loading: false });
        }
    },
    async loadMore() {
        this.setData({ loadingMore: true, current: this.data.current + 1 });
        try {
            const res = await (0, request_1.get)('/system/users', this.buildParams());
            const records = (res.records || res || []).map((u) => (Object.assign({}, u)));
            this.setData({ users: [...this.data.users, ...records], noMore: records.length < this.data.size });
        }
        catch (e) {
            console.error(e);
        }
        finally {
            this.setData({ loadingMore: false });
        }
    },
    showCreateDialog() { wx.navigateTo({ url: '/pages/system/user-edit' }); },
    toggleStatus(e) {
        const user = e.currentTarget.dataset.user;
        const newStatus = user.status === 1 ? 0 : 1;
        const label = newStatus === 1 ? '启用' : '禁用';
        wx.showModal({
            title: `确认${label}`,
            content: `确定${label}用户"${user.realName || user.username}"？`,
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await (0, request_1.put)(`/system/users/${user.id}/status`, { status: newStatus });
                        wx.showToast({ title: `${label}成功`, icon: 'success' });
                        this.resetAndLoad();
                    }
                    catch (e) { /* handled */ }
                }
            }
        });
    },
    resetPassword(e) {
        const user = e.currentTarget.dataset.user;
        wx.showModal({
            title: '重置密码',
            content: `确定重置用户"${user.realName || user.username}"的密码为 123456？`,
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await (0, request_1.put)(`/system/users/${user.id}/password`, { newPassword: '123456' });
                        wx.showToast({ title: '密码已重置', icon: 'success' });
                    }
                    catch (e) { /* handled */ }
                }
            }
        });
    },
    deleteUser(e) {
        const user = e.currentTarget.dataset.user;
        wx.showModal({
            title: '确认删除',
            content: `确定删除用户"${user.realName || user.username}"？`,
            confirmColor: '#E34D59',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await (0, request_1.del)(`/system/users/${user.id}`);
                        wx.showToast({ title: '已删除', icon: 'success' });
                        this.resetAndLoad();
                    }
                    catch (e) { /* handled */ }
                }
            }
        });
    }
});
