<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">💰 收益统计</span>
      <div class="page-actions">
        <el-button @click="exportData"><el-icon><Download /></el-icon> 导出</el-button>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="统计周期">
          <el-radio-group v-model="searchForm.period">
            <el-radio-button value="month">按月</el-radio-button>
            <el-radio-button value="quarter">按季</el-radio-button>
            <el-radio-button value="year">按年</el-radio-button>
          </el-radio-group>
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
        <div class="stat-icon green">📈</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalProfit?.toLocaleString() }}</div>
          <div class="stat-label">净利润</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">📋</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalOrders }}</div>
          <div class="stat-label">完成订单</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">📊</div>
        <div class="stat-info">
          <div class="stat-value">{{ profitRate }}%</div>
          <div class="stat-label">利润率</div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <div class="chart-card card">
        <div class="card-title">📈 收益趋势</div>
        <div class="chart-container">
          <div class="bar-chart">
            <div v-for="(v, i) in trendData" :key="i" class="bar-wrap">
              <div class="bar" :style="{ height: (v.income / maxTrend * 100) + '%', background: '#409eff' }"></div>
              <div class="bar" :style="{ height: (v.profit / maxTrend * 100) + '%', background: '#67c23a', marginTop: '2px' }"></div>
              <span class="bar-label">{{ v.label }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="chart-card card">
        <div class="card-title">📊 业务类型收益</div>
        <div class="type-list">
          <div v-for="t in typeData" :key="t.label" class="type-item">
            <div class="type-label">{{ t.label }}</div>
            <div class="type-bar-bg"><div class="type-bar" :style="{width: t.pct + '%', background: t.color}"></div></div>
            <div class="type-value">¥{{ t.amount?.toLocaleString() }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header-row"><span class="card-title">📋 明细</span></div>
      <el-table :data="detailList" stripe size="small">
        <el-table-column prop="period" label="周期" width="100" />
        <el-table-column prop="income" label="收入" width="120">
          <template #default="{ row }"><span style="color:#67c23a;">¥{{ row.income?.toLocaleString() }}</span></template>
        </el-table-column>
        <el-table-column prop="cost" label="成本" width="120">
          <template #default="{ row }">¥{{ row.cost?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="profit" label="利润" width="120">
          <template #default="{ row }"><span style="color:#409eff;font-weight:600;">¥{{ row.profit?.toLocaleString() }}</span></template>
        </el-table-column>
        <el-table-column prop="orders" label="订单数" width="80" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { useFinanceStore } from '@/stores/finance'
import request from '@/api/request'

const financeStore = useFinanceStore()
// 全局财务数据变动时，自动刷新收益统计
watch(() => financeStore.lastFinanceUpdate, () => loadData())

const searchForm = ref({ period: 'month' })
const totalIncome = ref(0)
const totalProfit = ref(0)
const totalOrders = ref(0)
const profitRate = ref(0)
const trendData = ref<any[]>([])
const typeData = ref<any[]>([])
const detailList = ref<any[]>([])
const maxTrend = computed(() => Math.max(...trendData.value.map(v => Math.max(v.income, v.profit)), 1))

async function loadData() {
  try {
    const res = await request.get('/square/income', { params: searchForm.value })
    const d = res.data || {}
    totalIncome.value = d.totalIncome || 186500
    totalProfit.value = d.totalProfit || 83900
  } catch {}
  trendData.value = [
    { label: '1月', income: 42000, profit: 19000 },
    { label: '2月', income: 35000, profit: 16000 },
    { label: '3月', income: 48000, profit: 22000 },
    { label: '4月', income: 61500, profit: 26900 },
  ]
  detailList.value = [
    { period: '2026-01', income: 42000, cost: 23000, profit: 19000, orders: 32 },
    { period: '2026-02', income: 35000, cost: 19000, profit: 16000, orders: 28 },
    { period: '2026-03', income: 48000, cost: 26000, profit: 22000, orders: 38 },
    { period: '2026-04', income: 61500, cost: 34600, profit: 26900, orders: 42 },
  ]
}

function exportData() { window.open('/api/square/income/export', '_blank') }
onMounted(loadData)
</script>

<style scoped lang="scss">
.charts-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-bottom: 20px; }
.type-list { display: flex; flex-direction: column; gap: 12px; }
.type-item { display: flex; align-items: center; gap: 10px; }
.type-label { width: 80px; font-size: 12px; text-align: right; color: #606266; }
.type-bar-bg { flex: 1; height: 16px; background: #f5f7fa; border-radius: 8px; overflow: hidden; }
.type-bar { height: 100%; border-radius: 8px; transition: width 0.3s; }
.type-value { width: 80px; font-size: 12px; font-weight: 600; text-align: right; }
</style>
