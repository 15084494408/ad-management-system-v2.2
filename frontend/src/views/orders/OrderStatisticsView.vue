<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item">订单管理</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">订单统计</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">📊 订单统计</h1>
      <div class="page-actions">
        <button class="btn btn-default" @click="exportVisible = true">⬇️ 导出报表</button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-form">
      <div class="form-group">
        <label>统计周期</label>
        <select v-model="period" class="form-control" @change="loadData">
          <option value="today">今日</option>
          <option value="week">本周</option>
          <option value="month">本月</option>
          <option value="year">本年</option>
        </select>
      </div>
      <div class="form-group" style="align-self:flex-end;">
        <button class="btn btn-primary" @click="loadData">🔍 查询</button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon blue">📋</div>
        <div class="stat-info">
          <h3>{{ stats.totalCount || 0 }}</h3>
          <p>订单总数</p>
          <div class="stat-trend up">↑ {{ completionRate }}% 完成率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">✅</div>
        <div class="stat-info">
          <h3>{{ stats.completedCount || 0 }}</h3>
          <p>已完成订单</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">⏳</div>
        <div class="stat-info">
          <h3>{{ stats.processingCount || 0 }}</h3>
          <p>进行中订单</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">❌</div>
        <div class="stat-info">
          <h3>{{ stats.cancelledCount || 0 }}</h3>
          <p>已取消订单</p>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="charts-grid">
      <!-- 趋势图 -->
      <div class="card">
        <div class="card-title">📈 订单趋势（近7天）</div>
        <div class="bar-chart">
          <div class="bar-item" v-for="item in trend" :key="item.date">
            <div class="bar-value">{{ item.count }}</div>
            <div class="bar-fill" :style="{ height: barMaxHeight(item.count) + '%' }"></div>
            <div class="bar-label">{{ formatBarDate(item.date) }}</div>
          </div>
        </div>
      </div>

      <!-- 状态分布 -->
      <div class="card">
        <div class="card-title">📊 订单状态分布</div>
        <div class="pie-chart-wrap">
          <div class="pie-chart" :style="pieGradient">
            <div class="pie-center">
              <div class="pie-total">{{ stats.totalCount || 0 }}</div>
              <div class="pie-label">总订单</div>
            </div>
          </div>
        </div>
        <div class="pie-legend">
          <span><span style="color:#409eff;">●</span> 待确认 {{ statusPercent('pending') }}%</span>
          <span><span style="color:#e6a23c;">●</span> 进行中 {{ statusPercent('designing') }}%</span>
          <span><span style="color:#67c23a;">●</span> 已完成 {{ statusPercent('completed') }}%</span>
          <span><span style="color:#f56c6c;">●</span> 已取消 {{ statusPercent('cancelled') }}%</span>
        </div>
      </div>
    </div>

    <!-- 金额汇总 -->
    <div class="card" style="margin-top:16px;">
      <div class="amount-summary">
        <div class="amount-item">
          <span class="amount-label">有效订单总额</span>
          <span class="amount-value primary">¥{{ formatMoney(stats.totalAmount) }}</span>
          <span class="amount-hint">（不含已取消）</span>
        </div>
        <div class="amount-item">
          <span class="amount-label">已收金额</span>
          <span class="amount-value success">¥{{ formatMoney(stats.paidAmount) }}</span>
        </div>
        <div class="amount-item">
          <span class="amount-label">待收金额</span>
          <span class="amount-value danger">¥{{ formatMoney(Math.max((stats.unpaidAmount ?? (stats.totalAmount || 0) - (stats.paidAmount || 0)), 0)) }}</span>
        </div>
        <div class="amount-item" v-if="stats.cancelledCount > 0">
          <span class="amount-label">已取消订单</span>
          <span class="amount-value cancelled">{{ stats.cancelledCount }} 笔</span>
          <span class="amount-hint">金额已排除</span>
        </div>
      </div>
    </div>

    <!-- 导出弹窗 -->
    <div class="modal-overlay" v-if="exportVisible" @click.self="exportVisible = false">
      <div class="modal-container" style="max-width:500px;">
        <div class="modal-header"><h3>⬇️ 导出统计报表</h3><button class="modal-close" @click="exportVisible = false">&times;</button></div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">报表类型</label>
              <select class="form-input">
                <option>订单统计报表</option><option>营收统计报表</option>
                <option>客户统计报表</option><option>综合报表</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">统计周期</label>
              <select class="form-input">
                <option>本月</option><option>本季度</option><option>本年</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">导出格式</label>
            <select class="form-input">
              <option>Excel (.xlsx)</option><option>PDF (.pdf)</option>
            </select>
          </div>
          <div style="background:#ecf5ff;padding:15px;border-radius:8px;margin-top:15px;">
            <h4 style="font-size:13px;margin-bottom:10px;">📊 报表预览</h4>
            <div style="font-size:12px;color:#909399;">
              本月订单数：{{ stats.totalCount || 0 }} 单（已取消 {{ stats.cancelledCount || 0 }} 笔）<br>
              有效营收：¥{{ formatMoney(stats.totalAmount) }}（不含已取消）<br>
              已完成订单：{{ stats.completedCount || 0 }} 单
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="exportVisible = false">取消</button>
          <button class="btn btn-primary" @click="doExport">⬇️ 导出报表</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderApi } from '@/api'
import { exportToExcel } from '@/utils/excelExport'

const router = useRouter()
const period = ref('month')
const loading = ref(false)
const exportVisible = ref(false)

const stats = reactive<any>({
  totalCount: 0, completedCount: 0, processingCount: 0,
  pendingCount: 0, cancelledCount: 0, totalAmount: 0, paidAmount: 0,
  completionRate: 0, statusDistribution: {}, trend: [],
})

const completionRate = computed(() => stats.completionRate || 0)

const trend = computed(() => stats.trend || [])

const pieGradient = computed(() => {
  const d = stats.statusDistribution || {}
  const total = stats.totalCount || 1
  const p = d.pending || 0
  const pr = d.designing || 0
  const c = d.completed || 0
  const ca = d.cancelled || 0
  const pEnd = (p / total * 100).toFixed(1)
  const prEnd = ((p + pr) / total * 100).toFixed(1)
  const cEnd = ((p + pr + c) / total * 100).toFixed(1)
  return { background: `conic-gradient(#409eff 0% ${pEnd}%, #e6a23c ${pEnd}% ${prEnd}%, #67c23a ${prEnd}% ${cEnd}%, #f56c6c ${cEnd}% 100%)` }
})

function statusPercent(key: string) {
  const d = stats.statusDistribution || {}
  const total = stats.totalCount || 1
  return Math.round((d[key] || 0) / total * 100)
}

function barMaxHeight(count: number) {
  const max = Math.max(...trend.value.map((t: any) => t.count), 1)
  return Math.max(5, (count / max) * 100)
}

function formatBarDate(d: string) {
  return d?.slice(5) || d // MM-DD
}

function formatMoney(v: any) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadData() {
  loading.value = true
  try {
    const res = await orderApi.getStatistics({ period: period.value })
    Object.assign(stats, res.data || {})
  } catch {
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

function doExport() {
  const periodLabel: Record<string, string> = { today: '今日', week: '本周', month: '本月', year: '本年' }

  // 统计概览数据
  const summaryHeader = ['统计项', '数值']
  const summaryData = [
    ['订单总数', String(stats.totalCount || 0)],
    ['已完成订单', String(stats.completedCount || 0)],
    ['进行中订单', String(stats.processingCount || 0)],
    ['待确认订单', String(stats.pendingCount || 0)],
    ['已取消订单', String(stats.cancelledCount || 0)],
    ['完成率', `${stats.completionRate || 0}%`],
    ['订单总额（不含已取消）', `¥${formatMoney(stats.totalAmount)}`],
    ['已收金额', `¥${formatMoney(stats.paidAmount)}`],
    ['待收金额', `¥${formatMoney(Math.max((stats.unpaidAmount ?? (stats.totalAmount || 0) - (stats.paidAmount || 0)), 0))}`],
    ['已取消订单', `${stats.cancelledCount || 0} 笔（金额已排除）`],
  ]

  // 趋势数据（近7天）
  let trendRows: any[] = []
  if (trend.value.length > 0) {
    trendRows = [
      ['', ''],
      ...trend.value.map((t: any) => [`日期: ${formatBarDate(t.date)}`, `订单数: ${t.count}`]),
    ]
  }

  // 状态分布
  const dist = stats.statusDistribution || {}
  const statusDistRows: any[] = [
    ['状态分布', '数量 / 占比'],
    [`待确认`, `${dist.pending || 0} 单 / ${statusPercent('pending')}%`],
    [`进行中`, `${dist.designing || 0} 单 / ${statusPercent('designing')}%`],
    [`已完成`, `${dist.completed || 0} 单 / ${statusPercent('completed')}%`],
    [`已取消`, `${dist.cancelled || 0} 单 / ${statusPercent('cancelled')}%`],
  ]

  exportToExcel({
    filename: '订单统计报表',
    title: `订单统计报表（${periodLabel[period.value] || period.value}）`,
    header: summaryHeader,
    data: summaryData,
    infoRows: [
      [`导出时间：${new Date().toLocaleString()}`],
      [`统计周期：${periodLabel[period.value] || period.value}`],
      [`数据来源：系统自动统计`],
    ],
  })

  // 同时导出趋势和状态分布到同一文件的多 sheet 暂不支持，这里用追加行的方式整合
  // 将趋势数据和状态分布作为附加信息追加

  exportVisible.value = false
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.page-container { padding: 20px 24px; }

.stats-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
}
.stat-card {
  background: #fff; border-radius: 12px; padding: 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.stat-icon {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px;
  &.blue { background: #ecf5ff; }
  &.green { background: #f0f9eb; }
  &.orange { background: #fdf6ec; }
  &.red { background: #fef0f0; }
}
.stat-info h3 { font-size: 24px; margin: 0 0 4px; }
.stat-info p { font-size: 13px; color: #909399; margin: 0; }
.stat-trend {
  font-size: 12px; margin-top: 4px;
  &.up { color: #67c23a; }
  &.down { color: #f56c6c; }
}

.charts-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px;
}

.bar-chart {
  display: flex; align-items: flex-end; gap: 8px;
  height: 200px; padding: 16px 12px;
}
.bar-item {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  height: 100%; justify-content: flex-end;
}
.bar-value { font-size: 11px; color: #606266; margin-bottom: 4px; font-weight: 600; }
.bar-fill {
  width: 100%; max-width: 36px; background: linear-gradient(180deg, #409eff, #79bbff);
  border-radius: 4px 4px 0 0; min-height: 4px; transition: height 0.5s ease;
}
.bar-label { font-size: 10px; color: #909399; margin-top: 6px; }

.pie-chart-wrap {
  display: flex; align-items: center; justify-content: center; height: 200px;
}
.pie-chart {
  width: 180px; height: 180px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.5s ease;
}
.pie-center {
  width: 110px; height: 110px; background: #fff; border-radius: 50%;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.pie-total { font-size: 22px; font-weight: 700; }
.pie-label { font-size: 11px; color: #909399; }
.pie-legend {
  display: flex; justify-content: center; gap: 16px; margin-top: 16px; font-size: 12px; flex-wrap: wrap;
}

.card-title {
  font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #303133;
}

.amount-summary {
  display: flex; gap: 32px; justify-content: center;
}
.amount-item {
  text-align: center;
}
.amount-label { font-size: 13px; color: #909399; display: block; margin-bottom: 6px; }
.amount-value {
  font-size: 20px; font-weight: 700;
  &.primary { color: #409eff; }
  &.success { color: #67c23a; }
  &.danger { color: #f56c6c; }
  &.cancelled { color: #909399; font-size: 16px; }
}
.amount-hint { font-size: 11px; color: #c0c4cc; display: block; margin-top: 2px; }

// Modal
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modal-container {
  background: #fff; border-radius: 12px; width: 90%;
  box-shadow: 0 8px 30px rgba(0,0,0,0.15);
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 24px; border-bottom: 1px solid #f0f0f0;
  h3 { font-size: 16px; margin: 0; }
}
.modal-close {
  width: 30px; height: 30px; border: none; background: none;
  font-size: 20px; cursor: pointer; color: #909399; border-radius: 6px;
  &:hover { background: #f5f7fa; color: #303133; }
}
.modal-body { padding: 20px 24px; }
.modal-footer {
  padding: 14px 24px; border-top: 1px solid #f0f0f0;
  display: flex; justify-content: flex-end; gap: 10px;
}
</style>
