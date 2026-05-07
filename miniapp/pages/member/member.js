"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../utils/request");
const helpers_1 = require("../../utils/helpers");
Page({
    data: {
        keyword: '',
        members: [],
        loading: false,
        loadingMore: false,
        noMore: false,
        current: 1,
        size: 20
    },
    onLoad() { this.loadMembers(); },
    onShow() { this.resetAndLoad(); },
    onPullDownRefresh() { this.resetAndLoad().then(() => wx.stopPullDownRefresh()); },
    onReachBottom() { if (!this.data.noMore && !this.data.loadingMore)
        this.loadMore(); },
    onSearch(e) { this.setData({ keyword: e.detail.value }); },
    doSearch() { this.resetAndLoad(); },
    buildParams() {
        const params = { current: this.data.current, size: this.data.size, isMember: 1 };
        if (this.data.keyword)
            params.keyword = this.data.keyword;
        return params;
    },
    resetAndLoad() {
        this.setData({ current: 1, members: [], noMore: false });
        return this.loadMembers();
    },
    async loadMembers() {
        this.setData({ loading: true });
        try {
            const res = await (0, request_1.get)('/customers', this.buildParams());
            const levelName = { 1: '普通会员', 2: 'VIP会员', 3: '战略会员' };
            const records = (res.records || []).map((c) => (Object.assign(Object.assign({}, c), { levelName: levelName[c.memberLevel || c.level] || '普通会员', balance: (0, helpers_1.formatMoney)(c.balance || 0), totalRecharge: (0, helpers_1.formatMoney)(c.totalRecharge || 0), totalConsume: (0, helpers_1.formatMoney)(c.totalConsume || 0), createTime: (0, helpers_1.formatDate)(c.createTime) })));
            this.setData({ members: records, noMore: records.length < this.data.size });
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
            const records = (res.records || []).map((c) => (Object.assign({}, c)));
            this.setData({ members: [...this.data.members, ...records], noMore: records.length < this.data.size });
        }
        catch (e) {
            console.error(e);
        }
        finally {
            this.setData({ loadingMore: false });
        }
    },
    goDetail(e) {
        wx.navigateTo({ url: `/pages/customer/detail/detail?id=${e.currentTarget.dataset.id}` });
    },
    goRecharge(e) {
        wx.navigateTo({ url: `/pages/customer/recharge/recharge?id=${e.currentTarget.dataset.id}` });
    },
    goTransactions(e) {
        wx.navigateTo({ url: `/pages/member/transactions?id=${e.currentTarget.dataset.id}&name=${encodeURIComponent(e.currentTarget.dataset.name)}` });
    }
});
