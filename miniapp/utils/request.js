"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.get = get;
exports.post = post;
exports.put = put;
exports.del = del;
exports.patch = patch;
exports.upload = upload;
const config_1 = __importDefault(require("./config"));
/** 统一请求封装 */
function request(options) {
    return new Promise((resolve, reject) => {
        const token = wx.getStorageSync('token');
        if (options.showLoading !== false) {
            wx.showLoading({ title: '加载中...', mask: true });
        }
        wx.request({
            url: config_1.default.baseUrl + options.url,
            method: options.method || 'GET',
            data: options.data,
            header: Object.assign({ 'Content-Type': 'application/json', 'Authorization': token ? `Bearer ${token}` : '' }, options.header),
            timeout: options.timeout || config_1.default.timeout,
            success: (res) => {
                wx.hideLoading();
                if (res.statusCode === 200) {
                    const body = res.data;
                    if (body.code === 200 || body.code === 0) {
                        resolve(body.data);
                    }
                    else if (body.code === 401) {
                        // Token 过期，跳转登录
                        wx.removeStorageSync('token');
                        wx.showToast({ title: '登录已过期', icon: 'none' });
                        setTimeout(() => {
                            wx.navigateTo({ url: '/pages/login/login' });
                        }, 1500);
                        reject(new Error(body.message || '未授权'));
                    }
                    else {
                        if (options.showError !== false) {
                            wx.showToast({ title: body.message || '请求失败', icon: 'none' });
                        }
                        reject(new Error(body.message || '请求失败'));
                    }
                }
                else if (res.statusCode === 401) {
                    wx.removeStorageSync('token');
                    wx.showToast({ title: '登录已过期', icon: 'none' });
                    setTimeout(() => {
                        wx.navigateTo({ url: '/pages/login/login' });
                    }, 1500);
                    reject(new Error('未授权'));
                }
                else {
                    if (options.showError !== false) {
                        wx.showToast({ title: `请求失败(${res.statusCode})`, icon: 'none' });
                    }
                    reject(new Error(`HTTP ${res.statusCode}`));
                }
            },
            fail: (err) => {
                wx.hideLoading();
                wx.showToast({ title: '网络异常', icon: 'none' });
                reject(err);
            }
        });
    });
}
/** GET 请求 */
function get(url, data, options) {
    return request(Object.assign({ url, method: 'GET', data }, options));
}
/** POST 请求 */
function post(url, data, options) {
    return request(Object.assign({ url, method: 'POST', data }, options));
}
/** PUT 请求 */
function put(url, data, options) {
    return request(Object.assign({ url, method: 'PUT', data }, options));
}
/** DELETE 请求 */
function del(url, data, options) {
    return request(Object.assign({ url, method: 'DELETE', data }, options));
}
/** PATCH 请求 */
function patch(url, data, options) {
    return request(Object.assign({ url, method: 'PATCH', data }, options));
}
/** 文件上传 */
function upload(url, filePath, name = 'file') {
    return new Promise((resolve, reject) => {
        const token = wx.getStorageSync('token');
        wx.uploadFile({
            url: config_1.default.baseUrl + url,
            filePath,
            name,
            header: {
                'Authorization': token ? `Bearer ${token}` : ''
            },
            success: (res) => {
                if (res.statusCode === 200) {
                    const body = JSON.parse(res.data);
                    if (body.code === 200 || body.code === 0) {
                        resolve(body.data);
                    }
                    else {
                        reject(new Error(body.message || '上传失败'));
                    }
                }
                else {
                    reject(new Error(`上传失败(${res.statusCode})`));
                }
            },
            fail: reject
        });
    });
}
