<template>
  <div class="dashboard-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item active">首页</span>
    </div>
    
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">欢迎回来，系统管理员</h1>
      <div class="page-actions">
        <button class="btn btn-success" @click="showQuickAccount">⚡ 快速记账</button>
        <button class="btn btn-primary" @click="router.push('/orders/create')">+ 创建订单</button>
      </div>
    </div>
    
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon blue">📋</div>
        <div class="stat-info">
          <h3>{{ dashboard.todayOrders }}</h3>
          <p>今日订单</p>
          <div class="stat-trend" :class="dashboard.orderTrend >= 0 ? 'up' : 'down'">
            {{ dashboard.orderTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(dashboard.orderTrend) }}% 较昨日
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">💰</div>
        <div class="stat-info">
          <h3>¥{{ dashboard.todayRevenue.toLocaleString() }}</h3>
          <p>今日营收</p>
          <div class="stat-trend" :class="dashboard.revenueTrend >= 0 ? 'up' : 'down'">
            {{ dashboard.revenueTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(dashboard.revenueTrend) }}% 较昨日
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">👥</div>
        <div class="stat-info">
          <h3>{{ dashboard.totalCustomers.toLocaleString() }}</h3>
          <p>客户总数</p>
          <div class="stat-trend up">↑ {{ dashboard.newCustomers }} 新增</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">📦</div>
        <div class="stat-info">
          <h3>{{ dashboard.stockWarnings }}</h3>
          <p>库存预警</p>
          <div class="stat-trend" :class="dashboard.stockWarnings > 0 ? 'down' : 'up'">
            {{ dashboard.stockWarnings > 0 ? '需处理' : '正常' }}
          </div>
        </div>
      </div>
    </div>
    
    <!-- 每日流水看板 -->
    <div class="card">
      <div class="card-header">
        <div class="card-title">💵 每日流水看板 <span class="v2-badge">V2.2</span></div>
      </div>
      <div class="dashboard-grid">
        <div class="dashboard-item">
          <div class="dashboard-value" style="color:#67c23a;">¥{{ dashboard.todayFlow.toLocaleString() }}</div>
          <div class="dashboard-label">今日流水</div>
        </div>
        <div class="dashboard-item">
          <div class="dashboard-value" style="color:#409eff;">¥{{ dashboard.weekFlow.toLocaleString() }}</div>
          <div class="dashboard-label">本周流水</div>
        </div>
        <div class="dashboard-item">
          <div class="dashboard-value" style="color:#e6a23c;">¥{{ dashboard.monthFlow.toLocaleString() }}</div>
          <div class="dashboard-label">本月流水</div>
        </div>
        <div class="dashboard-item">
          <div class="dashboard-value">{{ dashboard.todayTxCount }}</div>
          <div class="dashboard-label">今日笔数</div>
        </div>
      </div>
    </div>
    
    <!-- 快捷操作 -->
    <div class="card">
      <div class="card-header">
        <div class="card-title">⚡ 快捷操作</div>
      </div>
      <div class="quick-actions">
        <button class="quick-action-btn" @click="showQuickAccount">
          <div class="quick-action-icon">💵</div>
          <div class="quick-action-text">快速记账</div>
        </button>
        <button class="quick-action-btn" @click="router.push('/orders/create')">
          <div class="quick-action-icon">📋</div>
          <div class="quick-action-text">创建订单</div>
        </button>
        <button class="quick-action-btn" @click="router.push('/customers')">
          <div class="quick-action-icon">👥</div>
          <div class="quick-action-text">添加客户</div>
        </button>
        <button class="quick-action-btn" @click="router.push('/square/publish')">
          <div class="quick-action-icon">🎨</div>
          <div class="quick-action-text">发布需求</div>
        </button>
      </div>
    </div>
    
    <!-- 图表区域 -->
    <div class="charts-grid">
      <div class="chart-card">
        <div class="chart-title">📈 订单趋势（近7天）</div>
        <div class="chart-container">
          <div v-for="(v, i) in chartData" :key="i" class="chart-bar-wrap">
            <div class="chart-bar" :style="{ height: (v / maxChartValue * 180) + 'px' }"></div>
            <span class="chart-label">周{{ weekDays[i] }}</span>
          </div>
          <div v-if="chartData.length === 0" class="chart-empty">暂无数据</div>
        </div>
      </div>
      <div class="chart-card">
        <div class="chart-title">💰 营收构成</div>
        <div class="pie-container">
          <div class="pie-chart" :style="pieGradient">
            <div class="pie-center">
              <span class="pie-value">¥{{ formatAmount(dashboard.monthRevenue) }}</span>
              <span class="pie-label">本月营收</span>
            </div>
          </div>
          <div class="pie-legend" v-if="dashboard.revenueBreakdown?.length">
            <span v-for="item in dashboard.revenueBreakdown" :key="item.name">
              <span class="pie-dot" :style="{ background: item.color }"></span>
              {{ item.name }} {{ item.percent }}%
            </span>
          </div>
          <div v-else class="pie-legend">
            <span style="color:#909399;">暂无数据</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 待处理事项 -->
    <div class="card">
      <div class="card-header">
        <div class="card-title">📋 待处理事项</div>
        <button class="btn btn-default btn-sm">查看全部</button>
      </div>
      <table class="data-table" v-if="pendingItems.length > 0">
        <thead>
          <tr>
            <th>类型</th>
            <th>内容</th>
            <th>时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in pendingItems" :key="item.id">
            <td><span class="status-tag" :class="'status-' + item.type">{{ item.typeText }}</span></td>
            <td>{{ item.content }}</td>
            <td>{{ item.time }}</td>
            <td><button class="btn btn-primary btn-sm">{{ item.action }}</button></td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无待处理事项</div>
    </div>
    
    <!-- 最近订单 -->
    <div class="card">
      <div class="card-header">
        <div class="card-title">🕐 最近订单</div>
        <button class="btn btn-default btn-sm" @click="router.push('/orders')">查看全部</button>
      </div>
      <table class="data-table" v-if="recentOrders.length > 0">
        <thead>
          <tr>
            <th>订单编号</th>
            <th>客户名称</th>
            <th>订单金额</th>
            <th>订单状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in recentOrders" :key="order.orderNo">
            <td>{{ order.orderNo }}</td>
            <td>{{ order.customerName }}</td>
            <td>¥{{ order.amount.toLocaleString() }}.00</td>
            <td>
              <span class="status-tag" :class="'status-' + order.status">{{ order.statusText }}</span>
            </td>
            <td>{{ order.createTime }}</td>
            <td class="action-btns">
              <button class="action-btn view">查看</button>
              <button class="action-btn edit" v-if="order.status !== 'completed'">编辑</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-tip">暂无订单数据</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'

const router = useRouter()

const weekDays = ['一', '二', '三', '四', '五', '六', '日']
const chartData = ref<number[]>([])
const maxChartValue = computed(() => Math.max(...chartData.value, 1))

const dashboard = reactive({
  todayOrders: 0,
  todayRevenue: 0,
  totalCustomers: 0,
  stockWarnings: 0,
  orderTrend: 0,
  revenueTrend: 0,
  newCustomers: 0,
  todayFlow: 0,
  weekFlow: 0,
  monthFlow: 0,
  todayTxCount: 0,
  monthRevenue: 0,
  revenueBreakdown: [] as { name: string; percent: number; color: string }[],
})

const recentOrders = ref<any[]>([])
const pendingItems = ref<any[]>([])

const pieGradient = computed(() => {
  const list = dashboard.revenueBreakdown
  if (!list || list.length === 0) {
    return { background: 'conic-gradient(#e4e7ed 0% 100%)' }
  }
  let gradient = ''
  let cum = 0
  for (const item of list) {
    const start = cum
    cum += item.percent
    gradient += `${item.color} ${start}% ${cum}%, `
  }
  return { background: `conic-gradient(${gradient.slice(0, -2)})` }
})

function formatAmount(v: number) {
  if (v >= 10000) return (v / 10000).toFixed(1) + 'W'
  if (v >= 1000) return (v / 1000).toFixed(1) + 'K'
  return String(v)
}

const showQuickAccount = () => {
  window.dispatchEvent(new CustomEvent('show-quick-account'))
}

const loadDashboard = async () => {
  try {
    const res = await request.get('/dashboard/stats')
    const data = res.data || {}
    dashboard.todayOrders = data.todayOrders || 0
    dashboard.todayRevenue = data.todayRevenue || 0
    dashboard.totalCustomers = data.totalCustomers || 0
    dashboard.stockWarnings = data.stockWarnings || 0
    dashboard.orderTrend = data.orderTrend || 0
    dashboard.revenueTrend = data.revenueTrend || 0
    dashboard.newCustomers = data.newCustomers || 0
    dashboard.todayFlow = data.todayFlow || 0
    dashboard.weekFlow = data.weekFlow || 0
    dashboard.monthFlow = data.monthFlow || 0
    dashboard.todayTxCount = data.todayTxCount || 0
    dashboard.monthRevenue = data.monthRevenue || 0
    dashboard.revenueBreakdown = data.revenueBreakdown || []
    if (data.chartData?.length) {
      chartData.value = data.chartData
    }
  } catch {
    // 静默失败，保持默认0值
  }
}

const loadRecentOrders = async () => {
  try {
    const res = await request.get('/orders', { params: { page: 1, size: 5 } })
    const records = res.data?.records || res.data || []
    recentOrders.value = records.map((o: any) => ({
      orderNo: o.orderNo || o.orderNo,
      customerName: o.customerName || '',
      amount: o.amount || 0,
      status: o.status || '',
      statusText: o.statusText || o.status || '',
      createTime: o.createTime || '',
    }))
  } catch {
    recentOrders.value = []
  }
}

const loadPendingItems = async () => {
  try {
    const res = await request.get('/orders', { params: { page: 1, size: 10, status: 1 } })
    const records = res.data?.records || res.data || []
    pendingItems.value = records.map((o: any) => ({
      id: o.id,
      type: 'pending',
      typeText: '待确认',
      content: `客户"${o.customerName || ''}"提交订单 ${o.orderNo || ''}`,
      time: o.createTime || '',
      action: '处理',
    }))
  } catch {
    pendingItems.value = []
  }
}

onMounted(() => {
  loadDashboard()
  loadRecentOrders()
  loadPendingItems()
})
</script>

<style scoped lang="scss">
// 面包屑
.breadcrumb {
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 18px; font-size: 12px; color: $text-secondary;
  background: #fff; padding: 10px 16px; border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}
.breadcrumb-item { cursor: pointer; transition: color 0.2s; }
.breadcrumb-item:hover { color: $primary; }
.breadcrumb-item.active { color: $text-primary; font-weight: 500; cursor: default; }

// 页面标题
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
}
.page-title { font-size: 20px; font-weight: 700; color: $text-primary; }
.page-actions { display: flex; align-items: center; gap: 10px; }

// 按钮
.btn {
  padding: 9px 20px; border-radius: 7px; border: none;
  cursor: pointer; font-size: 13px; font-weight: 500;
  display: inline-flex; align-items: center; gap: 6px;
  transition: all 0.25s; white-space: nowrap;
}
.btn-primary {
  background: $primary; color: #fff;
  box-shadow: 0 2px 6px rgba(64,158,255,0.3);
  &:hover { background: #66b1ff; box-shadow: 0 4px 12px rgba(64,158,255,0.4); }
}
.btn-success {
  background: $success; color: #fff;
  box-shadow: 0 2px 6px rgba(103,194,58,0.3);
}
.btn-default {
  background: #fff; border: 1px solid $border-light; color: $text-primary;
  &:hover { border-color: $primary; color: $primary; background: #ecf5ff; }
}
.btn-sm { padding: 6px 13px; font-size: 12px; border-radius: 5px; }
.btn-warning {
  background: $warning; color: #fff;
  &:hover { background: #ebb563; }
}

// 统计卡片
.stats-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px; margin-bottom: 20px;
}
.stat-card {
  background: #fff; border-radius: 12px; padding: 18px 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: $shadow-card; transition: all 0.3s;
  border: 1px solid rgba(0,0,0,0.04);
  &:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.1); }
}
.stat-icon {
  width: 54px; height: 54px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; color: #fff; flex-shrink: 0;
  &.blue { background: linear-gradient(135deg, #409eff, #66b1ff); box-shadow: 0 4px 10px rgba(64,158,255,0.3); }
  &.green { background: linear-gradient(135deg, #67c23a, #85ce61); box-shadow: 0 4px 10px rgba(103,194,58,0.3); }
  &.orange { background: linear-gradient(135deg, #e6a23c, #ebb563); box-shadow: 0 4px 10px rgba(230,162,60,0.3); }
  &.red { background: linear-gradient(135deg, #f56c6c, #f78989); box-shadow: 0 4px 10px rgba(245,108,108,0.3); }
}
.stat-info h3 { font-size: 22px; font-weight: 700; margin-bottom: 4px; }
.stat-info p { font-size: 12px; color: $text-secondary; }
.stat-trend {
  display: flex; align-items: center; gap: 3px;
  font-size: 12px; margin-top: 5px;
  &.up { color: $success; }
  &.down { color: $danger; }
}

// 卡片
.card {
  background: #fff; border-radius: 12px;
  box-shadow: $shadow-card; padding: 20px;
  margin-bottom: 20px; border: 1px solid rgba(0,0,0,0.04);
}
.card-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px; padding-bottom: 14px;
  border-bottom: 1px solid $bg-base;
}
.card-title { font-size: 15px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.v2-badge {
  background: $success; color: #fff; font-size: 9px;
  padding: 2px 5px; border-radius: 3px; font-weight: normal;
}

// 流水看板
.dashboard-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px;
}
.dashboard-item {
  background: $bg-base; border-radius: 10px;
  padding: 15px; text-align: center;
}
.dashboard-value { font-size: 28px; font-weight: 700; color: $primary; margin-bottom: 5px; }
.dashboard-label { font-size: 12px; color: $text-secondary; }

// 快捷操作
.quick-actions { display: flex; gap: 15px; }
.quick-action-btn {
  flex: 1; padding: 20px; background: #fff;
  border: 2px dashed $border-light; border-radius: 10px;
  cursor: pointer; text-align: center; transition: all 0.3s;
  &:hover { border-color: $primary; background: #ecf5ff; }
}
.quick-action-icon { font-size: 30px; margin-bottom: 10px; }
.quick-action-text { font-size: 14px; color: $text-secondary; }

// 图表
.charts-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(380px, 1fr)); gap: 20px;
  margin-bottom: 20px;
}
.chart-card {
  background: #fff; border-radius: 12px; padding: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06); border: 1px solid rgba(0,0,0,0.04);
}
.chart-title { font-size: 14px; font-weight: 600; margin-bottom: 18px; color: $text-primary; }
.chart-container {
  height: 240px; display: flex; align-items: flex-end;
  gap: 10px; padding: 10px 0; position: relative;
}
.chart-bar-wrap {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; gap: 8px;
}
.chart-bar {
  width: 100%; background: linear-gradient(to top, #409eff, #66b1ff);
  border-radius: 4px 4px 0 0; min-height: 10px;
  transition: height 0.6s ease;
}
.chart-label { font-size: 11px; color: $text-secondary; }
.chart-empty {
  position: absolute; inset: 0; display: flex;
  align-items: center; justify-content: center;
  color: #909399; font-size: 14px;
}

// 饼图
.pie-container { display: flex; flex-direction: column; align-items: center; }
.pie-chart {
  width: 180px; height: 180px; border-radius: 50%;
  background: conic-gradient(#e4e7ed 0% 100%);
  display: flex; align-items: center; justify-content: center;
  transition: background 0.5s ease;
}
.pie-center {
  width: 120px; height: 120px; background: #fff; border-radius: 50%;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.pie-value { font-size: 20px; font-weight: 700; }
.pie-label { font-size: 11px; color: $text-secondary; }
.pie-legend {
  display: flex; justify-content: center; gap: 15px;
  margin-top: 15px; font-size: 12px;
}
.pie-dot {
  display: inline-block; width: 8px; height: 8px;
  border-radius: 50%; margin-right: 4px;
}

// 表格
.data-table {
  width: 100%; border-collapse: collapse;
  th {
    background: #fafbfc; padding: 11px 14px;
    text-align: left; font-weight: 600; font-size: 12px;
    color: $text-secondary; border-bottom: 1px solid #eef0f2;
    letter-spacing: 0.3px;
  }
  td {
    padding: 13px 14px; border-bottom: 1px solid $bg-base;
    font-size: 13px;
  }
  tr:last-child td { border-bottom: none; }
  tr:hover td { background: rgba(64,158,255,0.03); }
}

.empty-tip {
  text-align: center; padding: 40px 0; color: #909399; font-size: 14px;
}

// 状态标签
.status-tag {
  display: inline-block; padding: 4px 10px; border-radius: 4px;
  font-size: 12px; font-weight: 500;
}
.status-pending { background: #ecf5ff; color: #409eff; }
.status-designing { background: #fdf6ec; color: #e6a23c; }
.status-delivery { background: #f0f9eb; color: #67c23a; }
.status-completed { background: #e6f7ff; color: #1890ff; }
.status-cancelled { background: #fef0f0; color: #f56c6c; }

// 操作按钮
.action-btns { display: flex; gap: 5px; }
.action-btn {
  padding: 5px 10px; border-radius: 4px; border: none;
  cursor: pointer; font-size: 12px; transition: all 0.3s;
  &.view { background: #ecf5ff; color: $primary; }
  &.edit { background: #fdf6ec; color: $warning; }
  &.delete { background: #fef0f0; color: $danger; }
  &:hover { opacity: 0.8; }
}
</style>
