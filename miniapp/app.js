"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("./utils/request");
App({
    globalData: {
        userInfo: null,
        token: '',
        baseUrl: 'http://localhost:8080',
        unreadCount: 0
    },
    onLaunch() {
        const token = wx.getStorageSync('token');
        // 验证 token 格式：有效 JWT 应包含 2 个点号
        if (token && token.split('.').length === 3) {
            this.globalData.token = token;
        }
        else {
            // 清除无效的 token（如演示留下的 demo-token）
            wx.removeStorageSync('token');
            wx.removeStorageSync('userInfo');
        }
    },
    onShow() {
        // 每次小程序切换到前台时刷新未读数
        if (this.globalData.token) {
            this.updateUnreadCount();
        }
    },
    // 检查是否已登录
    checkLogin() {
        return !!this.globalData.token;
    },
    // 跳转登录页
    goLogin() {
        wx.navigateTo({ url: '/pages/login/login' });
    },
    // 获取未读消息数
    async getUnreadCount() {
        if (!this.globalData.token)
            return 0;
        try {
            const res = await (0, request_1.get)('/notice/unread-count', null, { showLoading: false });
            return res.count || 0;
        }
        catch (e) {
            return 0;
        }
    },
    // 更新全局未读数
    async updateUnreadCount() {
        const count = await this.getUnreadCount();
        this.globalData.unreadCount = count;
        return count;
    }
});
