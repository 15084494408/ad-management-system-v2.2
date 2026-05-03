# 数据看板改造 + 利润率修复 完成概要

## 1. 利润率计算修复
- **问题**: 利润率显示100%，因为使用`fin_record.expense`（本月为0）而非订单成本
- **修复**: `/finance/report/summary` 改为用 `ord_order.total_cost` 计算
- **结果**: 本月收入¥700 - 成本¥120 = 利润¥580, 利润率 **83%** ✅

## 2. 导航栏+创建订单按钮修正
- 恢复AppSidebar.vue中误删的"创建订单"菜单项
- 删除OrderListView.vue中"创建订单"按钮（由导航栏菜单项替代）

## 3. FinanceController NPE修复
- `buildSummaryData()` 在startDate=null时抛空指针
- 根因：`DateUtil.fillMonthRange()`结果未使用

## 4. 财务概览数据对接
- 后端返回 `{income, expense, profit}` 与前端期望 `{thisMonthIncome, ...}` 不匹配
- 已重写接口，新增Mapper查询方法

## 5. 科技风数据看板 🚀
- **后端**: `GET /dashboard/board` 聚合所有看板数据
- **前端**: 完全重写 DashboardView.vue
- **风格**: 深色背景 + 发光边框 + 毛玻璃卡片
- **内容**: KPI指标行 → 趋势图/饼图/未完成订单(三列) → 财务流水表
- **数据保留**: 未完成订单列表

## 待重启后测试
- 后端须重启使改动生效
