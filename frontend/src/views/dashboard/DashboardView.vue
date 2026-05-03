<template>
  <div class="dashboard-wrap">
    <!-- 背景粒子/网格 -->
    <div class="bg-grid"></div>
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>
    <div class="bg-glow bg-glow-3"></div>

    <!-- 顶部标题栏 -->
    <div class="header-bar">
      <div class="header-left">
        <div class="logo-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="#00d4ff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="header-title">企业广告管理系统 · 数据驾驶舱</div>
        <div class="header-subtitle">AD MANAGEMENT SYSTEM</div>
      </div>
      <div class="header-center">
        <div class="date-tag">
          <span class="date-label">统计日期</span>
          <span class="date-value">{{ dateStr }}</span>
        </div>
      </div>
      <div class="header-right">
        <div class="time-display">{{ now }}</div>
        <div class="weekday">{{ weekday }}</div>
      </div>
    </div>

    <!-- 主体内容区 -->
    <div class="main-content">

      <!-- ===== 左侧栏 ===== -->
      <div class="col-left">
        <!-- KPI 卡片组 -->
        <div class="kpi-grid">
          <div class="kpi-card" v-for="kpi in kpiList" :key="kpi.label">
            <div class="kpi-bg-icon">{{ kpi.icon }}</div>
            <div class="kpi-content">
              <div class="kpi-value" :style="{ color: kpi.color }">{{ kpi.value }}</div>
              <div class="kpi-label">{{ kpi.label }}</div>
            </div>
            <div class="kpi-indicator" :style="{ background: kpi.color }"></div>
          </div>
        </div>

        <!-- 待收款订单 -->
        <div class="board-card pending-orders-card">
          <div class="card-title-bar">
            <span class="card-title-dot"></span>
            <span>💸 待收款订单</span>
            <span class="badge-warn" v-if="pendingPaymentOrders.length">{{ pendingPaymentOrders.length }} 笔待收</span>
          </div>
          <div class="pending-summary" v-if="pendingPaymentOrders.length">
            <div class="pending-stat">
              <span class="pending-stat-label">待收总额</span>
              <span class="pending-stat-value">¥{{ formatMoney(pendingTotalAmount) }}</span>
            </div>
          </div>
          <div class="pending-order-list">
            <div
              v-for="order in pendingPaymentOrders.slice(0, 6)"
              :key="order.id"
              class="pending-order-item"
            >
              <div class="po-left">
                <div class="po-customer">{{ order.customerName || '散客' }}</div>
                <div class="po-order-no">{{ order.orderNo }}</div>
              </div>
              <div class="po-right">
                <div class="po-amount">¥{{ formatMoney(order.unpaidAmount) }}</div>
                <div class="po-status" :class="order.paymentStatus === 1 ? 'status-unpaid' : 'status-partial'">
                  {{ order.paymentStatus === 1 ? '未付' : '部分付' }}
                </div>
              </div>
            </div>
            <div v-if="!pendingPaymentOrders.length" class="empty-tip">🎉 所有订单已收款</div>
          </div>
        </div>

        <!-- 客户类型分布 -->
        <div class="board-card">
          <div class="card-title-bar">
            <span class="card-title-dot"></span>
            <span>客户类型分布</span>
          </div>
          <div class="customer-type-chart">
            <div class="ct-bar" v-for="item in customerTypeData" :key="item.name">
              <div class="ct-info">
                <span class="ct-name">{{ item.name }}</span>
                <span class="ct-count">{{ item.count }}</span>
              </div>
              <div class="ct-track">
                <div class="ct-fill" :style="{ width: item.pct + '%', background: item.color }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 中间区域 ===== -->
      <div class="col-center">
        <!-- 环形总览 -->
        <div class="board-card hub-card">
          <div class="hub-content">
            <div class="hub-ring">
              <svg viewBox="0 0 200 200" class="hub-svg">
                <!-- 外圈 -->
                <circle cx="100" cy="100" r="85" fill="none" stroke="rgba(0,212,255,0.08)" stroke-width="8"/>
                <!-- 进度弧 -->
                <circle cx="100" cy="100" r="85" fill="none" :stroke="hubArc.color" stroke-width="8"
                  :stroke-dasharray="hubArc.circum"
                  :stroke-dashoffset="hubArc.offset"
                  transform="rotate(-90, 100, 100)"
                  stroke-linecap="round"/>
                <!-- 内圈 -->
                <circle cx="100" cy="100" r="65" fill="none" stroke="rgba(0,212,255,0.05)" stroke-width="4"/>
              </svg>
              <div class="hub-center">
                <div class="hub-main-value">{{ hubMainValue }}</div>
                <div class="hub-main-label">{{ hubMainLabel }}</div>
              </div>
            </div>
            <div class="hub-stats">
              <div class="hub-stat" v-for="s in hubStats" :key="s.label">
                <div class="hub-stat-value" :style="{ color: s.color }">{{ s.value }}</div>
                <div class="hub-stat-label">{{ s.label }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 近30天收款趋势 -->
        <div class="board-card trend-card">
          <div class="card-title-bar">
            <span class="card-title-dot"></span>
            <span>📈 近30天收款趋势</span>
          </div>
          <div class="trend-chart">
            <div class="bar-wrapper">
              <div
                v-for="(v, i) in incomeTrend"
                :key="i"
                class="bar-item"
                :title="'¥' + (incomeTrendAmounts[i] || 0).toFixed(2)"
              >
                <div
                  class="bar-fill"
                  :style="{ height: v + '%', background: v > 60 ? 'linear-gradient(to top, #00d4ff, #7b2ff7)' : 'linear-gradient(to top, #00d4ff, #0099ff)' }"
                ></div>
              </div>
            </div>
            <div class="bar-labels">
              <span>30天前</span>
              <span>今天</span>
            </div>
          </div>
        </div>

        <!-- 订单状态分布 -->
        <div class="board-card pie-card">
          <div class="card-title-bar">
            <span class="card-title-dot"></span>
            <span>订单状态分布</span>
          </div>
          <div class="pie-content">
            <div class="pie-canvas">
              <svg viewBox="0 0 120 120" class="pie-svg">
                <circle cx="60" cy="60" r="50" fill="none" stroke="#1e2a3a" stroke-width="18"/>
                <circle
                  v-for="(seg, i) in pieSegments"
                  :key="i"
                  cx="60" cy="60" r="50" fill="none"
                  :stroke="seg.color"
                  stroke-width="18"
                  :stroke-dasharray="seg.circum"
                  :stroke-dashoffset="seg.offset"
                  transform="rotate(-90, 60, 60)"
                  style="transition: all 1s ease;"
                />
              </svg>
              <div class="pie-center-text">{{ pieTotal }}<span class="pie-unit">单</span></div>
            </div>
            <div class="pie-legend">
              <div class="legend-row" v-for="s in pieSegments" :key="s.label">
                <span class="legend-dot" :style="{ background: s.color }"></span>
                <span class="legend-label">{{ s.label }}</span>
                <span class="legend-val">{{ s.count }}单</span>
                <span class="legend-pct">{{ s.percent }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 右侧栏 ===== -->
      <div class="col-right">
        <!-- 今日概况 -->
        <div class="board-card today-card">
          <div class="card-title-bar">
            <span class="card-title-dot"></span>
            <span>今日概况</span>
          </div>
          <div class="today-grid">
            <div class="today-item" v-for="item in todayStats" :key="item.label">
              <div class="today-icon">{{ item.icon }}</div>
              <div class="today-info">
                <div class="today-value" :style="{ color: item.color }">{{ item.value }}</div>
                <div class="today-label">{{ item.label }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 支付渠道占比 -->
        <div class="board-card channel-card">
          <div class="card-title-bar">
            <span class="card-title-dot"></span>
            <span>支付渠道占比</span>
          </div>
          <div class="channel-list">
            <div class="channel-item" v-for="(item, idx) in channelData" :key="item.name">
              <div class="channel-rank" :class="{ top: idx < 3 }">{{ idx + 1 }}</div>
              <div class="channel-info">
                <span class="channel-name">{{ item.name }}</span>
                <span class="channel-amount">¥{{ formatMoney(item.amount) }}</span>
              </div>
              <div class="channel-bar-track">
                <div class="channel-bar-fill" :style="{ width: item.pct + '%', background: item.color }"></div>
              </div>
              <span class="channel-pct">{{ item.pct }}%</span>
            </div>
          </div>
        </div>

        <!-- 未完成订单 TOP -->
        <div class="board-card order-card-panel">
          <div class="card-title-bar">
            <span class="card-title-dot"></span>
            <span>待处理订单</span>
            <span class="badge-warn" v-if="unfinishedOrders.length">{{ unfinishedOrders.length }} 笔</span>
          </div>
          <div class="order-scroll-list">
            <div
              v-for="order in unfinishedOrders.slice(0, 8)"
              :key="order.id"
              class="order-item"
            >
              <div class="order-item-left">
                <span class="order-no">{{ order.orderNo }}</span>
                <span class="order-customer">{{ order.customerName }}</span>
              </div>
              <div class="order-item-center">
                <span class="order-amount">¥{{ formatMoney(order.totalAmount) }}</span>
              </div>
              <div class="order-item-right">
                <span class="order-status-tag" :class="getStatusClass(order)">{{ getMainStatus(order) }}</span>
              </div>
            </div>
            <div v-if="!unfinishedOrders.length" class="empty-tip">🎉 暂无待处理订单</div>
          </div>
        </div>

        <!-- 最近流水 -->
        <div class="board-card flow-card">
          <div class="card-title-bar">
            <span class="card-title-dot"></span>
            <span>最近财务流水</span>
          </div>
          <div class="flow-table-wrap">
            <table class="flow-table">
              <thead>
                <tr>
                  <th>编号</th>
                  <th>类别</th>
                  <th>金额</th>
                  <th>方向</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in recentRecords.slice(0, 6)" :key="r.id">
                  <td class="mono">{{ (r.recordNo || '').slice(-8) }}</td>
                  <td>{{ r.category || '-' }}</td>
                  <td :style="{ color: r.type === 'income' ? '#00d4ff' : '#ff6b6b', fontWeight: 600 }">
                    {{ r.type === 'income' ? '+' : '-' }}¥{{ formatMoney(r.amount) }}
                  </td>
                  <td>
                    <span class="flow-dir" :class="r.type === 'income' ? 'dir-in' : 'dir-out'">
                      {{ r.type === 'income' ? '收入' : '支出' }}
                    </span>
                  </td>
                </tr>
                <tr v-if="!recentRecords.length"><td colspan="4" class="empty-tip">暂无流水</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部版本信息 -->
    <div class="footer-bar">
      <span>V3.1</span>
      <span class="sep">|</span>
      <span>企业广告管理系统</span>
      <span class="sep">|</span>
      <span>数据更新时间: {{ updateTime }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '@/api/request'

// ── 数据 ──
const kpiList = ref<any[]>([])
const incomeTrend = ref<number[]>([])
const incomeTrendAmounts = ref<number[]>([])
const unfinishedOrders = ref<any[]>([])
const recentRecords = ref<any[]>([])
const orderDist = ref<any>({})
const now = ref('')
const dateStr = ref('')
const weekday = ref('')
const updateTime = ref('')
const totalCustomers = ref(0)
const todayStats = ref<any[]>([])
const pendingPaymentOrders = ref<any[]>([])

// 模拟今日数据（实际应从后端获取）
const todayData = ref({
  newOrders: 0,
  todayIncome: 0,
  todayExpense: 0,
  todayTxCount: 0
})

// 渠道数据
const channelData = ref<any[]>([
  { name: '微信支付', amount: 0, pct: 0, color: '#07c160' },
  { name: '支付宝', amount: 0, pct: 0, color: '#1677ff' },
  { name: '现金', amount: 0, pct: 0, color: '#ff7875' },
  { name: '转账', amount: 0, pct: 0, color: '#ffa940' },
])

// 客户类型分布
const customerTypeData = ref<any[]>([])

// 环形总览
const hubArc = computed(() => {
  const total = pieTotal.value || 1
  const completed = orderDist.value.completed || 0
  const pct = completed / total
  const circum = 2 * Math.PI * 85
  return {
    color: '#00d4ff',
    circum: `${circum * pct} ${circum}`,
    offset: 0
  }
})

const hubMainValue = computed(() => {
  return kpiList.value.find(k => k.label === '本月收款')?.value || '¥0' 
})

const hubMainLabel = computed(() => '本月收款')

const hubStats = computed(() => [
  { label: '本月订单', value: kpiList.value.find(k => k.label === '本月订单')?.value || '0', color: '#66bb6a' },
  { label: '待收款', value: kpiList.value.find(k => k.label === '待收款')?.value || '¥0', color: '#ffa726' },
  { label: '客户总数', value: totalCustomers.value + ' 个', color: '#ab47bc' },
  { label: '利润率', value: kpiList.value.find(k => k.label === '利润率')?.value || '0%', color: '#00d4ff' },
])

// 饼图数据
const pieSegments = computed(() => {
  const d = orderDist.value || {}
  const total = d.pending + d.processing + d.completed + d.cancelled || 1
  const items = [
    { label: '待确认', key: 'pending', count: d.pending || 0, color: '#ffa726' },
    { label: '进行中', key: 'processing', count: d.processing || 0, color: '#42a5f5' },
    { label: '已完成', key: 'completed', count: d.completed || 0, color: '#66bb6a' },
    { label: '已取消', key: 'cancelled', count: d.cancelled || 0, color: '#ef5350' },
  ]
  const circum = 2 * Math.PI * 50
  let offset = 0
  return items.map(item => {
    const pct = item.count / total
    const segLen = circum * Math.max(pct, 0.01)
    const seg = {
      ...item,
      percent: Math.round(pct * 100),
      circum: `${segLen} ${circum - segLen}`,
      offset: -offset,
    }
    offset += segLen
    return seg
  })
})

// 待收款总额
const pendingTotalAmount = computed(() => {
  return pendingPaymentOrders.value.reduce((sum: number, o: any) => sum + (o.unpaidAmount || 0), 0)
})

const pieTotal = computed(() => {
  const d = orderDist.value || {}
  return d.pending + d.processing + d.completed + d.cancelled || 0
})

// ── 工具函数 ──
function formatMoney(v: any) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function getMainStatus(order: any) {
  const ps = order.paymentStatus
  const os = order.status
  const unpaid = (order.totalAmount || 0) - (order.paidAmount || 0) - (order.roundingAmount || 0)
  if (unpaid > 0 && ps === 1) return '待收款'
  if (unpaid > 0 && ps === 2) return '部分收款'
  if (os === 2) return '进行中'
  if (os === 1) return '待处理'
  return '进行中'
}

function getStatusClass(order: any) {
  const s = getMainStatus(order)
  if (s === '待收款') return 'tag-danger'
  if (s === '部分收款') return 'tag-warn'
  if (s === '进行中') return 'tag-primary'
  return 'tag-info'
}

// ── 定时刷新时间 ──
function updateClock() {
  const d = new Date()
  now.value = d.toLocaleTimeString('zh-CN', { hour12: false })
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  weekday.value = weekdays[d.getDay()]
  dateStr.value = d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

// ── 加载待收款订单 ──
async function loadPendingPaymentOrders() {
  try {
    const res = await request.get('/orders/pending-payment')
    pendingPaymentOrders.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    console.error('待收款订单加载失败', e)
    pendingPaymentOrders.value = []
  }
}

// ── 加载数据 ──
async function loadBoard() {
  try {
    const res = await request.get('/dashboard/board')
    const d = res.data || {}

    const kpi = d.kpi || {}
    const profitRate = kpi.profitRate || 0
    const profitColor = profitRate > 0 ? '#00d4ff' : profitRate < 0 ? '#ff6b6b' : '#909399'

    kpiList.value = [
      { label: '本月收款', value: '¥' + formatMoney(kpi.thisMonthIncome), icon: '💰', color: '#00d4ff', bg: 'rgba(0,212,255,0.12)' },
      { label: '本月订单', value: (kpi.thisMonthOrders || 0) + ' 张', icon: '📋', color: '#66bb6a', bg: 'rgba(102,187,106,0.12)' },
      { label: '待收款', value: '¥' + formatMoney(kpi.unpaidAmount), icon: '⏳', color: '#ffa726', bg: 'rgba(255,167,38,0.12)' },
      { label: '客户总数', value: (d.totalCustomers || 0) + ' 个', icon: '👥', color: '#ab47bc', bg: 'rgba(171,71,188,0.12)' },
      { label: '利润率', value: profitRate + '%', icon: '📈', color: profitColor, bg: 'rgba(0,212,255,0.12)', trend: profitRate > 0 ? '↑ 盈利' : profitRate < 0 ? '↓ 亏损' : '', trendColor: profitRate > 0 ? '#66bb6a' : '#ff6b6b' },
    ]

    incomeTrend.value = d.incomeTrend || []
    incomeTrendAmounts.value = d.incomeTrendAmounts || []
    unfinishedOrders.value = d.unfinishedOrders || []
    recentRecords.value = d.recentRecords || []
    orderDist.value = d.orderStatusDist || {}
    totalCustomers.value = d.totalCustomers || 0

    // 模拟今日数据（实际可从 /dashboard/stats 获取）
    todayStats.value = [
      { icon: '📦', label: '今日订单', value: todayData.value.newOrders + ' 单', color: '#42a5f5' },
      { icon: '💵', label: '今日收入', value: '¥' + formatMoney(todayData.value.todayIncome), color: '#52c41a' },
      { icon: '📤', label: '今日支出', value: '¥' + formatMoney(todayData.value.todayExpense), color: '#ff4d4f' },
      { icon: '🔢', label: '今日流水', value: todayData.value.todayTxCount + ' 笔', color: '#722ed1' },
    ]

    // 模拟渠道数据（实际可从后端获取）
    channelData.value = [
      { name: '微信支付', amount: Math.round((kpi.thisMonthIncome || 0) * 0.45), pct: 45, color: '#07c160' },
      { name: '支付宝', amount: Math.round((kpi.thisMonthIncome || 0) * 0.30), pct: 30, color: '#1677ff' },
      { name: '现金', amount: Math.round((kpi.thisMonthIncome || 0) * 0.15), pct: 15, color: '#ff7875' },
      { name: '转账', amount: Math.round((kpi.thisMonthIncome || 0) * 0.10), pct: 10, color: '#ffa940' },
    ]

    // 模拟客户类型数据
    const total = totalCustomers.value || 1
    customerTypeData.value = [
      { name: '普通客户', count: Math.round(total * 0.6), pct: 60, color: '#00d4ff' },
      { name: '会员客户', count: Math.round(total * 0.25), pct: 25, color: '#7b2ff7' },
      { name: '零售客户', count: Math.round(total * 0.15), pct: 15, color: '#52c41a' },
    ]

    updateTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch (e) {
    console.error('看板数据加载失败', e)
  }
  // 加载待收款订单
  loadPendingPaymentOrders()
}

let timer: any = null
onMounted(() => {
  updateClock()
  timer = setInterval(updateClock, 1000)
  loadBoard()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
/* ====== 全局 ====== */
.dashboard-wrap {
  min-height: 100vh;
  padding: 0 20px 20px;
  background: linear-gradient(135deg, #0a0e1a 0%, #0d1423 50%, #0a0e1a 100%);
  position: relative;
  overflow: hidden;
  color: #e0e6f0;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* 背景效果 */
.bg-grid {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 212, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 212, 255, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  pointer-events: none;
  z-index: 0;
}
.bg-glow {
  position: fixed;
  pointer-events: none;
  z-index: 0;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}
.bg-glow-1 {
  top: -150px;
  right: -100px;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.15) 0%, transparent 70%);
}
.bg-glow-2 {
  bottom: -100px;
  left: -100px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(123, 47, 247, 0.1) 0%, transparent 70%);
}
.bg-glow-3 {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.03) 0%, transparent 70%);
}

/* ====== 顶部标题栏 ====== */
.header-bar {
  position: relative;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0 14px;
  border-bottom: 1px solid rgba(0, 212, 255, 0.15);
  margin-bottom: 16px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.logo-icon {
  width: 40px;
  height: 40px;
  background: rgba(0, 212, 255, 0.1);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.header-title {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(90deg, #00d4ff, #7b2ff7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 2px;
}
.header-subtitle {
  font-size: 11px;
  color: rgba(0, 212, 255, 0.4);
  letter-spacing: 3px;
  margin-top: 2px;
}
.header-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}
.date-tag {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.date-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
}
.date-value {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}
.header-right {
  text-align: right;
}
.time-display {
  font-size: 32px;
  font-weight: 700;
  color: #00d4ff;
  font-family: 'Courier New', monospace;
  letter-spacing: 3px;
  text-shadow: 0 0 20px rgba(0, 212, 255, 0.4);
}
.weekday {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 2px;
}

/* ====== 主体布局 ====== */
.main-content {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 280px 1fr 320px;
  gap: 16px;
  height: calc(100vh - 140px);
}

.col-left,
.col-center,
.col-right {
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
}
.col-left::-webkit-scrollbar,
.col-right::-webkit-scrollbar {
  width: 3px;
}
.col-left::-webkit-scrollbar-thumb,
.col-right::-webkit-scrollbar-thumb {
  background: rgba(0, 212, 255, 0.2);
  border-radius: 2px;
}

/* ====== KPI 卡片 ====== */
.kpi-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.kpi-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
}
.kpi-card:hover {
  border-color: rgba(0, 212, 255, 0.2);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}
.kpi-bg-icon {
  font-size: 24px;
  opacity: 0.6;
  position: absolute;
  right: 10px;
  top: 10px;
}
.kpi-content { flex: 1; }
.kpi-value {
  font-size: 20px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  line-height: 1.2;
}
.kpi-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
  margin-top: 3px;
}
.kpi-indicator {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: 0 2px 2px 0;
}

/* ====== 通用卡片 ====== */
.board-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 16px;
  backdrop-filter: blur(12px);
  position: relative;
  overflow: hidden;
}
.board-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.3), transparent);
}
.card-title-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 14px;
  color: rgba(255, 255, 255, 0.85);
}
.card-title-dot {
  width: 4px;
  height: 14px;
  background: #00d4ff;
  border-radius: 2px;
  box-shadow: 0 0 8px rgba(0, 212, 255, 0.5);
}
.badge-warn {
  margin-left: auto;
  font-size: 10px;
  background: rgba(255, 167, 38, 0.15);
  color: #ffa726;
  padding: 2px 8px;
  border-radius: 8px;
  border: 1px solid rgba(255, 167, 38, 0.2);
}

/* ====== 待收款订单 ====== */
.pending-orders-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.pending-summary {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.pending-stat {
  flex: 1;
  background: rgba(255, 107, 107, 0.08);
  border: 1px solid rgba(255, 107, 107, 0.15);
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.pending-stat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}
.pending-stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #ff6b6b;
  font-family: 'Courier New', monospace;
}
.pending-order-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow-y: auto;
  max-height: 220px;
}
.pending-order-list::-webkit-scrollbar { width: 3px; }
.pending-order-list::-webkit-scrollbar-thumb { background: rgba(255, 107, 107, 0.2); border-radius: 2px; }
.pending-order-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.04);
  border-radius: 8px;
  transition: all 0.2s;
}
.pending-order-item:hover {
  background: rgba(255, 107, 107, 0.06);
  border-color: rgba(255, 107, 107, 0.15);
}
.po-left { display: flex; flex-direction: column; gap: 2px; }
.po-customer {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
}
.po-order-no {
  font-size: 11px;
  font-family: 'Courier New', monospace;
  color: rgba(255, 255, 255, 0.35);
}
.po-right { text-align: right; }
.po-amount {
  font-size: 15px;
  font-weight: 700;
  color: #ff6b6b;
  font-family: 'Courier New', monospace;
}
.po-status {
  font-size: 10px;
  padding: 1px 7px;
  border-radius: 5px;
  margin-top: 3px;
  display: inline-block;
}
.status-unpaid {
  background: rgba(255, 107, 107, 0.15);
  color: #ff6b6b;
  border: 1px solid rgba(255, 107, 107, 0.2);
}
.status-partial {
  background: rgba(255, 167, 38, 0.15);
  color: #ffa726;
  border: 1px solid rgba(255, 167, 38, 0.2);
}

/* ====== 客户类型分布 ====== */
.customer-type-chart {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ct-bar {}
.ct-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}
.ct-name {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}
.ct-count {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}
.ct-track {
  height: 8px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 4px;
  overflow: hidden;
}
.ct-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 1s ease;
}

/* ====== 中间 - 环形总览 ====== */
.hub-card {
  padding: 20px;
}
.hub-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.hub-ring {
  position: relative;
  width: 180px;
  height: 180px;
}
.hub-svg {
  width: 100%;
  height: 100%;
}
.hub-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.hub-main-value {
  font-size: 28px;
  font-weight: 700;
  color: #00d4ff;
  font-family: 'Courier New', monospace;
}
.hub-main-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 4px;
}
.hub-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 20px;
  margin-top: 20px;
  width: 100%;
}
.hub-stat {
  text-align: center;
  padding: 10px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 8px;
}
.hub-stat-value {
  font-size: 16px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
}
.hub-stat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 3px;
}

/* ====== 中间 - 趋势图 ====== */
.trend-chart {
  height: 120px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.bar-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 100px;
  padding: 2px 0;
}
.bar-item {
  flex: 1;
  display: flex;
  align-items: flex-end;
  height: 100%;
  position: relative;
}
.bar-fill {
  width: 100%;
  border-radius: 2px 2px 0 0;
  min-height: 2px;
  transition: height 0.8s ease;
  box-shadow: 0 0 6px rgba(0, 212, 255, 0.2);
}
.bar-labels {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.3);
  padding: 4px 2px 0;
}

/* ====== 中间 - 饼图 ====== */
.pie-content {
  display: flex;
  align-items: center;
  gap: 14px;
}
.pie-canvas {
  position: relative;
  width: 100px;
  height: 100px;
  flex-shrink: 0;
}
.pie-svg {
  width: 100%;
  height: 100%;
}
.pie-center-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #00d4ff;
}
.pie-unit {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  margin-left: 2px;
}
.pie-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.legend-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.legend-label {
  flex: 1;
  color: rgba(255, 255, 255, 0.6);
}
.legend-val {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  min-width: 36px;
}
.legend-pct {
  color: rgba(255, 255, 255, 0.4);
  min-width: 30px;
  text-align: right;
}

/* ====== 右侧 - 今日概况 ====== */
.today-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.today-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 8px;
}
.today-icon {
  font-size: 20px;
}
.today-info {}
.today-value {
  font-size: 15px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
}
.today-label {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 2px;
}

/* ====== 右侧 - 渠道占比 ====== */
.channel-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.channel-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.channel-rank {
  width: 18px;
  height: 18px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.5);
}
.channel-rank.top {
  background: rgba(0, 212, 255, 0.15);
  color: #00d4ff;
}
.channel-info {
  min-width: 70px;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.channel-name {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}
.channel-amount {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}
.channel-bar-track {
  flex: 1;
  height: 6px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 3px;
  overflow: hidden;
}
.channel-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 1s ease;
}
.channel-pct {
  min-width: 36px;
  text-align: right;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.6);
}

/* ====== 右侧 - 订单列表 ====== */
.order-card-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.order-scroll-list {
  flex: 1;
  overflow-y: auto;
  max-height: 200px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.order-scroll-list::-webkit-scrollbar {
  width: 3px;
}
.order-scroll-list::-webkit-scrollbar-thumb {
  background: rgba(0, 212, 255, 0.2);
  border-radius: 2px;
}
.order-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.02);
  transition: background 0.2s;
}
.order-item:hover {
  background: rgba(0, 212, 255, 0.06);
}
.order-item-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.order-no {
  font-size: 11px;
  font-family: 'Courier New', monospace;
  color: rgba(255, 255, 255, 0.5);
}
.order-customer {
  font-size: 12px;
  font-weight: 600;
}
.order-item-center {
  text-align: right;
}
.order-amount {
  font-size: 13px;
  font-weight: 700;
  color: #00d4ff;
}
.order-item-right {
  min-width: 55px;
  text-align: right;
}
.order-status-tag {
  display: inline-block;
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 5px;
  font-weight: 600;
}
.tag-danger {
  background: rgba(255, 107, 107, 0.15);
  color: #ff6b6b;
  border: 1px solid rgba(255, 107, 107, 0.2);
}
.tag-warn {
  background: rgba(255, 167, 38, 0.15);
  color: #ffa726;
  border: 1px solid rgba(255, 167, 38, 0.2);
}
.tag-primary {
  background: rgba(0, 212, 255, 0.15);
  color: #00d4ff;
  border: 1px solid rgba(0, 212, 255, 0.2);
}
.tag-info {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* ====== 右侧 - 流水表 ====== */
.flow-card {}
.flow-table-wrap {
  overflow-x: auto;
}
.flow-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.flow-table th {
  text-align: left;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.03);
  color: rgba(255, 255, 255, 0.4);
  font-weight: 500;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.flow-table td {
  padding: 8px 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
}
.flow-table tr:hover td {
  background: rgba(0, 212, 255, 0.03);
}
.mono {
  font-family: 'Courier New', monospace;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}
.flow-dir {
  display: inline-block;
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 5px;
  font-weight: 600;
}
.dir-in {
  background: rgba(0, 212, 255, 0.12);
  color: #00d4ff;
}
.dir-out {
  background: rgba(255, 107, 107, 0.12);
  color: #ff6b6b;
}
.empty-tip {
  text-align: center;
  padding: 16px 0;
  color: rgba(255, 255, 255, 0.25);
  font-size: 12px;
}

/* ====== 底部 ====== */
.footer-bar {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 10px 0 6px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.25);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}
.footer-bar .sep {
  margin: 0 8px;
  color: rgba(255, 255, 255, 0.1);
}

/* ====== 响应式 ====== */
@media (max-width: 1400px) {
  .main-content {
    grid-template-columns: 260px 1fr 280px;
  }
}
@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr 1fr;
    height: auto;
  }
  .col-center {
    grid-column: 1 / -1;
    order: -1;
  }
}
@media (max-width: 768px) {
  .main-content {
    grid-template-columns: 1fr;
  }
  .header-bar {
    flex-direction: column;
    gap: 10px;
    text-align: center;
  }
  .header-center {
    position: static;
    transform: none;
  }
  .header-right {
    text-align: center;
  }
}
</style>
