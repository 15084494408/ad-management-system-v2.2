<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">💰 营收统计</span>
      <div class="page-actions">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="loadData" />
        <el-button @click="exportData">
          <el-icon><Download /></el-icon> 导出报表
        </el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">💵</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalRevenue?.toLocaleString() }}</div>
          <div class="stat-label">总营收</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">📈</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ monthRevenue?.toLocaleString() }}</div>
          <div class="stat-label">本月营收</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">📊</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ avgOrderValue?.toFixed(2) }}</div>
          <div class="stat-label">客单价</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">📋</div>
        <div class="stat-info">
          <div class="stat-value">{{ orderCount }}</div>
          <div class="stat-label">订单数量</div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <div class="chart-card">
        <div class="card-header-row">
          <span class="card-title">📈 营收趋势</span>
          <el-radio-group v-model="trendType" size="small" @change="loadTrendData">
            <el-radio-button label="day">按天</el-radio-button>
            <el-radio-button label="week">按周</el-radio-button>
            <el-radio-button label="month">按月</el-radio-button>
          </el-radio-group>
        </div>
        <div class="chart-area" ref="trendChartRef"></div>
      </div>
      <div class="chart-card">
        <div class="card-header-row">
          <span class="card-title">📊 营收构成</span>
        </div>
        <div class="chart-area" ref="pieChartRef"></div>
      </div>
    </div>

    <div class="card">
      <div class="card-header-row">
        <span class="card-title">📋 营收明细</span>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="orderCount" label="订单数" width="100" />
        <el-table-column prop="revenue" label="营收金额" width="150">
          <template #default="{ row }">
            <span class="revenue-value">¥{{ row.revenue?.toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cost" label="成本" width="120">
          <template #default="{ row }">¥{{ row.cost?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="profit" label="利润" width="120">
          <template #default="{ row }">
            <span :class="row.profit >= 0 ? 'profit-value' : 'loss-value'">
              ¥{{ row.profit?.toLocaleString() }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="profitRate" label="利润率" width="100">
          <template #default="{ row }">
            <span :class="row.profitRate >= 0 ? 'profit-value' : 'loss-value'">
              {{ row.profitRate?.toFixed(1) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="customerCount" label="客户数" width="100" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import request from '@/api/request'
import { exportToExcel } from '@/utils/excelExport'

const loading = ref(false)
const trendType = ref('day')
const dateRange = ref<string[]>([
  new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
  new Date().toISOString().split('T')[0]
])
const tableData = ref<any[]>([])
const trendData = ref<any[]>([])
const categoryData = ref<any[]>([])
const trendChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()

const totalRevenue = computed(() => tableData.value.reduce((sum, t) => sum + (t.revenue || 0), 0))
const monthRevenue = computed(() => {
  const currentMonth = new Date().toISOString().slice(0, 7)
  return tableData.value.filter(t => t.date.startsWith(currentMonth)).reduce((sum, t) => sum + (t.revenue || 0), 0)
})
const orderCount = computed(() => tableData.value.reduce((sum, t) => sum + (t.orderCount || 0), 0))
const avgOrderValue = computed(() => {
  const total = totalRevenue.value
  const count = orderCount.value
  return count > 0 ? total / count : 0
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/statistics/revenue/summary', {
      params: { startDate: dateRange.value?.[0], endDate: dateRange.value?.[1] }
    })
    tableData.value = res.data || []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
  loadTrendData()
  loadCategoryData()
}

const loadCategoryData = () => {
  categoryData.value = []
}

const loadTrendData = () => {
  let labels: string[] = []
  let values: number[] = []

  if (trendType.value === 'day') {
    const last7Days = tableData.value.slice(-7)
    labels = last7Days.map(t => t.date.slice(5))
    values = last7Days.map(t => t.revenue)
  } else if (trendType.value === 'week') {
    const weeks = groupByWeek(tableData.value)
    labels = Object.keys(weeks).map(k => `第${k}周`)
    values = Object.values(weeks).map((week: any[]) => week.reduce((sum: number, t: any) => sum + t.revenue, 0))
  } else {
    const months = groupByMonth(tableData.value)
    labels = Object.keys(months)
    values = Object.values(months).map((month: any[]) => month.reduce((sum: number, t: any) => sum + t.revenue, 0))
  }

  trendData.value = { labels, values }
}

const groupByWeek = (data: any[]) => {
  const result: Record<string, any[]> = {}
  data.forEach(item => {
    const date = new Date(item.date)
    const week = getWeekNumber(date)
    const key = `${date.getFullYear()}-W${week}`
    if (!result[key]) result[key] = []
    result[key].push(item)
  })
  return result
}

const groupByMonth = (data: any[]) => {
  const result: Record<string, any[]> = {}
  data.forEach(item => {
    const month = item.date.slice(0, 7)
    if (!result[month]) result[month] = []
    result[month].push(item)
  })
  return result
}

const getWeekNumber = (date: Date) => {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
  const dayNum = d.getUTCDay() || 7
  d.setUTCDate(d.getUTCDate() + 4 - dayNum)
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1))
  return Math.ceil((((d.getTime() - yearStart.getTime()) / 86400000) + 1) / 7)
}

const exportData = () => {
  if (tableData.value.length === 0) { ElMessage.warning('暂无数据可导出'); return }
  exportToExcel({
    filename: '营收统计报表',
    header: ['日期', '订单数', '营收金额', '成本', '利润', '利润率', '客户数'],
    data: tableData.value.map(row => [
      row.date, row.orderCount || 0,
      `¥${(row.revenue || 0).toLocaleString()}`,
      `¥${(row.cost || 0).toLocaleString()}`,
      `¥${(row.profit || 0).toLocaleString()}`,
      `${(row.profitRate || 0).toFixed(1)}%`,
      row.customerCount || 0,
    ]),
    summaryRow: ['合计', orderCount.value, `¥${totalRevenue.value.toLocaleString()}`, '', `¥${tableData.value.reduce((s, r) => s + (r.profit || 0), 0).toLocaleString()}`, '', ''],
    infoRows: [[`导出时间：${new Date().toLocaleString()}`], [`统计范围：${dateRange.value?.[0] || '-'} ~ ${dateRange.value?.[1] || '-'}`]],
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.chart-area {
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafbfc;
  border-radius: 8px;
}

.revenue-value {
  color: #409eff;
  font-weight: 600;
}

.profit-value {
  color: #67c23a;
  font-weight: 600;
}

.loss-value {
  color: #f56c6c;
  font-weight: 600;
}
</style>
