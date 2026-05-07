"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const auth_1 = require("../../utils/auth");
Page({
    data: {
        userInfo: {},
        roleText: '',
        wxBound: false
    },
    onShow() {
        if (!(0, auth_1.isLoggedIn)()) {
            wx.navigateTo({ url: '/pages/login/login' });
            return;
        }
        const info = (0, auth_1.getUserInfo)() || {};
        const roles = (info.roles || []);
        const roleText = roles.includes('admin') ? '超级管理员' :
            roles.includes('manager') ? '管理员' :
                roles.includes('designer') ? '设计师' :
                    roles.length > 0 ? roles[0] : '普通用户';
        this.setData({
            userInfo: info,
            roleText,
            wxBound: !!info.wxBound
        });
    },
    // ===== 业务模块导航 =====
    goTodo() { wx.navigateTo({ url: '/pages/todo/todo' }); },
    goOrderQuote() { wx.navigateTo({ url: '/pages/orders/quote/quote' }); },
    goCustomerBills() { wx.navigateTo({ url: '/pages/customer/bills/customer-bills' }); },
    goFactoryBills() { wx.navigateTo({ url: '/pages/customer/bills/factory-bills' }); },
    goMember() { wx.navigateTo({ url: '/pages/member/member' }); },
    goMaterial() { wx.navigateTo({ url: '/pages/material/material' }); },
    goUsers() { wx.navigateTo({ url: '/pages/system/users' }); },
    goCompany() { wx.navigateTo({ url: '/pages/system/company' }); },
    // ===== 账户功能 =====
    async bindWxAction() {
        if (this.data.wxBound) {
            wx.showToast({ title: '已绑定微信', icon: 'none' });
            return;
        }
        try {
            await (0, auth_1.bindWx)();
            this.setData({ wxBound: true });
        }
        catch (e) { /* handled */ }
    },
    changePassword() {
        wx.showToast({ title: '请联系管理员修改密码', icon: 'none' });
    },
    clearCache() {
        wx.showModal({
            title: '确认清除',
            content: '将清除本地缓存数据，不影响账号信息',
            success: (res) => {
                if (res.confirm) {
                    wx.removeStorageSync('userInfo');
                    wx.showToast({ title: '缓存已清除', icon: 'success' });
                }
            }
        });
    },
    about() {
        wx.showModal({
            title: '关于',
            content: '企业广告管理系统 v2.0\n微信小程序版\n数据与Web端实时同步',
            showCancel: false
        });
    },
    doLogout() {
        wx.showModal({
            title: '提示',
            content: '确认退出登录？',
            success: (res) => {
                if (res.confirm) {
                    (0, auth_1.logout)();
                }
            }
        });
    }
});
