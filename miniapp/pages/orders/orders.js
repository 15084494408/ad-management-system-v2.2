"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const request_1 = require("../../utils/request");
const helpers_1 = require("../../utils/helpers");
Page({
    data: {
        searchValue: '',
        activeTab: 'all',
        orders: [],
        loading: false,
        loadingMore: false,
        noMore: false,
        current: 1,
        size: 10
    },
    onLoad() {
        // onShow 会负责首次加载，这里不做重复请求
    },
    onShow() {
        // 切回页面时刷新
        this.setData({ current: 1, orders: [], noMore: false });
        this.loadOrders();
    },
    onPullDownRefresh() {
        this.setData({ current: 1, orders: [], noMore: false });
        this.loadOrders().then(() => wx.stopPullDownRefresh());
    },
    onReachBottom() {
        if (!this.data.noMore && !this.data.loadingMore) {
            this.loadMore();
        }
    },
    /** 搜索输入 */
    onSearch(e) {
        this.setData({ searchValue: e.detail.value });
    },
    /** 执行搜索 */
    doSearch() {
        this.setData({ current: 1, orders: [], noMore: false });
        this.loadOrders();
    },
    /** 切换 Tab */
    onTabChange(e) {
        this.setData({
            activeTab: e.detail.value,
            current: 1,
            orders: [],
            noMore: false
        });
        this.loadOrders();
    },
    /** 构建查询参数 */
    buildParams() {
        const params = {
            current: this.data.current,
            size: this.data.size
        };
        if (this.data.activeTab !== 'all') {
            params.status = this.data.activeTab;
        }
        if (this.data.searchValue) {
            params.keyword = this.data.searchValue;
        }
        return params;
    },
    /** 加载订单列表 */
    async loadOrders() {
        this.setData({ loading: true });
        try {
            const res = await (0, request_1.get)('/orders', this.buildParams());
            const records = (res.records || []).map(this.formatOrder);
            this.setData({ orders: records, noMore: records.length < this.data.size });
        }
        catch (e) {
            console.error('加载订单失败', e);
        }
        finally {
            this.setData({ loading: false });
        }
    },
    /** 加载更多 */
    async loadMore() {
        this.setData({ loadingMore: true, current: this.data.current + 1 });
        try {
            const res = await (0, request_1.get)('/orders', this.buildParams());
            const records = (res.records || []).map(this.formatOrder);
            this.setData({
                orders: [...this.data.orders, ...records],
                noMore: records.length < this.data.size
            });
        }
        catch (e) {
            console.error('加载更多失败', e);
        }
        finally {
            this.setData({ loadingMore: false });
        }
    },
    /** 格式化订单数据 */
    formatOrder(item) {
        return Object.assign(Object.assign({}, item), { statusText: helpers_1.ORDER_STATUS[item.status] || '未知', statusColor: helpers_1.ORDER_STATUS_COLOR[item.status] || 'default', totalAmount: (0, helpers_1.formatMoney)(item.totalAmount), 
            // ★ 修复 P0-金额：待收金额需减去优惠、抹零
            unpaidAmount: Math.max((item.totalAmount || 0) - (item.paidAmount || 0)
                - (item.discountAmount || 0) - (item.roundingAmount || 0), 0), createTime: (0, helpers_1.timeAgo)(item.createTime) });
    },
    /** 跳转创建订单 */
    createOrder() {
        wx.navigateTo({ url: '/pages/orders/create/create' });
    },
    /** 跳转订单详情 */
    goDetail(e) {
        const id = e.currentTarget.dataset.id;
        wx.navigateTo({ url: `/pages/orders/detail/detail?id=${id}` });
    }
});
