"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../../utils/request");
const helpers_1 = require("../../../utils/helpers");
Page({
    data: {
        customerId: 0,
        customerName: '',
        balance: '0.00',
        amount: '',
        paymentMethod: '现金',
        paymentMethods: ['现金', '微信', '支付宝', '转账', '其他'],
        remark: '',
        submitting: false,
        quickAmounts: [100, 200, 500, 1000, 2000, 5000, 8000, 10000]
    },
    onLoad(options) {
        this.setData({ customerId: options.id });
        this.loadCustomer();
    },
    async loadCustomer() {
        try {
            const c = await (0, request_1.get)(`/customers/${this.data.customerId}`);
            this.setData({
                customerName: c.name || c.customerName || '',
                balance: (0, helpers_1.formatMoney)(c.balance || 0)
            });
        }
        catch (e) {
            console.error(e);
        }
    },
    onAmountChange(e) { this.setData({ amount: e.detail.value }); },
    onRemarkChange(e) { this.setData({ remark: e.detail.value }); },
    selectMethod(e) {
        this.setData({ paymentMethod: e.currentTarget.dataset.method });
    },
    setAmount(e) {
        this.setData({ amount: String(e.currentTarget.dataset.amount) });
    },
    async submit() {
        const amount = parseFloat(this.data.amount);
        if (!amount || amount <= 0) {
            wx.showToast({ title: '请输入有效金额', icon: 'none' });
            return;
        }
        this.setData({ submitting: true });
        try {
            await (0, request_1.post)(`/customers/${this.data.customerId}/recharge`, {
                amount,
                paymentMethod: this.data.paymentMethod,
                remark: this.data.remark
            });
            wx.showToast({ title: '充值成功', icon: 'success' });
            setTimeout(() => wx.navigateBack(), 1500);
        }
        catch (e) { /* handled */ }
        finally {
            this.setData({ submitting: false });
        }
    }
});
