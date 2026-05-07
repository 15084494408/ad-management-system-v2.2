"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../utils/request");
const helpers_1 = require("../../utils/helpers");
Page({
    data: {
        materials: [],
        selectedIndex: -1,
        quantity: '',
        unitCost: '',
        remark: '',
        submitting: false
    },
    onLoad() { this.loadMaterials(); },
    async loadMaterials() {
        try {
            const res = await (0, request_1.get)('/material/all', null, { showLoading: false });
            const list = Array.isArray(res) ? res : (res.records || []);
            this.setData({ materials: list.map((m) => (Object.assign(Object.assign({}, m), { unitPriceStr: (0, helpers_1.formatMoney)(m.unitPrice || 0) }))) });
        }
        catch (e) {
            console.error(e);
        }
    },
    pickMaterial() {
        const names = this.data.materials.map((m) => `${m.name} (库存:${m.stock || 0}${m.unit || ''})`);
        if (names.length === 0) {
            wx.showToast({ title: '暂无物料', icon: 'none' });
            return;
        }
        wx.showActionSheet({
            itemList: names,
            success: (res) => {
                const m = this.data.materials[res.tapIndex];
                this.setData({ selectedIndex: res.tapIndex, unitCost: String(m.unitCost || m.unitPrice || '') });
            }
        });
    },
    onFieldChange(e) {
        const field = e.currentTarget.dataset.field;
        this.setData({ [field]: e.detail.value });
    },
    async submit() {
        if (this.data.selectedIndex < 0) {
            wx.showToast({ title: '请选择物料', icon: 'none' });
            return;
        }
        const qty = parseFloat(this.data.quantity);
        if (!qty || qty <= 0) {
            wx.showToast({ title: '请输入有效数量', icon: 'none' });
            return;
        }
        this.setData({ submitting: true });
        try {
            const m = this.data.materials[this.data.selectedIndex];
            await (0, request_1.post)('/material/stock-in', {
                materialId: m.id,
                quantity: qty,
                unitCost: parseFloat(this.data.unitCost) || 0,
                remark: this.data.remark || null
            });
            wx.showToast({ title: '入库成功', icon: 'success' });
            setTimeout(() => wx.navigateBack(), 1500);
        }
        catch (e) { /* handled */ }
        finally {
            this.setData({ submitting: false });
        }
    }
});
