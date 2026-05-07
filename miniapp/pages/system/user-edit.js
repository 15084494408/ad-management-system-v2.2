"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../utils/request");
Page({
    data: {
        form: { username: '', password: '', realName: '', phone: '', email: '' },
        submitting: false
    },
    onChange(e) {
        const field = e.currentTarget.dataset.field;
        this.setData({ [`form.${field}`]: e.detail.value });
    },
    async submit() {
        const f = this.data.form;
        if (!f.username.trim()) {
            wx.showToast({ title: '请输入用户名', icon: 'none' });
            return;
        }
        if (!f.password) {
            wx.showToast({ title: '请输入密码', icon: 'none' });
            return;
        }
        this.setData({ submitting: true });
        try {
            await (0, request_1.post)('/system/users', {
                username: f.username.trim(),
                password: f.password,
                realName: f.realName || null,
                phone: f.phone || null,
                email: f.email || null
            });
            wx.showToast({ title: '创建成功', icon: 'success' });
            setTimeout(() => wx.navigateBack(), 1500);
        }
        catch (e) { /* handled */ }
        finally {
            this.setData({ submitting: false });
        }
    }
});
