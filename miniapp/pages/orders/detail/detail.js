"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../../utils/request");
const helpers_1 = require("../../../utils/helpers");
const PAYMENT_STATUS_MAP = {
    1: '未付款',
    2: '部分付款',
    3: '已结清',
    4: '已抹零'
};
const PAYMENT_ICON_MAP = {
    1: 'time',
    2: 'money-circle',
    3: 'check-circle',
    4: 'discount'
};
const PAYMENT_DESC_MAP = {
    1: '该订单尚未收款',
    2: '部分款项已到账',
    3: '所有款项已结清',
    4: '已抹零结清'
};
Page({
    data: {
        id: 0,
        detail: {},
        paymentStatus: 1,
        paymentStatusText: '',
        paymentStatusIcon: '',
        paymentStatusDesc: '',
        loading: false
    },
    onLoad(options) {
        if (options.id) {
            this.setData({ id: parseInt(options.id) });
            this.loadDetail();
        }
    },
    async loadDetail() {
        this.setData({ loading: true });
        try {
            const res = await (0, request_1.get)(`/orders/${this.data.id}`);
            const raw = res;
            const unpaid = Math.max((raw.totalAmount || 0) - (raw.paidAmount || 0) - (raw.discountAmount || 0) - (raw.roundingAmount || 0), 0);
            const detail = Object.assign(Object.assign({}, res), { totalAmount: (0, helpers_1.formatMoney)(raw.totalAmount), discountAmount: (0, helpers_1.formatMoney)(raw.discountAmount), roundingAmount: (0, helpers_1.formatMoney)(raw.roundingAmount), paidAmount: (0, helpers_1.formatMoney)(raw.paidAmount), unpaidAmount: (0, helpers_1.formatMoney)(unpaid), statusText: helpers_1.ORDER_STATUS[res.status] || '未知', statusColor: helpers_1.ORDER_STATUS_COLOR[res.status] || 'default', createTime: (0, helpers_1.formatDateTime)(res.createTime), updateTime: (0, helpers_1.formatDateTime)(res.updateTime) });
            // 支付状态：使用原始数值做比较
            let paymentStatus = 1;
            if ((raw.roundingAmount || 0) > 0 && (raw.paidAmount || 0) >= (raw.totalAmount || 0) - (raw.discountAmount || 0) - (raw.roundingAmount || 0)) {
                paymentStatus = 4;
            }
            else if ((raw.paidAmount || 0) >= (raw.totalAmount || 0) - (raw.discountAmount || 0)) {
                paymentStatus = 3;
            }
            else if ((raw.paidAmount || 0) > 0) {
                paymentStatus = 2;
            }
            this.setData({
                detail,
                paymentStatus,
                paymentStatusText: PAYMENT_STATUS_MAP[paymentStatus] || '未知',
                paymentStatusIcon: PAYMENT_ICON_MAP[paymentStatus] || 'time',
                paymentStatusDesc: PAYMENT_DESC_MAP[paymentStatus] || ''
            });
        }
        catch (e) {
            console.error('加载订单详情失败', e);
            wx.showToast({ title: '加载失败', icon: 'none' });
        }
        finally {
            this.setData({ loading: false });
        }
    },
    editOrder() {
        wx.navigateTo({ url: `/pages/orders/edit/edit?id=${this.data.id}` });
    },
    printOrder() {
        wx.showToast({ title: '打印功能已提交', icon: 'none' });
    },
    recordPayment() {
        wx.navigateTo({ url: `/pages/orders/payment/payment?id=${this.data.id}` });
    },
    shareOrder() {
        wx.showToast({ title: '分享功能即将开放', icon: 'none' });
    }
});
