<template>
  <div class="page-container">
    <div class="page-header"><span class="page-title">📊 订单统计</span><div class="page-actions"><el-button @click="exportData"><el-icon><Download /></el-icon> 导出报表</el-button></div></div>

    <div class="stat-grid">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-icon" :style="{background: s.bg}">{{ s.icon }}</div>
        <div class="stat-info">
          <h3>{{ s.value }}</h3>
          <p>{{ s.label }}</p>
          <div v-if="s.trend" class="stat-trend" :class="s.trend > 0 ? 'up' : 'down'">{{ s.trend > 0 ? '↑' : '↓' }} {{ Math.abs(s.trend) }}%</div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <div class="chart-card card">
        <div class="card-title">📈 订单趋势</div>
        <div class="chart-container">
          <div class="bar-chart">
            <div v-for="(v, i) in trendData" :key="i" class="bar-wrap">
              <div class="bar" :style="{ height: (v.count / maxTrend * 100) + '%' }"></div>
              <span class="bar-label">{{ v.date?.slice(5) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="chart-card card">
        <div class="card-title">📊 订单状态分布</div>
        <div class="pie-container">
          <div class="pie-chart" :style="{ background: `conic-gradient(#409eff 0% ${statusData[0]?.pct}%, #67c23a ${statusData[0]?.pct}% ${statusData[0]?.pct + statusData[1]?.pct}%, #e6a23c ${statusData[0]?.pct + statusData[1]?.pct}% ${statusData[0]?.pct + statusData[1]?.pct + statusData[2]?.pct}%, #909399 ${statusData[0]?.pct + statusData[1]?.pct + statusData[2]?.pct}%)` }">
            <div class="pie-center">{{ summary.totalAmount?.toLocaleString() }}</div>
          </div>
          <div class="pie-legend">
            <div v-for="s in statusData" :key="s.label" class="legend-item"><span class="dot" :style="{background: s.color}"></span>{{ s.label }}: {{ s.count }}单 ({{ s.pct }}%)</div>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header-row"><span class="card-title">📋 订单明细</span></div>
      <el-table :data="orderList" stripe size="small">
        <el-table-column prop="orderNo" label="订单编号" width="140" />
        <el-table-column prop="customerName" label="客户" />
        <el-table-column prop="totalAmount" label="订单金额" width="120">
          <template #default="{ row }">¥{{ row.totalAmount?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="statusTagMap[row.status]?.type" size="small">{{ statusTagMap[row.status]?.label }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import request from '@/api/request'

const stats = ref([
  { label:'总订单数', value:'0', icon:'📋', bg:'linear-gradient(135deg,#409eff,#66b1ff)' },
  { label:'总金额', value:'¥0', icon:'💰', bg:'linear-gradient(135deg,#67c23a,#85ce61)' },
  { label:'已完成', value:'0', icon:'✅', bg:'linear-gradient(135deg,#00bcd4,#26c6da)' },
  { label:'进行中', value:'0', icon:'🔄', bg:'linear-gradient(135deg,#e6a23c,#ebb563)' },
])
const summary = ref<any>({})
const trendData = ref<any[]>([])
const statusData = ref([
  { label:'待确认', count:0, pct:0, color:'#409eff' },
  { label:'进行中', count:0, pct:0, color:'#67c23a' },
  { label:'已完成', count:0, pct:0, color:'#e6a23c' },
])
const orderList = ref<any[]>([])
const statusTagMap: Record<string, { label: string; type: string }> = {
  1:{ label:'待确认', type:'info' }, 2:{ label:'进行中', type:'primary' }, 3:{ label:'已完成', type:'success' }, 4:{ label:'已取消', type:'danger' }
}
const maxTrend = computed(() => Math.max(...trendData.value.map((v:any) => v.count), 1))

async function loadData() {
  try {
    const res = await request.get('/statistics/order/summary')
    summary.value = res.data || {}
    stats.value[0].value = summary.value.totalOrders || 0
    stats.value[1].value = '¥' + (summary.value.totalAmount || 0).toLocaleString()
    stats.value[2].value = summary.value.completedCount || 0
    stats.value[3].value = (summary.value.pendingCount || 0) + (summary.value.processingCount || 0)
  } catch {}
  try {
    const res = await request.get('/statistics/order/trend', { params: { type: 'daily' } })
    trendData.value = res.data || []
  } catch {}
  orderList.value = []
}
onMounted(loadData)
</script>

<style scoped lang="scss">
.charts-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-bottom: 20px; }
.pie-container { display: flex; align-items: center; gap: 20px; }
.pie-chart { width: 160px; height: 160px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.pie-center { width: 100px; height: 100px; background: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; }
.pie-legend { display: flex; flex-direction: column; gap: 8px; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.dot { width: 10px; height: 10px; border-radius: 50%; }
</style>
