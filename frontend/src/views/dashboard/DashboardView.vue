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
    
    <!-- 未完成订单（进行中 + 未收款） -->
    <div v-if="activeOrders.length > 0" class="payment-reminder">
      <div class="reminder-header" @click="showActiveDetail = !showActiveDetail">
        <span class="reminder-icon">📋</span>
        <span class="reminder-title">未完成订单</span>
        <span class="reminder-count in-progress-count">{{ inProgressOrders.length }} 笔进行中</span>
        <span v-if="unpaidOrders.length > 0" class="reminder-count unpaid-count">{{ unpaidOrders.length }} 笔未收款</span>
        <span v-if="unpaidOrders.length > 0" class="reminder-total">
          待收 ¥{{ unpaidOrders.reduce((sum, o) => sum + o.unpaidAmount, 0).toFixed(2) }}
        </span>
        <span class="toggle-btn">{{ showActiveDetail ? '收起' : '展开' }} ▼</span>
      </div>

      <div v-show="showActiveDetail" class="reminder-cards">
        <!-- 进行中订单 -->
        <div
          v-for="order in inProgressOrders"
          :key="'p-' + order.id"
          class="order-card order-card--progress"
        >
          <div class="card-left">
            <div class="card-order-no">{{ order.orderNo }}</div>
            <div class="card-customer">{{ order.customerName }}</div>
            <div class="card-status-tag progress-tag">⚙️ 进行中</div>
          </div>
          <div class="card-center">
            <div class="card-amount-info">
              <span>总额 ¥{{ (order.totalAmount || 0).toFixed(2) }}</span>
              <template v-if="order.paidAmount > 0">
                <span class="card-divider">|</span>
                <span>已付 ¥{{ order.paidAmount.toFixed(2) }}</span>
              </template>
            </div>
            <div v-if="order.designerName" class="designer-tip">🎨 {{ order.designerName }}</div>
          </div>
          <div class="card-right">
            <button class="collect-btn" @click="completeOrder(order)">✅ 完成</button>
          </div>
        </div>

        <!-- 未收款订单（待处理状态） -->
        <div
          v-for="order in unpaidOrders"
          :key="'u-' + order.id"
          class="order-card"
          :class="{ 'partial-paid': order.paymentStatus === 2 }"
        >
          <div class="card-left">
            <div class="card-order-no">{{ order.orderNo }}</div>
            <div class="card-customer">{{ order.customerName }}</div>
            <div class="card-status-tag unpaid-tag">💰 未收款</div>
          </div>
          <div class="card-center">
            <div class="card-amount-info">
              <span>总额 ¥{{ order.totalAmount.toFixed(2) }}</span>
              <span class="card-divider">|</span>
              <span>已付 ¥{{ order.paidAmount.toFixed(2) }}</span>
              <span class="card-divider">|</span>
              <span class="card-unpaid">未付 ¥{{ order.unpaidAmount.toFixed(2) }}</span>
            </div>
            <div v-if="order.memberId && paymentMemberBalanceMap[order.id] > 0" class="member-balance-tip">
              🎫 会员预存：¥{{ (paymentMemberBalanceMap[order.id] || 0).toFixed(2) }}
            </div>
          </div>
          <div class="card-right">
            <button class="collect-btn" @click="showPayment(order)">收款</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 收款对话框 -->
    <div v-if="paymentDialogVisible" class="payment-modal-overlay" @click.self="paymentDialogVisible = false">
      <div class="payment-modal">
        <div class="modal-header">
          <h3>收款 — {{ paymentOrder?.orderNo }}</h3>
          <button class="close-btn" @click="paymentDialogVisible = false">×</button>
        </div>
        <div class="modal-body">
          <p class="modal-customer">客户：{{ paymentOrder?.customerName }}</p>
          <div class="modal-row"><label>订单金额：</label><span>¥{{ paymentOrder?.totalAmount?.toFixed(2) }}</span></div>
          <div class="modal-row"><label>已付金额：</label><span>¥{{ paymentOrder?.paidAmount?.toFixed(2) }}</span></div>
          <div class="modal-row highlight">
            <label>本次收款：</label>
            <input type="number" v-model.number="paymentForm.amount" min="0.01"
              :max="paymentOrder?.unpaidAmount" step="0.01" class="modal-input" placeholder="输入收款金额" />
          </div>
          <div v-if="paymentMemberBalance > 0" class="member-balance-card">
            <div class="balance-header">🎫 会员预存可用</div>
            <div class="balance-amount">¥{{ paymentMemberBalance.toFixed(2) }}</div>
            <div class="balance-hint">将优先从预存余额扣除（最多扣除 ¥{{ Math.min(paymentForm.amount, paymentMemberBalance).toFixed(2) }}）</div>
          </div>
          <div class="modal-row"><label>备注：</label>
            <input type="text" v-model="paymentForm.remark" class="modal-input" placeholder="可选备注" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="paymentDialogVisible = false">取消</button>
          <button class="confirm-btn" @click="confirmPayment">确认收款</button>
        </div>
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
        <button class="btn btn-default btn-sm" @click="router.push('/orders')">查看全部</button>
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
            <td><button class="btn btn-primary btn-sm" @click="router.push(item.actionRoute || '/orders')">{{ item.action }}</button></td>
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
import { ElMessage, ElMessageBox } from 'element-plus'

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

// ========== 未完成订单（进行中 + 未收款） ==========
const unpaidOrders = ref<any[]>([])
const inProgressOrders = ref<any[]>([])
const activeOrders = computed(() => [...inProgressOrders.value, ...unpaidOrders.value])
const unpaidLoading = ref(false)
const showActiveDetail = ref(true)
const showUnpaidDetail = ref(true)
const paymentMemberBalanceMap = ref<Record<number, number>>({})

// 收款对话框
const paymentDialogVisible = ref(false)
const paymentOrder = ref<any>(null)
const paymentForm = reactive({
  amount: 0,
  remark: '',
})
const paymentMemberBalance = ref(0) // 会员预存余额

// 加载未收款订单（payment_status != 3）
const loadUnpaidOrders = async () => {
  unpaidLoading.value = true
  try {
    const res = await request.get('/orders', {
      params: { page: 1, size: 50, paymentStatus: 1 }
    })
    const records = res.data?.records || res.data || []
    let unpaidList = records.map((o: any) => {
      const total = o.totalAmount || 0
      const paid = o.paidAmount || 0
      return {
        id: o.id,
        orderNo: o.orderNo || '',
        customerName: o.customerName || '',
        memberId: o.memberId || null,
        totalAmount: total,
        paidAmount: paid,
        unpaidAmount: Math.max(0, total - paid),
        status: o.status || '',
        paymentStatus: o.paymentStatus || 1,
      }
    })
    // 同时加载部分付款的订单（payment_status = 2）
    const res2 = await request.get('/orders', {
      params: { page: 1, size: 50, paymentStatus: 2 }
    })
    const records2 = res2.data?.records || res2.data || []
    const partialPaid = records2.map((o: any) => ({
      id: o.id,
      orderNo: o.orderNo || '',
      customerName: o.customerName || '',
      memberId: o.memberId || null,
      totalAmount: o.totalAmount || 0,
      paidAmount: o.paidAmount || 0,
      unpaidAmount: Math.max(0, (o.totalAmount || 0) - (o.paidAmount || 0)),
      status: o.status || '',
      paymentStatus: 2,
    }))
    // 合并并去重
    const allOrders = [...unpaidList, ...partialPaid]
    const uniqueOrders = allOrders.filter((order, index, self) =>
      index === self.findIndex(o => o.id === order.id)
    )
    unpaidOrders.value = uniqueOrders

    // 查询每个订单关联会员的预存余额
    const balanceMap: Record<number, number> = {}
    for (const order of uniqueOrders) {
      if (order.memberId) {
        try {
          const res = await request.get(`/members/${order.memberId}`)
          const member = res.data || {}
          balanceMap[order.id] = member.balance || 0
        } catch {
          balanceMap[order.id] = 0
        }
      }
    }
    paymentMemberBalanceMap.value = balanceMap
  } catch (e: any) {
    unpaidOrders.value = []
  } finally {
    unpaidLoading.value = false
  }
}

// 加载进行中订单
const loadInProgressOrders = async () => {
  try {
    const res = await request.get('/orders', {
      params: { page: 1, size: 50, status: 2 }
    })
    const records = res.data?.records || res.data || []
    inProgressOrders.value = records.map((o: any) => ({
      id: o.id,
      orderNo: o.orderNo || '',
      customerName: o.customerName || '',
      totalAmount: o.totalAmount || 0,
      paidAmount: o.paidAmount || 0,
      designerName: o.designerName || '',
      status: o.status,
    }))
  } catch {
    inProgressOrders.value = []
  }
}

// 完成订单
const completeOrder = async (order: any) => {
  try {
    await ElMessageBox.confirm(
      `确认将订单 ${order.orderNo} 标记为已完成？\n客户 ${order.customerName} 已取走货物？`,
      '确认完成',
      { confirmButtonText: '确认完成', cancelButtonText: '取消', type: 'info' }
    )
    await request.patch(`/orders/${order.id}/status`, { status: 3 })
    ElMessage.success('订单已完成！')
    // 刷新数据
    loadInProgressOrders()
    loadUnpaidOrders()
    loadDashboard()
    loadPendingItems()
  } catch (e: any) {
    // 用户取消或接口错误
    if (e !== 'cancel' && e?.message) {
      ElMessage.error('操作失败：' + e.message)
    }
  }
}

// 显示收款对话框
const showPayment = async (order: any) => {
  paymentOrder.value = order
  paymentForm.amount = order.unpaidAmount
  paymentForm.remark = ''
  paymentMemberBalance.value = 0

  if (order.memberId) {
    try {
      const res = await request.get(`/members/${order.memberId}`)
      const member = res.data || {}
      paymentMemberBalance.value = member.balance || 0
    } catch {
      paymentMemberBalance.value = 0
    }
  }

  paymentDialogVisible.value = true
}

// 确认收款
const confirmPayment = async () => {
  if (!paymentOrder.value) return
  if (paymentForm.amount <= 0) {
    ElMessage.warning('收款金额必须大于0')
    return
  }

  try {
    await request.post(`/orders/${paymentOrder.value.id}/payment`, {
      amount: paymentForm.amount,
      remark: paymentForm.remark,
    })
    ElMessage.success('收款成功！')
    paymentDialogVisible.value = false
    loadUnpaidOrders()
    loadDashboard()
  } catch (e: any) {
    ElMessage.error('收款失败：' + (e?.message || '请稍后重试'))
  }
}

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
    // 加载所有未完成的订单（待处理 + 进行中），不只展示未收款
    const [res1, res2] = await Promise.all([
      request.get('/orders', { params: { page: 1, size: 10, status: 1 } }),
      request.get('/orders', { params: { page: 1, size: 10, status: 2 } }),
    ])

    const items: any[] = []

    // 待处理订单
    const pending = res1.data?.records || res1.data || []
    pending.forEach((o: any) => {
      items.push({
        id: o.id,
        type: 'pending',
        typeText: '📋 待处理',
        content: `客户"${o.customerName || ''}"提交订单 ${o.orderNo || ''}`,
        time: o.createTime || '',
        action: '处理',
        actionRoute: `/orders`,
      })
    })

    // 进行中订单
    const inProgress = res2.data?.records || res2.data || []
    inProgress.forEach((o: any) => {
      const paymentText = o.paymentStatus === 3 ? '' : '（未收齐款项）'
      items.push({
        id: o.id,
        type: 'in-progress',
        typeText: '⚙️ 进行中',
        content: `客户"${o.customerName || ''}"的订单 ${o.orderNo || ''} 正在设计制作中${paymentText}`,
        time: o.createTime || '',
        action: '查看',
        actionRoute: `/orders`,
      })
    })

    // 按时间倒序排列
    items.sort((a, b) => (b.time > a.time ? 1 : -1))
    pendingItems.value = items.slice(0, 15)
  } catch {
    pendingItems.value = []
  }
}

onMounted(() => {
  loadDashboard()
  loadRecentOrders()
  loadPendingItems()
  loadUnpaidOrders()
  loadInProgressOrders()
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
.status-in-progress { background: #fdf6ec; color: #e6a23c; }
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

// ========== 未完成订单提醒 ==========
.payment-reminder {
  background: linear-gradient(135deg, #f0f9ff, #e8f4fd);
  border: 1px solid #b3d8ff;
  border-radius: 12px;
  margin-bottom: 20px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(64, 158, 255, .12);
}
.reminder-header { display: flex; align-items: center; gap: 10px; padding: 14px 20px; cursor: pointer; transition: background .2s; }
.reminder-header:hover { background: rgba(64,158,255,.06); }
.reminder-icon { font-size: 22px; }
.reminder-title { font-size: 15px; font-weight: 600; color: #409eff; }
.reminder-count { padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 600; color: #fff; }
.reminder-count.in-progress-count { background: #409eff; }
.reminder-count.unpaid-count { background: #e6a23c; }
.reminder-total { margin-left: auto; font-size: 16px; font-weight: 700; color: #e6a23c; }
.toggle-btn { font-size: 12px; color: #909399; cursor: pointer; }

.reminder-cards {
  padding: 0 20px 20px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.order-card {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px;
  background: #fff; border-radius: 10px;
  border: 1px solid rgba(230,162,60,.3);
  transition: all .25s;
}
.order-card:hover { box-shadow: 0 4px 12px rgba(64,158,255,.12); transform: translateY(-2px); }
.order-card.order-card--progress { border-color: rgba(64,158,255,.3); }
.order-card.partial-paid { border-color: rgba(64,158,255,.3); }
.card-left { flex-shrink: 0; min-width: 110px; }
.card-order-no { font-size: 13px; font-weight: 600; color: $text-primary; }
.card-customer { font-size: 11px; color: $text-secondary; margin-top: 3px; }
.card-status-tag { font-size: 10px; padding: 1px 6px; border-radius: 4px; display: inline-block; margin-top: 3px; font-weight: 600; }
.card-status-tag.progress-tag { background: #ecf5ff; color: #409eff; }
.card-status-tag.unpaid-tag { background: #fef0f0; color: #e6a23c; }
.designer-tip { margin-top: 4px; font-size: 11px; color: #909399; }
.card-center { flex: 1; min-width: 0; }
.card-amount-info { display: flex; align-items: center; gap: 8px; font-size: 12px; flex-wrap: wrap; }
.card-divider { color: #dcdfe6; }
.card-unpaid { color: #e6a23c; font-weight: 600; }
.member-balance-tip { margin-top: 4px; font-size: 11px; color: #409eff; background: #ecf5ff; padding: 2px 6px; border-radius: 4px; display: inline-block; }
.card-right { flex-shrink: 0; }
.collect-btn { background: linear-gradient(135deg,#e6a23c,#ebb563); color: #fff; border: none; padding: 7px 16px; border-radius: 6px; cursor: pointer; font-size: 12px; font-weight: 500; transition: all .25s; flex-shrink: 0; }
.collect-btn:hover { background: linear-gradient(135deg,#cf8f1a,#d9a03e); box-shadow: 0 4px 12px rgba(230,162,60,.4); }
.order-card--progress .collect-btn { background: linear-gradient(135deg, #67c23a, #85ce61); }
.order-card--progress .collect-btn:hover { background: linear-gradient(135deg, #5daf34, #78c550); box-shadow: 0 4px 12px rgba(103,194,58,.4); }

// 收款对话框
.payment-modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.payment-modal { background: #fff; border-radius: 12px; padding: 24px; width: 480px; max-width: 90vw; box-shadow: 0 20px 60px rgba(0,0,0,.3); }
.modal-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; padding-bottom: 14px; border-bottom: 1px solid #eef0f2; }
.modal-header h3 { font-size: 16px; font-weight: 600; color: $text-primary; }
.close-btn { background: none; border: none; font-size: 22px; cursor: pointer; color: #909399; &:hover { color: $text-primary; } }
.modal-body { margin-bottom: 20px; }
.modal-customer { font-size: 14px; color: $text-secondary; margin-bottom: 16px; }
.modal-row { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; font-size: 14px; label { width: 100px; color: $text-secondary; flex-shrink: 0; } span { font-weight: 500; color: $text-primary; } &.highlight label { color: $warning; font-weight: 600; } }
.modal-input { flex: 1; padding: 8px 12px; border: 1px solid $border-light; border-radius: 6px; font-size: 14px; &:focus { outline: none; border-color: $primary; box-shadow: 0 0 0 2px rgba(64,158,255,.2); } }
.member-balance-card { background: linear-gradient(135deg,#ecf5ff,#d9ecff); border: 1px solid #b3d8ff; border-radius: 8px; padding: 14px; margin: 12px 0; text-align: center; }
.balance-header { font-size: 13px; color: #409eff; margin-bottom: 6px; }
.balance-amount { font-size: 24px; font-weight: 700; color: #409eff; }
.balance-hint { font-size: 12px; color: #909399; margin-top: 6px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding-top: 14px; border-top: 1px solid #eef0f2; }
.cancel-btn { padding: 8px 20px; border-radius: 6px; border: 1px solid $border-light; background: #fff; cursor: pointer; font-size: 14px; color: $text-primary; &:hover { background: $bg-base; } }
.confirm-btn { padding: 8px 20px; border-radius: 6px; border: none; background: $success; color: #fff; cursor: pointer; font-size: 14px; &:hover { background: #85ce61; } }
.loading-tip { text-align: center; padding: 20px; color: #909399; font-size: 14px; }

</style>
