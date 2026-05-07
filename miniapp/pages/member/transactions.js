"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../utils/request");
const helpers_1 = require("../../utils/helpers");
Page({
    data: {
        customerId: 0,
        customerName: '',
        activeTab: 'all',
        transactions: [],
        loading: false
    },
    onLoad(options) {
        this.setData({
            customerId: parseInt(options.id),
            customerName: decodeURIComponent(options.name || '')
        });
        wx.setNavigationBarTitle({ title: this.data.customerName ? `${this.data.customerName} - 交易记录` : '交易记录' });
        this.loadData();
    },
    onTabChange(e) {
        this.setData({ activeTab: e.detail.value });
        this.loadData();
    },
    async loadData() {
        this.setData({ loading: true });
        try {
            const tab = this.data.activeTab;
            let url = '';
            if (tab === 'recharge') {
                url = `/customers/${this.data.customerId}/transactions`;
            }
            else if (tab === 'consume') {
                url = `/customers/${this.data.customerId}/transactions`;
            }
            else {
                url = `/customers/${this.data.customerId}/transactions`;
            }
            const res = await (0, request_1.get)(url, null, { showLoading: false });
            let items = Array.isArray(res) ? res : (res.records || res.items || []);
            if (tab === 'recharge') {
                items = items.filter((t) => t.type === 'recharge');
            }
            else if (tab === 'consume') {
                items = items.filter((t) => t.type === 'consume' || t.type === 'refund');
            }
            this.setData({
                transactions: items.map((t) => (Object.assign(Object.assign({}, t), { amountStr: (t.type === 'consume' || t.type === 'refund') ? `-${(0, helpers_1.formatMoney)(t.amount)}` : `+${(0, helpers_1.formatMoney)(t.amount)}`, isIncome: t.type === 'recharge', typeLabel: { recharge: '充值', consume: '消费', refund: '退款' }[t.type] || t.type || '未知', createTime: (0, helpers_1.formatDateTime)(t.createTime) })))
            });
        }
        catch (e) {
            console.error(e);
        }
        finally {
            this.setData({ loading: false });
        }
    }
});
