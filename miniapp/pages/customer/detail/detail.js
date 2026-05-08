"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../../utils/request");
const helpers_1 = require("../../../utils/helpers");
Page({
    data: {
        customerId: 0,
        detail: null,
        displayName: '',
        balance: '0.00',
        typeName: '普通',
        memberLevelName: '',
        loading: true,
        activeTab: 'info',
        orders: [],
        ordersLoading: false
    },
    onLoad(options) {
        this.setData({ customerId: parseInt(options.id) });
        this.loadCustomer();
    },
    onShow() { if (this.data.customerId)
        this.loadCustomer(); },
    onPullDownRefresh() { this.loadCustomer().then(() => wx.stopPullDownRefresh()); },
    onTabChange(e) {
        const tab = e.detail.value;
        this.setData({ activeTab: tab });
        if (tab === 'orders' && this.data.orders.length === 0) {
            this.loadOrders();
        }
    },
    async loadCustomer() {
        this.setData({ loading: true });
        try {
            const c = await (0, request_1.get)(`/customers/${this.data.customerId}`);
            const memberLevelName = { 1: '普通会员', 2: 'VIP会员', 3: '战略会员' };
            const typeLabel = { 1: '普通', 2: '工厂', 3: '零售' };
            const displayName = c.name || c.customerName || '';
            const typeName = typeLabel[c.customerType] || '普通';
            this.setData({
                detail: Object.assign(Object.assign({}, c), { displayName,
                    typeName, memberLevelName: memberLevelName[c.memberLevel] || '普通会员', balance: (0, helpers_1.formatMoney)(c.balance || 0), totalRecharge: (0, helpers_1.formatMoney)(c.totalRecharge || 0), totalConsume: (0, helpers_1.formatMoney)(c.totalConsume || 0), totalAmount: (0, helpers_1.formatMoney)(c.totalAmount || 0), orderCount: c.orderCount || 0, isMember: c.isMember === 1, createTime: (0, helpers_1.formatDate)(c.createTime) }),
                displayName,
                typeName,
                balance: (0, helpers_1.formatMoney)(c.balance || 0),
                memberLevelName: memberLevelName[c.memberLevel] || ''
            });
        }
        catch (e) {
            console.error(e);
        }
        finally {
            this.setData({ loading: false });
        }
    },
    async loadOrders() {
        this.setData({ ordersLoading: true });
        try {
            const res = await (0, request_1.get)('/orders', { customerId: this.data.customerId, current: 1, size: 50 }, { showLoading: false });
            const statusMap = { 1: '待处理', 2: '进行中', 3: '已完成', 4: '已取消' };
            const statusColorMap = { 1: 'warning', 2: 'primary', 3: 'success', 4: 'default' };
            this.setData({
                orders: (res.records || []).map((o) => (Object.assign(Object.assign({}, o), { statusText: statusMap[o.status] || '未知', statusColor: statusColorMap[o.status] || 'default', totalAmount: (0, helpers_1.formatMoney)(o.totalAmount), createTime: (0, helpers_1.timeAgo)(o.createTime) })))
            });
        }
        catch (e) {
            console.error(e);
        }
        finally {
            this.setData({ ordersLoading: false });
        }
    },
    recharge() {
        wx.navigateTo({ url: `/pages/customer/recharge/recharge?id=${this.data.customerId}` });
    },
    consume() {
        wx.navigateTo({ url: `/pages/customer/consume/consume?id=${this.data.customerId}` });
    },
    async upgradeMember() {
        wx.showModal({
            title: '升级为会员',
            content: '确认将此客户升级为会员？',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await (0, request_1.post)(`/customers/${this.data.customerId}/upgrade-member`);
                        wx.showToast({ title: '升级成功', icon: 'success' });
                        this.loadCustomer();
                    }
                    catch (e) { /* handled */ }
                }
            }
        });
    },
    callPhone() {
        var _a;
        const phone = (_a = this.data.detail) === null || _a === void 0 ? void 0 : _a.phone;
        if (phone)
            wx.makePhoneCall({ phoneNumber: phone });
    },
    editCustomer() {
        wx.navigateTo({ url: `/pages/customer/create/create?id=${this.data.customerId}&mode=edit` });
    },
    goTransactions() {
        var _a;
        wx.navigateTo({ url: `/pages/member/transactions?id=${this.data.customerId}&name=${encodeURIComponent(((_a = this.data.detail) === null || _a === void 0 ? void 0 : _a.displayName) || '')}` });
    },
    goOrderDetail(e) {
        wx.navigateTo({ url: `/pages/orders/detail/detail?id=${e.currentTarget.dataset.id}` });
    }
});
