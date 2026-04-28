<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <span class="page-title">💵 流水统计</span>
      <div class="page-actions">
        <el-button @click="exportData"><el-icon><Download /></el-icon> 导出</el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="统计周期">
          <el-radio-group v-model="searchForm.period" size="default">
            <el-radio-button value="daily">按日</el-radio-button>
            <el-radio-button value="weekly">按周</el-radio-button>
            <el-radio-button value="monthly">按月</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            size="default"
          />
        </el-form-item>
        <el-form-item label="流水类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable size="default" style="width:120px;">
            <el-option label="全部" value="" />
            <el-option label="收入" value="income" />
            <el-option label="支出" value="expense" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="searchForm.paymentMethod" placeholder="全部" clearable size="default" style="width:120px;">
            <el-option label="全部" value="" />
            <el-option label="现金" value="现金" />
            <el-option label="转账" value="转账" />
            <el-option label="微信" value="微信" />
            <el-option label="支付宝" value="支付宝" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 统计卡片（6个：上方3个汇总 + 下方3个今日） -->
    <div class="stat-grid">
      <!-- 汇总统计 -->
      <div class="stat-card">
        <div class="stat-icon blue">💵</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalFlow?.toLocaleString() }}</div>
          <div class="stat-label">流水总额</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">⬆️</div>
        <div class="stat-info">
          <div class="stat-value income-text">¥{{ incomeTotal?.toLocaleString() }}</div>
          <div class="stat-label">收入总计</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">⬇️</div>
        <div class="stat-info">
          <div class="stat-value expense-text">¥{{ expenseTotal?.toLocaleString() }}</div>
          <div class="stat-label">支出总计</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">📊</div>
        <div class="stat-info">
          <div class="stat-value" :class="netAmount >= 0 ? 'income-text' : 'expense-text'">
            {{ netAmount >= 0 ? '+' : '' }}¥{{ netAmount?.toLocaleString() }}
          </div>
          <div class="stat-label">净流水</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">📅</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ todayTotal?.toLocaleString() }}</div>
          <div class="stat-label">今日流水</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon teal">📋</div>
        <div class="stat-info">
          <div class="stat-value">{{ recordCount }}</div>
          <div class="stat-label">流水笔数</div>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="charts-grid">
      <div class="chart-card card">
        <div class="card-title">📈 收支趋势</div>
        <div class="chart-container">
          <div class="bar-chart" v-if="trendData.length > 0">
            <div v-for="(v, i) in trendData" :key="i" class="bar-wrap">
              <div class="bar" :style="{ height: (Math.max(v.income, 1) / maxTrend * 100) + '%', background: '#67c23a' }"></div>
              <div class="bar" :style="{ height: (Math.max(v.expense, 1) / maxTrend * 100) + '%', background: '#f56c6c', marginTop: '2px' }"></div>
              <span class="bar-label">{{ v.label }}</span>
            </div>
          </div>
          <div v-else class="empty-chart">暂无趋势数据</div>
        </div>
      </div>
      <div class="chart-card card">
        <div class="card-title">📊 收入渠道占比</div>
        <div class="pie-container">
          <div class="pie-chart" :style="{ background: pieGradient }">
            <div class="pie-center">¥{{ incomeTotal?.toLocaleString() }}</div>
          </div>
          <div class="pie-legend">
            <div v-for="c in channelData" :key="c.label" class="legend-item">
              <span class="dot" :style="{background: c.color}"></span>
              <span class="legend-label">{{ c.label }}</span>
              <span class="legend-pct">{{ c.pct }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 流水记录表格 -->
    <div class="card" style="margin-top: 20px;">
      <div class="card-title" style="padding: 16px 20px; border-bottom: 1px solid #f0f0f0;">
        💰 流水记录
        <span class="record-count">共 {{ tableTotal }} 笔</span>
      </div>
      <div style="padding: 0 20px; overflow-x: auto;">
        <table class="data-table">
          <thead>
            <tr>
              <th>流水编号</th>
              <th>金额</th>
              <th>类型</th>
              <th>支付方式</th>
              <th>关联</th>
              <th>备注</th>
              <th>记账人</th>
              <th>发生时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="tableData.length === 0">
              <td colspan="8" style="text-align: center; color: #909399; padding: 40px;">暂无数据</td>
            </tr>
            <tr v-for="record in tableData" :key="record.id">
              <td>{{ record.recordNo || ('LF' + record.id) }}</td>
              <td :class="record.type === 'income' ? 'income-text' : 'expense-text'" style="font-weight: 600;">
                {{ record.type === 'income' ? '+' : '-' }}¥{{ record.amount?.toLocaleString() }}
              </td>
              <td>
                <span class="tag" :class="record.type === 'income' ? 'tag-income' : 'tag-expense'">
                  {{ record.type === 'income' ? '收入' : '支出' }}
                </span>
                <span class="category-tag">{{ record.category || '-' }}</span>
              </td>
              <td>
                <span class="tag" :class="getPaymentTag(record.paymentMethod)">
                  {{ record.paymentMethod || '-' }}
                </span>
              </td>
              <td>{{ record.relatedName || '-' }}</td>
              <td>{{ record.remark || '-' }}</td>
              <td>{{ record.creatorId || '系统' }}</td>
              <td>{{ record.createTime }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <!-- 分页 -->
      <div class="pagination" style="padding: 16px 20px;">
        <span class="pagination-info">共 {{ tableTotal }} 笔</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="tableTotal"
          layout="sizes, prev, pager, next"
          @size-change="loadTable"
          @current-change="loadTable"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { useFinanceStore } from '@/stores/finance'
import request from '@/api/request'
import { exportToExcel } from '@/utils/excelExport'

const financeStore = useFinanceStore()
// 全局财务数据变动时，自动刷新流水统计
watch(() => financeStore.lastFinanceUpdate, () => {
  loadAll()
})

// ── 搜索/筛选 ──
const searchForm = ref({
  period: 'daily',
  dateRange: [] as string[],
  type: '',
  paymentMethod: ''
})

// ── 汇总统计（来自 /statistics/flow/summary） ──
const totalFlow = ref(0)
const incomeTotal = ref(0)
const expenseTotal = ref(0)
const netAmount = ref(0)
const recordCount = ref(0)
const todayTotal = ref(0)
const avgDaily = ref(0)
const trendData = ref<any[]>([])
const channelData = ref<any[]>([])

// ── 流水表格（来自 /finance/records） ──
const tableData = ref<any[]>([])
const tableTotal = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// ── 计算属性 ──
const maxTrend = computed(() => Math.max(...trendData.value.map(v => Math.max(v.income, v.expense)), 1))

const pieGradient = computed(() => {
  let acc = 0
  return channelData.value.map(c => {
    const start = acc
    acc += c.pct
    return `${c.color} ${start}% ${acc}%`
  }).join(', ')
})

function getPaymentTag(method: string) {
  const map: Record<string, string> = {
    '现金': 'tag-cash', '转账': 'tag-transfer', '微信': 'tag-wechat', '支付宝': 'tag-alipay'
  }
  return map[method] || 'tag-default'
}

// ── 加载统计汇总 ──
async function loadSummary() {
  try {
    const [start, end] = searchForm.value.dateRange || []
    const res = await request.get('/statistics/flow/summary', {
      params: { period: searchForm.value.period, startDate: start, endDate: end }
    })
    const d = res.data || {}
    totalFlow.value = d.totalFlow || 0
    incomeTotal.value = d.incomeTotal || 0
    expenseTotal.value = d.expenseTotal || 0
    netAmount.value = d.netAmount || 0
    recordCount.value = d.recordCount || 0
    todayTotal.value = d.todayTotal || 0
    avgDaily.value = d.avgDaily || 0

    if (d.channelData && d.channelData.length > 0 && d.channelData[0].label !== '暂无数据') {
      channelData.value = d.channelData.map((c: any) => ({
        label: c.label,
        pct: Number(c.pct),
        color: c.color
      }))
    } else {
      channelData.value = []
    }
  } catch (e) {
    console.error('加载流水统计失败', e)
  }
}

// ── 加载趋势图 ──
async function loadTrend() {
  try {
    const [start, end] = searchForm.value.dateRange || []
    const res = await request.get('/statistics/flow/trend', {
      params: { period: searchForm.value.period, startDate: start, endDate: end }
    })
    trendData.value = (res.data || []).map((v: any) => ({
      label: v.label,
      income: Number(v.income) || 0,
      expense: Number(v.expense) || 0
    }))
  } catch {
    trendData.value = []
  }
}

// ── 加载流水表格 ──
async function loadTable() {
  try {
    const [start, end] = searchForm.value.dateRange || []
    const res = await request.get('/finance/records', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        type: searchForm.value.type,
        paymentMethod: searchForm.value.paymentMethod,
        startDate: start,
        endDate: end
      }
    })
    if (res.code === 200 || res.code === 0) {
      tableData.value = res.data?.records || []
      tableTotal.value = res.data?.total || 0
    }
  } catch (e) {
    console.error('加载流水记录失败', e)
  }
}

// ── 搜索 ──
function onSearch() {
  currentPage.value = 1
  loadAll()
}

// ── 导出 ──
function exportData() {
  if (tableData.value.length === 0) { ElMessage.warning('暂无数据可导出'); return }
  exportToExcel({
    filename: '流水统计报表',
    header: ['流水编号', '金额', '类型', '支付方式', '关联', '备注', '记账人', '发生时间'],
    data: tableData.value.map(r => [
      r.recordNo || `LF${r.id}`,
      `${r.type === 'income' ? '+' : '-'}¥${(r.amount || 0).toLocaleString()}`,
      r.type === 'income' ? '收入' : '支出',
      r.paymentMethod || '-',
      r.relatedName || '-',
      r.remark || '-',
      r.creatorId || '系统',
      (r.createTime || '').toString(),
    ]),
    infoRows: [[`导出时间：${new Date().toLocaleString()}`], [`共 ${tableData.value.length} 条记录`]],
  })
}

// ── 初始化 ──
async function loadAll() {
  await Promise.all([loadSummary(), loadTrend(), loadTable()])
}

onMounted(loadAll)
</script>

<style scoped lang="scss">
.page-container { padding: 20px 24px; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f0f0;
}

.stat-icon {
  font-size: 32px;
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  &.blue { background: #ecf5ff; }
  &.green { background: #f0f9eb; }
  &.red { background: #fef0f0; }
  &.orange { background: #fdf6ec; }
  &.purple { background: #f4f0ff; }
  &.teal { background: #ecfbf1; }
}

.stat-info { flex: 1; min-width: 0; }

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.income-text { color: #67c23a !important; }
.expense-text { color: #f56c6c !important; }

.charts-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.chart-container { height: 200px; }

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 100%;
  padding-bottom: 24px;
}

.bar-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  height: 100%;
  justify-content: flex-end;
}

.bar {
  width: 100%;
  max-width: 40px;
  min-height: 4px;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s;
}

.bar-label {
  font-size: 10px;
  color: #909399;
  text-align: center;
  position: absolute;
  bottom: 0;
}

.bar-wrap { position: relative; }

.empty-chart {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 13px;
}

.pie-container {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 200px;
}

.pie-chart {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pie-center {
  width: 100px;
  height: 100px;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.pie-legend {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.legend-label { color: #606266; flex: 1; }
.legend-pct { color: #909399; font-weight: 600; }

/* 表格 */
.card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.record-count {
  font-size: 12px;
  color: #909399;
  font-weight: 400;
  margin-left: 8px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  th, td {
    padding: 12px 8px;
    text-align: left;
    border-bottom: 1px solid #f0f0f0;
    white-space: nowrap;
  }
  th {
    background: #fafafa;
    color: #606266;
    font-weight: 600;
    font-size: 12px;
  }
  tr:hover td { background: #f5f7fa; }
  tr:last-child td { border-bottom: none; }
}

.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  margin-right: 4px;
}
.tag-income { background: #f0f9eb; color: #67c23a; }
.tag-expense { background: #fef0f0; color: #f56c6c; }
.tag-cash { background: #fdf6ec; color: #e6a23c; }
.tag-transfer { background: #ecf5ff; color: #409eff; }
.tag-wechat { background: #e8f8f0; color: #52c41a; }
.tag-alipay { background: #e8f0ff; color: #1677ff; }
.tag-default { background: #f4f4f5; color: #909399; }

.category-tag {
  font-size: 11px;
  color: #909399;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.pagination-info { font-size: 13px; color: #606266; }
</style>
