"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../utils/request");
Page({
    data: {
        keyword: '',
        activeType: 'all',
        customers: [],
        loading: false,
        loadingMore: false,
        noMore: false,
        current: 1,
        size: 20
    },
    onLoad() { this.loadCustomers(); },
    onShow() { this.resetAndLoad(); },
    onPullDownRefresh() { this.resetAndLoad().then(() => wx.stopPullDownRefresh()); },
    onReachBottom() {
        if (!this.data.noMore && !this.data.loadingMore)
            this.loadMore();
    },
    onSearch(e) { this.setData({ keyword: e.detail.value }); },
    doSearch() { this.resetAndLoad(); },
    filterType(e) {
        this.setData({ activeType: e.currentTarget.dataset.type, current: 1, customers: [], noMore: false });
        this.loadCustomers();
    },
    buildParams() {
        const params = { current: this.data.current, size: this.data.size };
        if (this.data.keyword)
            params.keyword = this.data.keyword;
        if (this.data.activeType === 'member')
            params.isMember = 1;
        if (this.data.activeType === 'normal')
            params.isMember = 0;
        if (this.data.activeType === 'factory')
            params.customerType = 2;
        return params;
    },
    resetAndLoad() {
        this.setData({ current: 1, customers: [], noMore: false });
        return this.loadCustomers();
    },
    async loadCustomers() {
        this.setData({ loading: true });
        try {
            const res = await (0, request_1.get)('/customers', this.buildParams());
            const customers = (res.records || []).map((c) => {
                const typeLabel = { 1: '普通', 2: '工厂', 3: '零售' }[c.customerType] || '普通';
                const memberLevelName = { 1: '普通会员', 2: 'VIP会员', 3: '战略会员' }[c.memberLevel] || '';
                return Object.assign(Object.assign({}, c), { displayName: c.name || c.customerName || '', typeName: typeLabel, memberBadge: c.isMember === 1, memberLevelName });
            });
            this.setData({ customers, noMore: customers.length < this.data.size });
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
            const res = await (0, request_1.get)('/customers', this.buildParams());
            const records = (res.records || []).map((c) => {
                const typeLabel = { 1: '普通', 2: '工厂', 3: '零售' }[c.customerType] || '普通';
                return Object.assign(Object.assign({}, c), { displayName: c.name || c.customerName || '', typeName: typeLabel });
            });
            this.setData({ customers: [...this.data.customers, ...records], noMore: records.length < this.data.size });
        }
        catch (e) {
            console.error(e);
        }
        finally {
            this.setData({ loadingMore: false });
        }
    },
    addCustomer() { wx.navigateTo({ url: '/pages/customer/create/create' }); },
    goDetail(e) {
        wx.navigateTo({ url: `/pages/customer/detail/detail?id=${e.currentTarget.dataset.id}` });
    },
    // 长按操作菜单
    showActions(e) {
        const id = e.currentTarget.dataset.id;
        const name = e.currentTarget.dataset.name;
        const isRetail = e.currentTarget.dataset.retail;
        const actions = ['查看详情', '编辑信息'];
        if (!isRetail)
            actions.push('删除客户');
        wx.showActionSheet({
            itemList: actions,
            success: (res) => {
                if (res.tapIndex === 0) {
                    wx.navigateTo({ url: `/pages/customer/detail/detail?id=${id}` });
                }
                else if (res.tapIndex === 1) {
                    wx.navigateTo({ url: `/pages/customer/create/create?id=${id}&mode=edit` });
                }
                else if (res.tapIndex === 2 && !isRetail) {
                    this.deleteCustomer(id, name);
                }
            }
        });
    },
    deleteCustomer(id, name) {
        wx.showModal({
            title: '确认删除',
            content: `确定删除客户"${name}"？关联的订单将无法删除此客户。`,
            confirmColor: '#E34D59',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await (0, request_1.del)(`/customers/${id}`);
                        wx.showToast({ title: '已删除', icon: 'success' });
                        this.resetAndLoad();
                    }
                    catch (e) { /* handled */ }
                }
            }
        });
    },
    callPhone(e) {
        const phone = e.currentTarget.dataset.phone;
        if (phone)
            wx.makePhoneCall({ phoneNumber: phone });
    }
});
