<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">💰 收益统计</span>
      <div class="page-actions">
        <el-button @click="exportData" class="export-btn">
          <el-icon><Download /></el-icon> 导出
        </el-button>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="统计周期">
          <el-radio-group v-model="searchForm.period" class="period-group">
            <el-radio-button value="month">按月</el-radio-button>
            <el-radio-button value="quarter">按季</el-radio-button>
            <el-radio-button value="year">按年</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData" class="search-btn">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="stat-grid">
      <div class="stat-card gradient-blue">
        <div class="stat-icon">💰</div>
        <div class="stat-content">
          <div class="stat-value">¥{{ totalIncome?.toLocaleString() }}</div>
          <div class="stat-label">总收入</div>
          <div class="stat-trend up">
            <span class="trend-icon">↑</span> 12.5%
          </div>
        </div>
      </div>
      <div class="stat-card gradient-green">
        <div class="stat-icon">📈</div>
        <div class="stat-content">
          <div class="stat-value">¥{{ totalProfit?.toLocaleString() }}</div>
          <div class="stat-label">净利润</div>
          <div class="stat-trend up">
            <span class="trend-icon">↑</span> 8.3%
          </div>
        </div>
      </div>
      <div class="stat-card gradient-orange">
        <div class="stat-icon">📋</div>
        <div class="stat-content">
          <div class="stat-value">{{ totalOrders }}</div>
          <div class="stat-label">完成订单</div>
          <div class="stat-trend up">
            <span class="trend-icon">↑</span> 15.2%
          </div>
        </div>
      </div>
      <div class="stat-card gradient-purple">
        <div class="stat-icon">📊</div>
        <div class="stat-content">
          <div class="stat-value">{{ profitRate }}%</div>
          <div class="stat-label">利润率</div>
          <div class="stat-trend neutral">
            <span class="trend-icon">-</span> 持平
          </div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <div class="chart-card card">
        <div class="card-title">
          <span class="title-icon">📈</span>
          <span>收益趋势</span>
          <div class="chart-legend">
            <span class="legend-item"><span class="dot blue"></span>收入</span>
            <span class="legend-item"><span class="dot green"></span>利润</span>
          </div>
        </div>
        <div class="chart-container">
          <div class="bar-chart">
            <div v-for="(v, i) in trendData" :key="i" class="bar-wrap">
              <div class="bar-group">
                <div class="bar income-bar" :style="{ height: (v.income / maxTrend * 100) + '%' }"></div>
                <div class="bar profit-bar" :style="{ height: (v.profit / maxTrend * 100) + '%' }"></div>
              </div>
              <span class="bar-label">{{ v.label }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="chart-card card">
        <div class="card-title">
          <span class="title-icon">📊</span>
          <span>业务类型收益</span>
        </div>
        <div class="type-list">
          <div v-for="t in typeData" :key="t.label" class="type-item">
            <div class="type-label">{{ t.label }}</div>
            <div class="type-bar-bg">
              <div class="type-bar" :style="{width: t.pct + '%', background: t.color}"></div>
            </div>
            <div class="type-value">¥{{ t.amount?.toLocaleString() }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="detail-card card">
      <div class="card-title">
        <span class="title-icon">📋</span>
        <span>明细</span>
        <span class="card-badge">共 {{ detailList.length }} 条</span>
      </div>
      <el-table :data="detailList" stripe size="default" class="detail-table" :header-cell-style="{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', color: '#fff', fontWeight: '600' }">
        <el-table-column prop="period" label="周期" width="140" align="center">
          <template #default="{ row }">
            <div class="period-cell">
              <span class="period-icon">📅</span>
              <span class="period-text">{{ row.period }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="income" label="收入（元）" min-width="160" align="right">
          <template #default="{ row }">
            <div class="money-cell income">
              <span class="money-prefix">¥</span>
              <span class="money-value">{{ row.income?.toLocaleString() }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="cost" label="成本（元）" min-width="160" align="right">
          <template #default="{ row }">
            <div class="money-cell cost">
              <span class="money-prefix">¥</span>
              <span class="money-value">{{ row.cost?.toLocaleString() }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="profit" label="利润（元）" min-width="160" align="right">
          <template #default="{ row }">
            <div class="money-cell profit">
              <span class="money-prefix">¥</span>
              <span class="money-value">{{ row.profit?.toLocaleString() }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="profitRate" label="利润率" width="120" align="center">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress
                :percentage="calcProfitRate(row)"
                :color="getRateColor(calcProfitRate(row))"
                :stroke-width="6"
                :show-text="true"
                :format="format => format + '%'"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="orders" label="订单数" width="100" align="center">
          <template #default="{ row }">
            <div class="orders-cell">
              <span class="orders-icon">📦</span>
              <span class="orders-value">{{ row.orders }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 合计行 -->
      <div class="summary-row">
        <div class="summary-item">
          <span class="summary-label">合计</span>
        </div>
        <div class="summary-item money-cell income">
          <span class="money-prefix">¥</span>
          <span class="money-value">{{ totalIncome?.toLocaleString() }}</span>
        </div>
        <div class="summary-item money-cell cost">
          <span class="money-prefix">¥</span>
          <span class="money-value">{{ (totalIncome - totalProfit)?.toLocaleString() }}</span>
        </div>
        <div class="summary-item money-cell profit">
          <span class="money-prefix">¥</span>
          <span class="money-value">{{ totalProfit?.toLocaleString() }}</span>
        </div>
        <div class="summary-item money-cell profit">
          <span class="money-prefix">¥</span>
          <span class="money-value">{{ profitRate }}%</span>
        </div>
        <div class="summary-item money-cell profit">
          <span class="orders-icon">📦</span>
          <span class="orders-value">{{ totalOrders }}</span>
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

function calcProfitRate(row: any) {
  if (!row.income || row.income === 0) return 0
  return Math.round((row.profit / row.income) * 100 * 10) / 10
}

function getRateColor(rate: number) {
  if (rate >= 50) return '#67c23a'
  if (rate >= 30) return '#409eff'
  if (rate >= 20) return '#e6a23c'
  return '#f56c6c'
}

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
  typeData.value = [
    { label: '广告制作', amount: 89500, pct: 48, color: '#409eff' },
    { label: '设计服务', amount: 52300, pct: 28, color: '#67c23a' },
    { label: '物料采购', amount: 32000, pct: 17, color: '#e6a23c' },
    { label: '其他', amount: 12700, pct: 7, color: '#909399' },
  ]
  totalOrders.value = 140
  profitRate.value = 45
  detailList.value = [
    { period: '2026-01', income: 42000, cost: 23000, profit: 19000, orders: 32 },
    { period: '2026-02', income: 35000, cost: 19000, profit: 16000, orders: 28 },
    { period: '2026-03', income: 48000, cost: 26000, profit: 22000, orders: 38 },
    { period: '2026-04', income: 61500, cost: 34600, profit: 26900, orders: 42 },
  ]
}

function exportData() {
  if (detailList.value.length === 0) { ElMessage.warning('暂无数据可导出'); return }
  const periodLabel = searchForm.value.period === 'month' ? '按月' : searchForm.value.period === 'quarter' ? '按季' : '按年'
  exportToExcel({
    filename: '广场收入统计',
    header: ['周期', '收入', '成本', '利润', '订单数'],
    data: detailList.value.map(row => [
      row.period,
      `¥${(row.income || 0).toLocaleString()}`,
      `¥${(row.cost || 0).toLocaleString()}`,
      `¥${(row.profit || 0).toLocaleString()}`,
      row.orders || 0,
    ]),
    summaryRow: ['合计', `¥${totalIncome.value.toLocaleString()}`, '', `¥${totalProfit.value.toLocaleString()}`, `${totalOrders}`],
    infoRows: [[`导出时间：${new Date().toLocaleString()}`], [`统计周期：${periodLabel}`]],
  })
}
onMounted(loadData)
</script>

<style scoped lang="scss">
// 统计卡片渐变背景
.gradient-blue { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.gradient-green { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
.gradient-orange { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.gradient-purple { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;

  .stat-card {
    border-radius: 16px;
    padding: 24px;
    display: flex;
    align-items: center;
    gap: 16px;
    color: #fff;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
    transition: transform 0.3s, box-shadow 0.3s;
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: -50%;
      right: -50%;
      width: 100%;
      height: 100%;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 50%;
    }

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
    }

    .stat-icon {
      font-size: 40px;
      filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1));
    }

    .stat-content {
      flex: 1;

      .stat-value {
        font-size: 28px;
        font-weight: 700;
        line-height: 1.2;
        text-shadow: 0 1px 2px rgba(0,0,0,0.1);
      }

      .stat-label {
        font-size: 14px;
        opacity: 0.9;
        margin-top: 4px;
      }

      .stat-trend {
        font-size: 12px;
        margin-top: 8px;
        padding: 2px 8px;
        border-radius: 10px;
        display: inline-block;
        background: rgba(255,255,255,0.2);

        &.up { background: rgba(103, 194, 58, 0.3); }
        &.down { background: rgba(245, 108, 108, 0.3); }
        &.neutral { background: rgba(144, 147, 153, 0.3); }
      }
    }
  }
}

.charts-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  .card-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 8px;

    .title-icon { font-size: 20px; }

    .chart-legend {
      margin-left: auto;
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: #606266;

      .legend-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }

      .dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;

        &.blue { background: #409eff; }
        &.green { background: #67c23a; }
      }
    }

    .card-badge {
      margin-left: auto;
      font-size: 12px;
      font-weight: normal;
      color: #909399;
      background: #f5f7fa;
      padding: 2px 10px;
      border-radius: 10px;
    }
  }
}

.chart-container {
  height: 200px;
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 100%;
  padding-top: 20px;

  .bar-wrap {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;
    max-width: 80px;

    .bar-group {
      display: flex;
      gap: 4px;
      align-items: flex-end;
      height: 160px;
      width: 100%;
      justify-content: center;
    }

    .bar {
      width: 28px;
      border-radius: 6px 6px 0 0;
      transition: height 0.5s ease-out;
      min-height: 4px;

      &.income-bar {
        background: linear-gradient(180deg, #409eff 0%, #66b1ff 100%);
        box-shadow: 0 2px 8px rgba(64, 158, 255, 0.4);
      }

      &.profit-bar {
        background: linear-gradient(180deg, #67c23a 0%, #85ce61 100%);
        box-shadow: 0 2px 8px rgba(103, 194, 58, 0.4);
      }
    }

    .bar-label {
      margin-top: 8px;
      font-size: 12px;
      color: #606266;
    }
  }
}

.type-list {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .type-item {
    display: flex;
    align-items: center;
    gap: 12px;

    .type-label {
      width: 70px;
      font-size: 13px;
      color: #606266;
      text-align: right;
    }

    .type-bar-bg {
      flex: 1;
      height: 20px;
      background: #f5f7fa;
      border-radius: 10px;
      overflow: hidden;

      .type-bar {
        height: 100%;
        border-radius: 10px;
        transition: width 0.5s ease-out;
      }
    }

    .type-value {
      width: 80px;
      font-size: 13px;
      font-weight: 600;
      text-align: right;
      color: #303133;
    }
  }
}

// 明细表格美化
.detail-card {
  .card-title {
    margin-bottom: 16px;
  }

  .detail-table {
    border-radius: 12px;
    overflow: hidden;

    :deep(.el-table__row) {
      transition: background 0.2s;

      &:hover > td {
        background: #f5f7fa !important;
      }
    }

    :deep(td.el-table__cell) {
      padding: 14px 0;
    }
  }
}

.period-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: #303133;

  .period-icon { font-size: 14px; }
  .period-text { font-family: 'JetBrains Mono', monospace; }
}

.money-cell {
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;

  .money-prefix {
    font-size: 12px;
    opacity: 0.7;
    margin-right: 2px;
  }

  .money-value {
    font-size: 15px;
  }

  &.income {
    color: #67c23a;
  }

  &.cost {
    color: #909399;
  }

  &.profit {
    color: #409eff;
  }
}

.rate-cell {
  .el-progress {
    width: 80px;
    margin: 0 auto;
  }

  :deep(.el-progress__text) {
    font-size: 12px !important;
    font-weight: 600;
  }
}

.orders-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;

  .orders-icon { font-size: 14px; }
  .orders-value { font-weight: 600; color: #606266; }
}

// 合计行
.summary-row {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 0 0 12px 12px;
  color: #fff;
  font-weight: 600;
  margin: 0 -20px -20px -20px;

  .summary-item {
    flex: 1;
    display: flex;
    align-items: center;

    &.money-cell {
      justify-content: flex-end;
      color: #fff;

      .money-prefix { opacity: 0.8; }
      .money-value { font-size: 16px; }
    }

    &:first-child {
      .summary-label {
        font-size: 14px;
        font-weight: 600;
      }
    }
  }
}

.search-form {
  margin-bottom: 20px;
  background: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  :deep(.el-form-item__label) {
    font-weight: 500;
  }
}

.period-group {
  :deep(.el-radio-button__inner) {
    border-radius: 8px !important;
  }
}

.search-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;

  &:hover {
    background: linear-gradient(135deg, #5a6fd6 0%, #6a4190 100%);
  }
}

.export-btn {
  border-radius: 8px;

  &:hover {
    background: #f5f7fa;
  }
}

// 响应式
@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
