<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">📦 物料统计</span>
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
          </el-radio-group>
        </el-form-item>
        <el-form-item label="物料分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部" clearable style="width:150px;">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">📦</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalTypes }}</div>
          <div class="stat-label">物料种类</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">💰</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalPurchase?.toLocaleString() }}</div>
          <div class="stat-label">采购总额</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">📉</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalConsumed?.toLocaleString() }}</div>
          <div class="stat-label">消耗总额</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">⚠️</div>
        <div class="stat-info">
          <div class="stat-value">{{ warningCount }}</div>
          <div class="stat-label">预警次数</div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <div class="chart-card card">
        <div class="card-title">📈 采购消耗趋势</div>
        <div class="chart-container">
          <div class="bar-chart">
            <div v-for="(v, i) in trendData" :key="i" class="bar-wrap">
              <div class="bar" :style="{ height: (v.purchase / maxTrend * 100) + '%', background: '#409eff' }"></div>
              <div class="bar" :style="{ height: (v.consumed / maxTrend * 100) + '%', background: '#e6a23c', marginTop: '2px' }"></div>
              <span class="bar-label">{{ v.month }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="chart-card card">
        <div class="card-title">📊 分类消耗占比</div>
        <div class="pie-container">
          <div class="pie-chart" :style="{ background: pieGradient }">
            <div class="pie-center">消耗统计</div>
          </div>
          <div class="pie-legend">
            <div v-for="c in categoryData" :key="c.name" class="legend-item">
              <span class="dot" :style="{background: c.color}"></span>{{ c.name }}: ¥{{ c.amount?.toLocaleString() }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header-row"><span class="card-title">📋 TOP 消耗物料</span></div>
      <el-table :data="topMaterials" stripe size="small">
        <el-table-column type="index" label="排名" width="60" />
        <el-table-column prop="name" label="物料名称" min-width="120" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column prop="totalConsumed" label="消耗数量" width="100" />
        <el-table-column prop="unit" label="单位" width="60" />
        <el-table-column prop="totalCost" label="消耗金额" width="120">
          <template #default="{ row }">¥{{ row.totalCost?.toLocaleString() }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import request from '@/api/request'

const searchForm = ref({ period: 'month', categoryId: '' })
const totalTypes = ref(0)
const totalPurchase = ref(0)
const totalConsumed = ref(0)
const warningCount = ref(0)
const categories = ref<any[]>([])
const trendData = ref<any[]>([])
const categoryData = ref<any[]>([])
const topMaterials = ref<any[]>([])

const pieGradient = computed(() => {
  const cats = categoryData.value
  const total = cats.reduce((s, c) => s + c.amount, 0)
  let acc = 0
  return cats.map(c => {
    const pct = (c.amount / total) * 100
    const start = acc; acc += pct
    return `${c.color} ${start}% ${acc}%`
  }).join(', ')
})

const maxTrend = computed(() => Math.max(...trendData.value.map(v => Math.max(v.purchase, v.consumed)), 1))

async function loadData() {
  try {
    const res = await request.get('/statistics/material/summary', { params: searchForm.value })
    const d = res.data || {}
    totalPurchase.value = d.totalPurchase || 0
    totalConsumed.value = d.totalConsumed || 0
  } catch {}
  trendData.value = []
  topMaterials.value = []
}

function exportData() { window.open('/api/statistics/material/export', '_blank') }
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
