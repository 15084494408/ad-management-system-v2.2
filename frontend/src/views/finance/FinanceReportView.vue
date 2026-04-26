<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">📊 财务报表</span>
      <div class="page-actions">
        <el-button @click="exportReport"><el-icon><Download /></el-icon> 导出报表</el-button>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="报表类型">
          <el-select v-model="searchForm.reportType" style="width:160px;">
            <el-option label="月度报表" value="monthly" />
            <el-option label="季度报表" value="quarterly" />
            <el-option label="年度报表" value="yearly" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">💰</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalIncome?.toLocaleString() }}</div>
          <div class="stat-label">总收入</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">📉</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalExpense?.toLocaleString() }}</div>
          <div class="stat-label">总支出</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">📈</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalProfit?.toLocaleString() }}</div>
          <div class="stat-label">净利润</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">📊</div>
        <div class="stat-info">
          <div class="stat-value">{{ profitRate }}%</div>
          <div class="stat-label">利润率</div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <div class="chart-card card">
        <div class="card-title">📈 收支趋势</div>
        <div class="chart-container">
          <div class="bar-chart">
            <div v-for="(v, i) in trendData" :key="i" class="bar-wrap">
              <div class="bar" :style="{ height: (v.income / maxTrend * 100) + '%', background: '#409eff' }"></div>
              <div class="bar" :style="{ height: (v.expense / maxTrend * 100) + '%', background: '#f56c6c', marginTop: '2px' }"></div>
              <span class="bar-label">{{ v.month }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="chart-card card">
        <div class="card-title">📊 支出分类</div>
        <div class="pie-container">
          <div class="pie-chart" :style="{ background: pieGradient }">
            <div class="pie-center">{{ totalExpense?.toLocaleString() }}</div>
          </div>
          <div class="pie-legend">
            <div v-for="s in expenseCategories" :key="s.label" class="legend-item">
              <span class="dot" :style="{background: s.color}"></span>{{ s.label }}: ¥{{ s.amount?.toLocaleString() }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header-row"><span class="card-title">📋 月度明细</span></div>
      <el-table :data="monthlyList" stripe size="small">
        <el-table-column prop="month" label="月份" width="100" />
        <el-table-column prop="income" label="收入" width="120">
          <template #default="{ row }"><span style="color:#67c23a;">¥{{ row.income?.toLocaleString() }}</span></template>
        </el-table-column>
        <el-table-column prop="expense" label="支出" width="120">
          <template #default="{ row }"><span style="color:#f56c6c;">¥{{ row.expense?.toLocaleString() }}</span></template>
        </el-table-column>
        <el-table-column prop="profit" label="净利润" width="120">
          <template #default="{ row }"><span :style="{color: row.profit >= 0 ? '#409eff' : '#f56c6c', fontWeight: 600}">¥{{ row.profit?.toLocaleString() }}</span></template>
        </el-table-column>
        <el-table-column prop="profitRate" label="利润率" width="100">
          <template #default="{ row }">{{ row.profitRate }}%</template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="80" />
        <el-table-column prop="newCustomers" label="新客户" width="80" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import request from '@/api/request'

const searchForm = ref({ reportType: 'monthly', dateRange: [] })
const totalIncome = ref(0)
const totalExpense = ref(0)
const totalProfit = ref(0)
const profitRate = ref(0)
const trendData = ref<any[]>([])
const expenseCategories = ref<any[]>([])
const monthlyList = ref<any[]>([])

const pieGradient = computed(() => {
  const cats = expenseCategories.value
  const total = cats.reduce((s, c) => s + c.amount, 0)
  let accumulated = 0
  return cats.map(c => {
    const pct = (c.amount / total) * 100
    const start = accumulated
    accumulated += pct
    return `${c.color} ${start}% ${accumulated}%`
  }).join(', ')
})

const maxTrend = computed(() => Math.max(...trendData.value.map(v => Math.max(v.income, v.expense)), 1))

async function loadData() {
  try {
    const res = await request.get('/finance/report/summary', { params: searchForm.value })
    const d = res.data || {}
    totalIncome.value = d.totalIncome || 0
    totalExpense.value = d.totalExpense || 0
    totalProfit.value = d.totalProfit || 0
    profitRate.value = d.profitRate || 0
  } catch {}
  trendData.value = []
  monthlyList.value = []
}

function exportReport() { window.open('/api/finance/report/export', '_blank') }
onMounted(loadData)
</script>

<style scoped lang="scss">
.charts-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-bottom: 20px; }
.pie-container { display: flex; align-items: center; gap: 20px; }
.pie-chart { width: 160px; height: 160px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.pie-center { width: 100px; height: 100px; background: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; }
.pie-legend { display: flex; flex-direction: column; gap: 8px; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.dot { width: 10px; height: 10px; border-radius: 50%; }
</style>
