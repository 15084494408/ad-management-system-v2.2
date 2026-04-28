<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">每日流水</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">💵 流水记录 <span class="v2-badge">V2.2</span></h1>
      <div class="page-actions">
        <button class="btn btn-default" @click="exportData">⬇️ 导出</button>
        <button class="btn btn-primary" @click="showQuickRecord">+ 新增流水</button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon green">📈</div>
        <div class="stat-info">
          <h3>¥{{ stats.totalIncome?.toLocaleString() || '0.00' }}</h3>
          <p>总收入</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">📉</div>
        <div class="stat-info">
          <h3>¥{{ stats.totalExpense?.toLocaleString() || '0.00' }}</h3>
          <p>总支出</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon blue">📊</div>
        <div class="stat-info">
          <h3>¥{{ stats.netAmount?.toLocaleString() || '0.00' }}</h3>
          <p>净流水</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">🏆</div>
        <div class="stat-info">
          <h3>{{ total || 0 }}</h3>
          <p>总笔数</p>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-form">
      <div class="form-group">
        <label>日期范围</label>
        <input type="date" v-model="searchForm.startDate" class="form-control">
      </div>
      <div class="form-group">
        <label>至</label>
        <input type="date" v-model="searchForm.endDate" class="form-control">
      </div>
      <div class="form-group">
        <label>收支方向</label>
        <select v-model="searchForm.direction" class="form-control">
          <option value="">全部</option>
          <option value="income">仅收入</option>
          <option value="expense">仅支出</option>
        </select>
      </div>
      <div class="form-group" style="align-self:flex-end;">
        <button class="btn btn-primary" @click="currentPage=1; loadData()">🔍 搜索</button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="card">
      <table class="data-table">
        <thead>
          <tr>
            <th>编号</th>
            <th>金额</th>
            <th>方向</th>
            <th>来源</th>
            <th>分类</th>
            <th>关联对象</th>
            <th>支付方式</th>
            <th>备注</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in tableData" :key="record.record_no + record.create_time">
            <td style="font-size:12px;color:#606266;">{{ record.record_no || '-' }}</td>
            <td :class="record.direction === 'income' ? 'income-amount' : 'expense-amount'">
              {{ record.direction === 'income' ? '+' : '-' }}¥{{ Number(record.amount).toLocaleString() }}
            </td>
            <td>
              <span class="tag" :class="record.direction === 'income' ? 'tag-success' : 'tag-danger'">
                {{ record.direction === 'income' ? '收入' : '支出' }}
              </span>
            </td>
            <td>
              <span class="tag" :class="'tag-' + getSourceTag(record.source)">{{ getSourceLabel(record.source) }}</span>
            </td>
            <td>{{ record.category || '-' }}</td>
            <td>{{ record.related_name || '-' }}</td>
            <td>
              <span class="tag" :class="'tag-' + getPaymentTag(record.payment_method)">{{ getPaymentLabel(record.payment_method) }}</span>
            </td>
            <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" :title="record.remark">
              {{ record.remark || '-' }}
            </td>
            <td style="font-size:12px;color:#909399;">{{ formatTime(record.create_time) }}</td>
          </tr>
          <tr v-if="!tableData.length">
            <td colspan="9" style="text-align:center;padding:40px;color:#909399;">暂无流水记录</td>
          </tr>
        </tbody>
      </table>
      <div class="pagination">
        <div class="pagination-info">共 {{ total }} 笔记录</div>
        <div class="pagination-buttons">
          <button class="page-btn" :disabled="currentPage <= 1" @click="currentPage--; loadData()">«</button>
          <button class="page-btn" v-for="p in pageNumbers" :key="p" :class="{ active: currentPage === p }" @click="currentPage = p; loadData()">{{ p }}</button>
          <button class="page-btn" :disabled="currentPage >= totalPages" @click="currentPage++; loadData()">»</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { financeApi } from '@/api/modules/finance'
import { ElMessage } from 'element-plus'
import { exportToExcel } from '@/utils/excelExport'

const router = useRouter()

const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const tableData = ref<any[]>([])
const loading = ref(false)
const stats = ref<any>({})

const searchForm = reactive({
  startDate: '',
  endDate: '',
  direction: ''
})

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
const pageNumbers = computed(() => {
  const pages: number[] = []
  const start = Math.max(1, currentPage.value - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

function getSourceLabel(source: string): string {
  const map: Record<string, string> = {
    quick_record: '快速记账',
    member_recharge: '会员充值',
    member_consume: '会员消费',
    order_income: '订单收款',
    factory_bill: '工厂付款'
  }
  return map[source] || source
}

function getSourceTag(source: string): string {
  const map: Record<string, string> = {
    quick_record: 'primary',
    member_recharge: 'success',
    member_consume: 'danger',
    order_income: 'success',
    factory_bill: 'warning'
  }
  return map[source] || 'default'
}

function getPaymentLabel(method: string): string {
  const map: Record<string, string> = {
    cash: '现金', wechat: '微信', alipay: '支付宝', bank: '银行卡', transfer: '银行转账'
  }
  return map[method] || method || '-'
}

function getPaymentTag(method: string): string {
  const map: Record<string, string> = {
    cash: 'warning', wechat: 'success', alipay: 'primary', bank: 'default', transfer: 'default'
  }
  return map[method] || 'default'
}

function formatTime(time: string): string {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      current: currentPage.value,
      size: pageSize.value
    }
    if (searchForm.startDate) params.startDate = searchForm.startDate
    if (searchForm.endDate) params.endDate = searchForm.endDate
    if (searchForm.direction) params.direction = searchForm.direction

    const res = await financeApi.getAllFlow(params)
    if (res.code === 200) {
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
    }

    // 同时加载统计数据
    const summaryParams: Record<string, any> = {}
    if (searchForm.startDate) summaryParams.startDate = searchForm.startDate
    if (searchForm.endDate) summaryParams.endDate = searchForm.endDate
    const summaryRes = await financeApi.getOverview(summaryParams)
    if (summaryRes.code === 200) {
      stats.value = summaryRes.data || {}
    }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function showQuickRecord() {
  window.dispatchEvent(new CustomEvent('show-quick-account'))
}

// ── 导出流水记录 ──
function exportData() {
  if (!tableData.value.length) { ElMessage.warning('暂无数据可导出'); return }
  exportToExcel({
    filename: '流水记录',
    header: ['编号', '金额(¥)', '方向', '来源', '分类', '关联对象', '支付方式', '备注', '时间'],
    data: tableData.value.map(r => [
      r.record_no || '-',
      (r.direction === 'income' ? '+' : '-') + Number(r.amount || 0).toLocaleString(),
      r.direction === 'income' ? '收入' : '支出',
      getSourceLabel(r.source),
      r.category || '-',
      r.related_name || '-',
      getPaymentLabel(r.payment_method),
      r.remark || '-',
      formatTime(r.create_time),
    ]),
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.page-container { padding: 20px 24px; }

.income-amount { color: #67c23a; font-weight: 600; }
.expense-amount { color: #f56c6c; font-weight: 600; }

.tag {
  display: inline-block; padding: 3px 8px; border-radius: 4px;
  font-size: 11px; margin-right: 5px; white-space: nowrap;
}
.tag-primary { background: #ecf5ff; color: #409eff; }
.tag-success { background: #f0f9eb; color: #67c23a; }
.tag-warning { background: #fdf6ec; color: #e6a23c; }
.tag-danger { background: #fef0f0; color: #f56c6c; }
.tag-default { background: #f4f4f5; color: #909399; }
</style>
