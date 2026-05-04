<template>
  <div class="page-container">
    <div class="page-header"><span class="page-title">💰 财务概览</span></div>
    <div class="stat-grid">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-label">{{s.label}}</div>
        <div class="stat-value" :style="{color:s.color}">{{s.value}}</div>
      </div>
    </div>
    <div class="card">
      <div class="card-header-row"><span class="card-title">📈 近30天收款趋势</span></div>
      <div class="trend-chart" ref="chartRef">
        <div class="trend-bars">
          <div class="trend-col" v-for="(v,i) in trendData" :key="i">
            <div class="trend-amount" v-if="trendAmounts[i] > 0">¥{{ trendAmounts[i] }}</div>
            <div class="trend-bar" :style="{height:v+'%', background: trendAmounts[i] > 0 ? 'linear-gradient(to top, #409eff, #66b1ff)' : 'linear-gradient(to top, #e8e8e8, #f0f0f0)'}"></div>
          </div>
        </div>
        <div class="trend-labels">
          <span class="trend-label" v-for="(label,i) in trendLabels" :key="i"
            :class="{ 'is-today': i === trendLabels.length - 1 }">{{ label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'; import { financeApi } from '@/api'
const stats = ref<any[]>([]); const trendData = ref<number[]>([]); const trendLabels = ref<string[]>([]); const trendAmounts = ref<number[]>([])
onMounted(async()=>{ try{ const r=await financeApi.getOverview(); const d=r.data; stats.value=[{label:'本月收款',value:'¥'+Number(d.thisMonthIncome||0).toLocaleString(),color:'#409eff'},{label:'待收款',value:'¥'+Number(d.unpaidAmount||0).toLocaleString(),color:'#e6a23c'},{label:'会员余额总计',value:'¥'+Number(d.memberBalance||0).toLocaleString(),color:'#67c23a'},{label:'本月订单',value:(d.thisMonthOrders||0)+' 张',color:'#f56c6c'}]; if(d.trendData?.length) { trendData.value=d.trendData; trendLabels.value=d.trendLabels||[]; trendAmounts.value=d.trendAmounts||[] } }catch{ stats.value=[]; trendData.value=[]; trendLabels.value=[]; trendAmounts.value=[] } })
</script>
<style scoped lang="scss">
.card-header-row{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;}
.card-title{font-size:15px;font-weight:600;}
.trend-chart {
  padding: 8px 0;
}
.trend-bars {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 140px;
  padding: 0 4px;
}
.trend-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
}
.trend-amount {
  font-size: 10px;
  color: #409eff;
  font-weight: 600;
  white-space: nowrap;
  margin-bottom: 2px;
  line-height: 1;
}
.trend-bar {
  width: 100%;
  border-radius: 2px 2px 0 0;
  min-height: 4px;
  transition: height 0.3s ease;
}
.trend-labels {
  display: flex;
  gap: 2px;
  margin-top: 6px;
  padding: 0 4px;
}
.trend-label {
  flex: 1;
  font-size: 9px;
  color: #909399;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  &.is-today {
    color: #409eff;
    font-weight: 700;
  }
}
</style>