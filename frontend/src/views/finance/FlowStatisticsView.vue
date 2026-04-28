<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">📊 流水统计</span>
      <div class="page-actions">
        <el-button @click="exportData"><el-icon><Download /></el-icon> 导出</el-button>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="统计周期">
          <el-radio-group v-model="searchForm.period">
            <el-radio-button value="day">按日</el-radio-button>
            <el-radio-button value="week">按周</el-radio-button>
            <el-radio-button value="month">按月</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="流水类型">
          <el-select v-model="searchForm.flowType" placeholder="全部" clearable style="width:140px;">
            <el-option v-for="t in flowTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">💵</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalIncome?.toLocaleString() }}</div>
          <div class="stat-label">收入合计</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">📉</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalExpense?.toLocaleString() }}</div>
          <div class="stat-label">支出合计</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">📈</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ netAmount?.toLocaleString() }}</div>
          <div class="stat-label">净流水</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">📋</div>
        <div class="stat-info">
          <div class="stat-value">{{ recordCount }}</div>
          <div class="stat-label">流水笔数</div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <div class="chart-card card">
        <div class="card-title">📈 流水趋势</div>
        <div class="chart-container">
          <div class="bar-chart">
            <div v-for="(v, i) in trendData" :key="i" class="bar-wrap">
              <div class="bar" :style="{ height: (Math.abs(v.income) / maxTrend * 100) + '%', background: '#67c23a' }"></div>
              <div class="bar" :style="{ height: (Math.abs(v.expense) / maxTrend * 100) + '%', background: '#f56c6c', marginTop: '2px' }"></div>
              <span class="bar-label">{{ v.label }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="chart-card card">
        <div class="card-title">📊 类型分布</div>
        <div class="type-list">
          <div v-for="t in typeDistribution" :key="t.label" class="type-item">
            <div class="type-label">{{ t.label }}</div>
            <div class="type-bar-bg"><div class="type-bar" :style="{width: t.pct + '%', background: t.color}"></div></div>
            <div class="type-value">¥{{ t.amount?.toLocaleString() }}</div>
          </div>
        </div>
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
watch(() => financeStore.lastFinanceUpdate, () => loadData())

const searchForm = ref({ period: 'day', dateRange: [], flowType: '' })
const totalIncome = ref(0)
const totalExpense = ref(0)
const netAmount = ref(0)
const recordCount = ref(0)
const trendData = ref<any[]>([])
const flowTypes = ref([
  { label: '现金收入', value: 'cash_in' }, { label: '微信收入', value: 'wechat_in' },
  { label: '支付宝收入', value: 'alipay_in' }, { label: '物料采购', value: 'material' },
  { label: '人工费用', value: 'labor' }, { label: '其他支出', value: 'other_out' },
])
const typeDistribution = ref<any[]>([])

const maxTrend = computed(() => Math.max(...trendData.value.map(v => Math.max(Math.abs(v.income), Math.abs(v.expense))), 1))

async function loadData() {
  try {
    const res = await request.get('/finance/flow/statistics', { params: searchForm.value })
    const d = res.data || {}
    totalIncome.value = d.totalIncome || 0
    totalExpense.value = d.totalExpense || 0
    netAmount.value = d.netAmount || 0
    recordCount.value = d.recordCount || 0
  } catch {}
  trendData.value = []
}

function exportData() {
  exportToExcel({
    filename: '流水统计',
    title: '流水统计报表',
    header: ['统计项', '金额(¥)', '笔数', '备注'],
    data: [
      ['总收入', stats.value.totalIncome || 0, '', ''],
      ['总支出', stats.value.totalExpense || 0, '', ''],
      ['净流水', stats.value.netAmount || 0, '', `收入-支出`],
      ['今日总收入', stats.value.todayIncome || 0, `${stats.value.todayCount || 0} 笔`, ''],
      ['今日总支出', stats.value.todayExpense || 0, '', ''],
      ['今日平均单笔', stats.value.avgPerRecord || 0, '', ''],
      ['今日最大单笔', stats.value.maxRecord || 0, '', ''],
    ],
    summaryRow: ['', '', '', `导出时间：${new Date().toLocaleString()}`],
  })
}
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
