"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../../utils/request");
const helpers_1 = require("../../../utils/helpers");
Page({
    data: {
        billId: 0,
        bill: null,
        details: [],
        loading: true
    },
    onLoad(options) {
        this.setData({ billId: parseInt(options.id) });
        this.loadBill();
        this.loadDetails();
    },
    async loadBill() {
        try {
            const bill = await (0, request_1.get)(`/factory/bills/${this.data.billId}`);
            const statusText = { 0: '未付', 1: '部分付', 2: '已付清', 3: '已取消' }[bill.paymentStatus] || '未知';
            this.setData({
                bill: Object.assign(Object.assign({}, bill), { amountStr: (0, helpers_1.formatMoney)(bill.totalAmount || bill.amount || 0), paidStr: (0, helpers_1.formatMoney)(bill.paidAmount || 0), unpaidStr: (0, helpers_1.formatMoney)(Math.max((bill.totalAmount || bill.amount || 0) - (bill.paidAmount || 0), 0)), statusText, createTime: (0, helpers_1.formatDate)(bill.createTime) })
            });
        }
        catch (e) {
            console.error(e);
        }
        finally {
            this.setData({ loading: false });
        }
    },
    async loadDetails() {
        try {
            const res = await (0, request_1.get)(`/factory/bills/${this.data.billId}/details`);
            this.setData({
                details: (res || []).map((d) => (Object.assign(Object.assign({}, d), { amountStr: (0, helpers_1.formatMoney)(d.amount || d.unitPrice * d.quantity || 0), unitPriceStr: (0, helpers_1.formatMoney)(d.unitPrice || 0) })))
            });
        }
        catch (e) {
            console.error(e);
        }
    },
    addPayment() {
        var _a, _b;
        const unpaid = (((_a = this.data.bill) === null || _a === void 0 ? void 0 : _a.totalAmount) || 0) - (((_b = this.data.bill) === null || _b === void 0 ? void 0 : _b.paidAmount) || 0);
        if (unpaid <= 0) {
            wx.showToast({ title: '无待付金额', icon: 'none' });
            return;
        }
        wx.showModal({
            title: '录入付款',
            content: `待付金额：¥${(0, helpers_1.formatMoney)(unpaid)}`,
            editable: true,
            placeholderText: '请输入付款金额',
            success: async (res) => {
                if (res.confirm && res.content) {
                    const amount = parseFloat(res.content);
                    if (isNaN(amount) || amount <= 0) {
                        wx.showToast({ title: '请输入有效金额', icon: 'none' });
                        return;
                    }
                    try {
                        await (0, request_1.put)(`/factory/bills/${this.data.billId}/paid`, { amount });
                        wx.showToast({ title: '付款成功', icon: 'success' });
                        this.loadBill();
                    }
                    catch (e) { /* handled */ }
                }
            }
        });
    }
});
