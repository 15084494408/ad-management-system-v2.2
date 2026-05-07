"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../utils/request");
Page({
    data: {
        categories: [],
        showAdd: false,
        editId: 0,
        name: '',
        submitting: false
    },
    onLoad() { this.loadCategories(); },
    onShow() { this.loadCategories(); },
    async loadCategories() {
        try {
            const res = await (0, request_1.get)('/material/category', null, { showLoading: false });
            this.setData({ categories: Array.isArray(res) ? res : (res.records || []) });
        }
        catch (e) {
            console.error(e);
        }
    },
    showAddDialog() { this.setData({ showAdd: true, editId: 0, name: '' }); },
    hideDialog() { this.setData({ showAdd: false }); },
    onNameChange(e) { this.setData({ name: e.detail.value }); },
    async addOrUpdate() {
        if (!this.data.name.trim()) {
            wx.showToast({ title: '请输入分类名称', icon: 'none' });
            return;
        }
        this.setData({ submitting: true });
        try {
            if (this.data.editId) {
                await (0, request_1.put)(`/material/category/${this.data.editId}`, { name: this.data.name.trim() });
            }
            else {
                await (0, request_1.post)('/material/category', { name: this.data.name.trim() });
            }
            wx.showToast({ title: '保存成功', icon: 'success' });
            this.setData({ showAdd: false, name: '', editId: 0 });
            this.loadCategories();
        }
        catch (e) { /* handled */ }
        finally {
            this.setData({ submitting: false });
        }
    },
    editCategory(e) {
        const item = e.currentTarget.dataset.item;
        this.setData({ showAdd: true, editId: item.id, name: item.name });
    },
    deleteCategory(e) {
        const id = e.currentTarget.dataset.id;
        const name = e.currentTarget.dataset.name;
        wx.showModal({
            title: '确认删除',
            content: `确定删除分类"${name}"？`,
            confirmColor: '#E34D59',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await (0, request_1.del)(`/material/category/${id}`);
                        wx.showToast({ title: '已删除', icon: 'success' });
                        this.loadCategories();
                    }
                    catch (e) { /* handled */ }
                }
            }
        });
    }
});
