<template>
  <div class="page-container">
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">财务流水</span>
    </div>

    <div class="page-header">
      <h1 class="page-title">💵 财务流水 <span class="v2-badge">统一</span></h1>
      <div class="page-actions">
        <button class="btn btn-default" @click="showFilter = !showFilter">
          {{ showFilter ? '收起筛选' : '🔍 筛选' }}
        </button>
        <button class="btn btn-primary" @click="router.push('/finance')">⚡ 快速记账</button>
      </div>
    </div>

    <!-- 顶部统计 -->
    <div class="stat-grid" style="margin-bottom:16px;">
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(0,212,255,0.12)">💰</div>
        <div class="stat-info">
          <div class="stat-value" style="color:#00d4ff;">¥{{ formatMoney(stats.income) }}</div>
          <div class="stat-label">收入合计</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(255,107,107,0.12)">💸</div>
        <div class="stat-info">
          <div class="stat-value" style="color:#ff6b6b;">¥{{ formatMoney(stats.expense) }}</div>
          <div class="stat-label">支出合计</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(102,187,106,0.12)">📊</div>
        <div class="stat-info">
          <div class="stat-value" :style="{ color: netProfit >= 0 ? '#66bb6a' : '#ff6b6b' }">
            ¥{{ formatMoney(netProfit) }}
          </div>
          <div class="stat-label">净收支</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(171,71,188,0.12)">📋</div>
        <div class="stat-info">
          <div class="stat-value" style="color:#ab47bc;">{{ totalRecords }}</div>
          <div class="stat-label">总笔数</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="search-form" v-if="showFilter">
      <div class="form-group">
        <label>时间范围</label>
        <div style="display:flex;gap:8px;">
          <input type="date" v-model="query.startDate" class="form-control">
          <span style="line-height:36px;color:#909399;">至</span>
          <input type="date" v-model="query.endDate" class="form-control">
        </div>
      </div>
      <div class="form-group">
        <label>收支方向</label>
        <select v-model="query.direction" class="form-control">
          <option value="">全部</option>
          <option value="income">收入</option>
          <option value="expense">支出</option>
        </select>
      </div>
      <div class="form-group" style="align-self:flex-end;">
        <button class="btn btn-primary" @click="loadData">🔍 查询</button>
        <button class="btn btn-default" style="margin-left:8px;" @click="resetFilter">重置</button>
      </div>
    </div>

    <!-- 流水表格 -->
    <div class="card">
      <table class="data-table">
        <thead>
          <tr>
            <th>来源</th>
            <th>编号</th>
            <th>类别</th>
            <th>金额</th>
            <th>关联</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in records" :key="r.recordNo + r.createTime">
            <td>
              <span class="source-tag" :class="'src-' + (r.source || 'quick')">
                {{ sourceLabel(r.source) }}
              </span>
            </td>
            <td class="mono">{{ r.recordNo }}</td>
            <td>{{ r.category }}</td>
            <td>
              <span :style="{ color: r.direction === 'income' ? '#00d4ff' : '#ff6b6b', fontWeight: 600 }">
                {{ r.direction === 'income' ? '+' : '-' }}¥{{ formatMoney(r.amount) }}
              </span>
            </td>
            <td>{{ r.relatedName || '-' }}</td>
            <td class="mono">{{ formatTime(r.createTime) }}</td>
          </tr>
          <tr v-if="!records.length && !loading">
            <td colspan="6" style="text-align:center;padding:40px;color:#909399;">暂无流水数据</td>
          </tr>
        </tbody>
      </table>
      <div class="pagination" v-if="totalRecords > 0">
        <div class="pagination-info">共 {{ totalRecords }} 条</div>
        <div class="pagination-buttons">
          <button class="page-btn" :disabled="query.page <= 1" @click="query.page--; loadData()">«</button>
          <button class="page-btn" v-for="p in pageNumbers" :key="p" :class="{ active: query.page === p }" @click="query.page = p; loadData()">{{ p }}</button>
          <button class="page-btn" :disabled="query.page >= totalPages" @click="query.page++; loadData()">»</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'

const router = useRouter()
const loading = ref(false)
const records = ref<any[]>([])
const stats = reactive({ income: 0, expense: 0 })
const totalRecords = ref(0)
const showFilter = ref(false)

const query = reactive({
  startDate: '',
  endDate: '',
  direction: '',
  page: 1,
  size: 20,
})

const totalPages = computed(() => Math.max(1, Math.ceil(totalRecords.value / query.size)))
const pageNumbers = computed(() => {
  const p: number[] = []
  const s = Math.max(1, query.page - 2)
  const e = Math.min(totalPages.value, s + 4)
  for (let i = s; i <= e; i++) p.push(i)
  return p
})

const netProfit = computed(() => stats.income - stats.expense)

function sourceLabel(src: string) {
  const map: Record<string, string> = {
    quick_record: '记账', member_recharge: '充值', member_consume: '消费',
    factory_bill: '工厂', square_income: '广场',
  }
  return map[src] || src || '记账'
}

function formatMoney(v: any) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(t: any) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

function resetFilter() {
  query.startDate = ''
  query.endDate = ''
  query.direction = ''
  query.page = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: query.page, size: query.size }
    if (query.startDate) params.startDate = query.startDate + ' 00:00:00'
    if (query.endDate) params.endDate = query.endDate + ' 23:59:59'
    if (query.direction) params.direction = query.direction

    const res = await request.get('/finance/all-flow', { params })
    const d = res.data || {}
    records.value = d.records || []
    totalRecords.value = d.total || 0

    // 计算收发合计
    let inc = 0, exp = 0
    ;(d.records || []).forEach((r: any) => {
      const amt = Number(r.amount) || 0
      if (r.direction === 'income') inc += amt
      else exp += amt
    })
    stats.income = inc
    stats.expense = exp
  } catch { records.value = []; totalRecords.value = 0 }
  finally { loading.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.v2-badge {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; font-size: 10px; padding: 2px 8px;
  border-radius: 4px; margin-left: 8px; vertical-align: middle;
}
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card {
  background: #fff; border-radius: 10px; padding: 16px 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.stat-icon {
  width: 46px; height: 46px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}
.stat-value { font-size: 22px; font-weight: 700; font-family: 'Courier New', monospace; }
.stat-label { font-size: 12px; color: #909399; margin-top: 2px; }
.search-form {
  display: flex; gap: 16px; flex-wrap: wrap; align-items: flex-end;
  background: #fafbfc; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px;
}
.form-group { display: flex; flex-direction: column; gap: 4px; }
.form-group label { font-size: 12px; color: #606266; font-weight: 500; }
.form-control {
  height: 36px; border: 1px solid #dcdfe6; border-radius: 6px;
  padding: 0 12px; font-size: 13px; outline: none;
}
.form-control:focus { border-color: #409eff; }
.source-tag {
  display: inline-block; font-size: 11px; padding: 2px 8px;
  border-radius: 4px; font-weight: 600;
}
.src-quick_record { background: #ecf5ff; color: #409eff; }
.src-member_recharge { background: #fdf6ec; color: #e6a23c; }
.src-member_consume { background: #fef0f0; color: #f56c6c; }
.src-factory_bill { background: #f0f9eb; color: #67c23a; }
.src-square_income { background: #f3e5f5; color: #9c27b0; }
.mono { font-family: 'Courier New', monospace; font-size: 12px; color: #606266; }
</style>
