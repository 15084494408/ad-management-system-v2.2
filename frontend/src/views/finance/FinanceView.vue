<template>
  <div class="page-container">
    <div class="page-header"><span class="page-title">💰 财务概览</span></div>
    <div class="stat-grid">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-label">{{s.label}}</div>
        <div class="stat-value" :style="{color:s.color}">{{s.value}}</div>
      </div>
    </div>
    <div class="card"><div class="card-header-row"><span class="card-title">📈 近30天收款趋势</span></div><div style="height:120px;display:flex;align-items:flex-end;gap:3px;padding:8px 0;"><div v-for="(v,i) in trendData" :key="i" style="flex:1;background:linear-gradient(to top,#409eff,#66b1ff);border-radius:2px 2px 0 0;min-height:4px;" :style="{height:v+'%'}"></div></div></div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'; import { financeApi } from '@/api'
const stats = ref<any[]>([]); const trendData = ref<number[]>([])
onMounted(async()=>{ try{ const r=await financeApi.getOverview(); const d=r.data; stats.value=[{label:'本月收款',value:'¥'+Number(d.thisMonthIncome||0).toLocaleString(),color:'#409eff'},{label:'待收款',value:'¥'+Number(d.unpaidAmount||0).toLocaleString(),color:'#e6a23c'},{label:'会员余额总计',value:'¥'+Number(d.memberBalance||0).toLocaleString(),color:'#67c23a'},{label:'本月订单',value:(d.thisMonthOrders||0)+' 张',color:'#f56c6c'}]; if(d.trendData?.length) trendData.value=d.trendData }catch{ stats.value=[]; trendData.value=[] } })
</script>
<style scoped lang="scss">
.card-header-row{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;}
.card-title{font-size:15px;font-weight:600;}
</style>
