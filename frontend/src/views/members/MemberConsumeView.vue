<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item" @click="$router.push('/members')">会员管理</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item active">消费记录</span>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">📊 消费记录（余额抵扣）</h1>
      <div class="page-actions">
        <button class="btn btn-default" @click="exportData">⬇️ 导出</button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon orange">💸</div>
        <div class="stat-info"><h3>¥{{ stats.monthConsume }}</h3><p>本月余额消费</p></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon blue">📊</div>
        <div class="stat-info"><h3>{{ stats.monthCount }}</h3><p>本月消费笔数</p></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">💵</div>
        <div class="stat-info"><h3>¥{{ stats.avgAmount }}</h3><p>平均消费金额</p></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">📊</div>
        <div class="stat-info"><h3>{{ stats.total }}</h3><p>消费记录总数</p></div>
      </div>
    </div>

    <!-- 搜索表单 -->
    <div class="search-form">
      <div class="form-group">
        <label>时间范围</label>
        <div style="display:flex;align-items:center;gap:6px;">
          <input v-model="searchForm.startDate" type="date" class="form-control" />
          <span style="color:#909399;">—</span>
          <input v-model="searchForm.endDate" type="date" class="form-control" />
        </div>
      </div>
      <div class="form-group">
        <label>关键词</label>
        <input v-model="searchForm.keyword" type="text" class="form-control" placeholder="搜索备注/会员ID" />
      </div>
      <div class="form-group" style="align-self:flex-end;">
        <button class="btn btn-primary" @click="handleSearch">🔍 搜索</button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="card">
      <table class="data-table consume-table">
        <thead>
          <tr>
            <th style="width:140px;">流水单号</th>
            <th style="min-width:130px;">会员</th>
            <th style="width:100px;">类型</th>
            <th style="width:110px;">消费金额</th>
            <th style="width:110px;">消费前余额</th>
            <th style="width:110px;">消费后余额</th>
            <th style="width:110px;">时间</th>
            <th>备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" style="text-align:center;padding:50px 20px;color:#909399;">
              <div style="font-size:28px;margin-bottom:8px;">⏳</div>
              <div>数据加载中...</div>
            </td>
          </tr>
          <tr v-else-if="list.length === 0">
            <td colspan="8" style="text-align:center;padding:50px 20px;color:#909399;">
              <div style="font-size:28px;margin-bottom:8px;">📭</div>
              <div>暂无消费记录</div>
            </td>
          </tr>
          <tr v-for="r in list" :key="r.id">
            <td style="color:#c0c4cc;font-size:12px;font-variant-numeric:tabular-nums;">#{{ r.transactionNo || r.id }}</td>
            <td style="font-weight:500;">{{ r.memberName || r.memberId }}</td>
            <td><span class="tag tag-warning">余额消费</span></td>
            <td style="font-weight:700;color:#f56c6c;font-variant-numeric:tabular-nums;">-¥{{ formatMoney(r.amount) }}</td>
            <td style="color:#909399;font-variant-numeric:tabular-nums;">¥{{ formatMoney(r.balanceBefore) }}</td>
            <td style="font-weight:600;font-variant-numeric:tabular-nums;">¥{{ formatMoney(r.balanceAfter) }}</td>
            <td style="color:#606266;font-size:12px;">{{ fmtDate(r.createTime) }}</td>
            <td style="color:#909399;font-size:12px;">{{ r.remark || '—' }}</td>
          </tr>
        </tbody>
      </table>
      <div class="pagination">
        <div class="pagination-info">共 {{ total }} 条记录</div>
        <div class="pagination-buttons">
          <button class="page-btn" :disabled="page <= 1" @click="page--;load()">«</button>
          <button v-for="p in pageRange" :key="p" :class="['page-btn', { active: p === page }]" @click="page=p;load()">{{ p }}</button>
          <button class="page-btn" :disabled="page >= totalPages" @click="page++;load()">»</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { memberApi } from '@/api'

const list = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 20

const searchForm = reactive({ keyword: '', startDate: '', endDate: '' })
const stats = reactive({ monthConsume: '0', monthCount: 0, avgAmount: '0', total: 0 })

function formatMoney(v: any) { return Number(v || 0).toLocaleString('zh', { minimumFractionDigits: 2 }) }
function fmtDate(d: any) {
  if (!d) return '-'
  return typeof d === 'string' ? d.slice(0, 10) : d.toISOString?.().slice(0, 10) || String(d).slice(0, 10)
}

const totalPages = computed(() => Math.ceil(total.value / pageSize))
const pageRange = computed(() => {
  const p = page.value, t = totalPages.value
  let arr = []
  for (let i = Math.max(1, p - 2); i <= Math.min(t, p + 2); i++) arr.push(i)
  return arr
})

function handleSearch() { page.value = 1; load() }
function exportData() { ElMessage.success('导出功能开发中') }

async function load() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, size: pageSize }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.startDate) params.startDate = searchForm.startDate
    if (searchForm.endDate) params.endDate = searchForm.endDate
    const r = await memberApi.getConsumeRecords(params)
    list.value = r.data?.records || r.data?.list || []
    total.value = r.data?.total || list.value.length
    stats.monthCount = list.value.length
    stats.total = total.value
    stats.monthConsume = list.value.reduce((s: number, r: any) => s + Number(r.amount || 0), 0).toLocaleString('zh', { minimumFractionDigits: 2 })
    const totalAmt = list.value.reduce((s: number, r: any) => s + Number(r.amount || 0), 0)
    stats.avgAmount = list.value.length > 0 ? (totalAmt / list.value.length).toFixed(1) : '0'
  } catch { list.value = [] }
  finally { loading.value = false }
}

onMounted(load)
</script>

<style scoped lang="scss">
:deep(.card) {
  overflow: hidden;
  padding: 0;
  table { width: 100%; border-collapse: collapse; }
}
:deep(.consume-table) {
  thead th {
    padding: 13px 16px;
    font-size: 12px;
    font-weight: 600;
    color: #909399;
    background: #fafbfc;
    border-bottom: 1px solid #ebeef5;
    text-align: left;
    white-space: nowrap;
  }
  tbody td {
    padding: 15px 16px;
    font-size: 13px;
    color: #303133;
    border-bottom: 1px solid #f2f3f5;
    vertical-align: middle;
  }
  tbody tr:hover td { background: #f8faff; }
  tbody tr:last-child td { border-bottom: none; }
}
:deep(.pagination) { padding: 16px 20px; }
</style>
