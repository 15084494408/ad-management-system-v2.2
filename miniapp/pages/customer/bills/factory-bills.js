"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../../utils/request");
const helpers_1 = require("../../../utils/helpers");
Page({
    data: {
        bills: [],
        loading: false,
        loadingMore: false,
        noMore: false,
        current: 1,
        size: 10
    },
    onLoad() { this.loadBills(); },
    onShow() { this.resetAndLoad(); },
    onPullDownRefresh() { this.resetAndLoad().then(() => wx.stopPullDownRefresh()); },
    onReachBottom() { if (!this.data.noMore && !this.data.loadingMore)
        this.loadMore(); },
    buildParams() {
        return { current: this.data.current, size: this.data.size, billType: 1 };
    },
    resetAndLoad() {
        this.setData({ current: 1, bills: [], noMore: false });
        return this.loadBills();
    },
    async loadBills() {
        this.setData({ loading: true });
        try {
            const res = await (0, request_1.get)('/factory/bills', this.buildParams());
            const records = (res.records || res || []).map((b) => (Object.assign(Object.assign({}, b), { amountStr: (0, helpers_1.formatMoney)(b.totalAmount || b.amount || 0), paidStr: (0, helpers_1.formatMoney)(b.paidAmount || 0), statusText: { 0: '未付', 1: '部分付', 2: '已付清', 3: '已取消' }[b.paymentStatus] || '未知', statusTheme: b.paymentStatus === 2 ? 'success' : b.paymentStatus === 1 ? 'warning' : 'danger', createTime: (0, helpers_1.formatDate)(b.createTime), billMonth: b.billMonth || '-' })));
            this.setData({ bills: records, noMore: records.length < this.data.size });
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
            const res = await (0, request_1.get)('/factory/bills', this.buildParams());
            const records = (res.records || res || []).map((b) => (Object.assign({}, b)));
            this.setData({ bills: [...this.data.bills, ...records], noMore: records.length < this.data.size });
        }
        catch (e) {
            console.error(e);
        }
        finally {
            this.setData({ loadingMore: false });
        }
    },
    goDetail(e) {
        wx.navigateTo({ url: `/pages/customer/bills/bill-detail?id=${e.currentTarget.dataset.id}&type=1` });
    }
});
