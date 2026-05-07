"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.wxLogin = wxLogin;
exports.bindWx = bindWx;
exports.passwordLogin = passwordLogin;
exports.getUserInfo = getUserInfo;
exports.getToken = getToken;
exports.isLoggedIn = isLoggedIn;
exports.logout = logout;
exports.requireAuth = requireAuth;
const request_1 = require("./request");
/** 微信登录（后端需实现 /auth/wx-login） */
async function wxLogin() {
    const { code } = await wx.login();
    const result = await (0, request_1.post)('/auth/wx-login', { code }, { showLoading: true });
    wx.setStorageSync('token', result.token);
    wx.setStorageSync('userInfo', result.userInfo);
    return result;
}
/** 绑定微信（已登录用户） */
async function bindWx() {
    const { code } = await wx.login();
    await (0, request_1.post)('/auth/wx-bind', { code }, { showLoading: true });
    wx.showToast({ title: '绑定成功', icon: 'success' });
}
/** 账号密码登录 */
async function passwordLogin(username, password) {
    const result = await (0, request_1.post)('/auth/login', { username, password }, { showLoading: true });
    wx.setStorageSync('token', result.token);
    wx.setStorageSync('userInfo', result.userInfo);
    return result;
}
/** 获取当前用户信息 */
function getUserInfo() {
    return wx.getStorageSync('userInfo') || null;
}
/** 获取 token */
function getToken() {
    return wx.getStorageSync('token') || '';
}
/** 是否已登录 */
function isLoggedIn() {
    return !!getToken();
}
/** 退出登录 */
function logout() {
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    wx.reLaunch({ url: '/pages/login/login' });
}
/** 检查登录状态，未登录跳转登录页 */
function requireAuth() {
    if (isLoggedIn())
        return true;
    wx.navigateTo({ url: '/pages/login/login' });
    return false;
}
