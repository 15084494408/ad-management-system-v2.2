<template>
  <div class="commission-page">
    <!-- 顶栏 -->
    <div class="top-bar">
      <div class="top-left">
        <div class="page-icon">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <div>
          <h1 class="page-title">设计师提成</h1>
          <p class="page-sub">所有关联了设计师的订单自动纳入提成统计</p>
        </div>
      </div>
      <div class="top-right">
        <button class="action-btn secondary" @click="switchView">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/>
            <rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>
          </svg>
          {{ viewMode === 'detail' ? '汇总视图' : '明细视图' }}
        </button>
        <button class="action-btn secondary" @click="recalcAll" :disabled="recalcLoading">
          <svg :class="{ spinning: recalcLoading }" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 2v6h-6"/><path d="M3 12a9 9 0 0 1 15-6.7L21 8"/><path d="M3 22v-6h6"/><path d="M21 12a9 9 0 0 1-15 6.7L3 16"/>
          </svg>
          重算全部
        </button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card glass pending">
        <div class="stat-icon"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg></div>
        <div class="stat-body">
          <div class="stat-amount">¥{{ overview.pendingAmount?.toFixed(2) }}</div>
          <div class="stat-meta">待结算 · {{ overview.pendingCount }}笔</div>
        </div>
        <div class="stat-trend up">+{{ overview.pendingCount }}</div>
      </div>
      <div class="stat-card glass settled">
        <div class="stat-icon"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg></div>
        <div class="stat-body">
          <div class="stat-amount">¥{{ overview.settledAmount?.toFixed(2) }}</div>
          <div class="stat-meta">已结算 · {{ overview.settledCount }}笔</div>
        </div>
        <div class="stat-trend up">+{{ overview.settledCount }}</div>
      </div>
      <div class="stat-card glass paid">
        <div class="stat-icon"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg></div>
        <div class="stat-body">
          <div class="stat-amount">¥{{ overview.paidAmount?.toFixed(2) }}</div>
          <div class="stat-meta">已打款 · {{ overview.paidCount }}笔</div>
        </div>
        <div class="stat-trend up">+{{ overview.paidCount }}</div>
      </div>
      <div class="stat-card glass total">
        <div class="stat-icon"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg></div>
        <div class="stat-body">
          <div class="stat-amount">¥{{ overview.totalAmount?.toFixed(2) }}</div>
          <div class="stat-meta">累计提成 · {{ overview.totalCount }}笔</div>
        </div>
        <div class="stat-trend up">{{ overview.totalCount }}</div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-group">
        <div class="filter-item">
          <label>设计师</label>
          <select v-model="searchForm.designerId" @change="onSearch">
            <option :value="null">全部设计师</option>
            <option v-for="d in designerList" :key="d.id" :value="d.id">{{ d.realName }}</option>
          </select>
        </div>
        <div class="filter-item">
          <label>状态</label>
          <select v-model="searchForm.status" @change="onSearch">
            <option :value="null">全部状态</option>
            <option :value="1">待结算</option>
            <option :value="2">已结算</option>
            <option :value="3">已打款</option>
          </select>
        </div>
        <div class="filter-item">
          <label>搜索</label>
          <input v-model="searchForm.keyword" placeholder="订单号 / 设计师" @keyup.enter="onSearch" />
        </div>
        <button class="action-btn primary" @click="onSearch">搜索</button>
        <button class="action-btn ghost" @click="resetSearch">重置</button>
      </div>
      <div class="filter-actions">
        <button class="action-btn accent" @click="showRateDialog">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="20" x2="12" y2="4"/><polyline points="6 10 12 4 18 10"/></svg>
          设置提成比例
        </button>
      </div>
    </div>

    <!-- 汇总视图 -->
    <div v-if="viewMode === 'summary'" class="table-wrap">
      <table class="data-table" v-loading="summaryLoading">
        <thead><tr>
          <th>设计师</th><th style="text-align:center">订单数</th>
          <th style="text-align:right">计算基数</th><th style="text-align:right">总提成</th>
          <th style="text-align:center">待结算</th><th style="text-align:center">已结算</th>
          <th style="text-align:center">已打款</th><th style="text-align:center">操作</th>
        </tr></thead>
        <tbody>
          <tr v-for="row in designerSummary" :key="row.designerId">
            <td><span class="name-tag">{{ row.designerName }}</span></td>
            <td style="text-align:center"><span class="count-badge">{{ row.count }}</span></td>
            <td style="text-align:right" class="mono">¥{{ row.totalBase?.toFixed(2) }}</td>
            <td style="text-align:right"><span class="commission-amount">¥{{ row.totalCommission?.toFixed(2) }}</span></td>
            <td style="text-align:center"><span v-if="row.pendingCount > 0" class="tag pending">{{ row.pendingCount }}</span><span v-else class="dash">-</span></td>
            <td style="text-align:center"><span v-if="row.settledCount > 0" class="tag settled">{{ row.settledCount }}</span><span v-else class="dash">-</span></td>
            <td style="text-align:center"><span v-if="row.paidCount > 0" class="tag paid">{{ row.paidCount }}</span><span v-else class="dash">-</span></td>
            <td style="text-align:center">
              <button class="tbl-btn" @click="searchByDesigner(row)">明细</button>
              <button class="tbl-btn warn" @click="editRate(row)">比例</button>
              <button v-if="row.pendingCount > 0" class="tbl-btn ok" @click="batchSettle(row)">全部结算</button>
              <button v-if="row.settledCount > 0" class="tbl-btn warn" @click="batchPay(row)">全部打款</button>
            </td>
          </tr>
          <tr v-if="designerSummary.length === 0"><td colspan="8" class="empty-state">暂无提成数据</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 明细视图 -->
    <div v-else class="table-wrap">
      <!-- 批量操作栏 -->
      <div class="batch-bar" v-if="searchForm.designerId">
        <span>当前筛选：
          <strong>{{ selectedDesignerName }}</strong>
          （{{ pagination.total }} 条记录）
        </span>
        <div class="batch-actions">
          <button class="action-btn ok" @click="batchSettleCurrent">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
            全部结算
          </button>
          <button class="action-btn warn" @click="batchPayCurrent">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
            全部打款
          </button>
        </div>
      </div>
      <table class="data-table" v-loading="loading">
        <thead><tr>
          <th>设计师</th><th>订单编号</th><th style="text-align:right">订单金额</th>
          <th style="text-align:center">提成比例</th><th style="text-align:right">提成金额</th>
          <th style="text-align:center">状态</th><th>创建时间</th><th style="text-align:center">操作</th>
        </tr></thead>
        <tbody>
          <tr v-for="row in tableData" :key="row.orderId">
            <td><span class="name-tag">{{ row.designerName }}</span></td>
            <td><span class="order-no">{{ row.orderNo }}</span></td>
            <td style="text-align:right" class="mono">¥{{ row.baseAmount?.toFixed(2) }}</td>
            <td style="text-align:center"><span class="rate-badge">{{ row.commissionRate }}%</span></td>
            <td style="text-align:right"><span class="commission-amount" :class="{ zero: row.commissionAmount <= 0 }">¥{{ row.commissionAmount?.toFixed(2) }}</span></td>
            <td style="text-align:center"><span class="status-tag" :class="statusClass(row.status)">{{ statusText(row.status) }}</span></td>
            <td class="mono small">{{ row.createTime }}</td>
            <td style="text-align:center">
              <button v-if="row.status === 1 && row.id" class="tbl-btn ok" @click="settleRow(row)">结算</button>
              <button v-if="row.status === 2 && row.id" class="tbl-btn warn" @click="payRow(row)">打款</button>
              <button v-if="row.id" class="tbl-btn danger" @click="deleteRow(row)">删除</button>
              <span v-if="!row.id" class="dash">-</span>
            </td>
          </tr>
          <tr v-if="tableData.length === 0"><td colspan="8" class="empty-state">暂无提成数据</td></tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="pagination-bar" v-if="pagination.total > 0">
        <div class="page-info">共 {{ pagination.total }} 条记录</div>
        <div class="page-controls">
          <button :disabled="pagination.current <= 1" @click="goPage(pagination.current - 1)">‹</button>
          <span class="page-num" v-for="p in pageNumbers" :key="p" :class="{ active: p === pagination.current }" @click="goPage(p)">{{ p }}</span>
          <button :disabled="pagination.current >= totalPages" @click="goPage(pagination.current + 1)">›</button>
        </div>
        <select v-model="pagination.size" @change="loadData" class="page-size">
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
      </div>
    </div>

    <!-- 设置弹窗 -->
    <div class="modal-overlay" v-if="rateDialogVisible" @click.self="rateDialogVisible = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>设置提成比例</h3>
          <button class="close-btn" @click="rateDialogVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>设计师</label>
            <select v-model="rateForm.designerId" class="modal-select">
              <option :value="null" disabled>请选择设计师</option>
              <option v-for="d in designerList" :key="d.id" :value="d.id">{{ d.realName }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>提成比例</label>
            <div class="rate-input-wrap">
              <input type="number" v-model.number="rateForm.rate" min="0" max="100" step="0.5" class="modal-input rate" />
              <span class="rate-unit">%</span>
            </div>
          </div>
          <div class="form-group switch-row">
            <label>启用</label>
            <label class="switch">
              <input type="checkbox" v-model="rateForm.enabled" />
              <span class="slider"></span>
            </label>
          </div>
          <div class="form-hint">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            设置后将自动重新计算该设计师<strong>所有订单</strong>的提成金额
          </div>
        </div>
        <div class="modal-footer">
          <button class="action-btn ghost" @click="rateDialogVisible = false">取消</button>
          <button class="action-btn primary" :disabled="rateSubmitting" @click="submitRate">
            <span v-if="!rateSubmitting">保存并重算</span>
            <span v-else>重算中…</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { financeApi } from '@/api'

const viewMode = ref<'detail' | 'summary'>('summary')
const loading = ref(false)
const summaryLoading = ref(false)
const recalcLoading = ref(false)

const designerList = ref<any[]>([])
const overview = reactive({
  pendingAmount: 0, pendingCount: 0,
  settledAmount: 0, settledCount: 0,
  paidAmount: 0, paidCount: 0,
  totalAmount: 0, totalCount: 0,
})
const designerSummary = ref<any[]>([])
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, size: 10, total: 0 })

const searchForm = reactive({ designerId: null as number | null, status: null as number | null, keyword: '' })
const rateDialogVisible = ref(false)
const rateSubmitting = ref(false)
const rateForm = reactive({ designerId: null as number | null, rate: 5, enabled: true })

const totalPages = computed(() => Math.ceil(pagination.total / pagination.size) || 1)

const pageNumbers = computed(() => {
  const tp = totalPages.value
  const cur = pagination.current
  if (tp <= 7) return Array.from({ length: tp }, (_, i) => i + 1)
  if (cur <= 4) return [1, 2, 3, 4, 5, -1, tp]
  if (cur >= tp - 3) return [1, -1, tp - 4, tp - 3, tp - 2, tp - 1, tp]
  return [1, -1, cur - 1, cur, cur + 1, -1, tp]
})

function statusText(s: number) { return { 1: '待结算', 2: '已结算', 3: '已打款' }[s] || '未知' }
function statusClass(s: number) { return { 1: 'pending', 2: 'settled', 3: 'paid' }[s] || '' }

const selectedDesignerName = computed(() => {
  if (!searchForm.designerId) return ''
  const d = designerList.value.find(d => d.id === searchForm.designerId)
  return d ? d.realName : ''
})

function goPage(p: number) { if (p >= 1 && p <= totalPages.value) { pagination.current = p; loadData() } }

function switchView() {
  viewMode.value = viewMode.value === 'detail' ? 'summary' : 'detail'
  if (viewMode.value === 'detail') loadData(); else loadSummary()
}

function onSearch() { pagination.current = 1; loadData() }

async function loadDesigners() {
  try { const res = await financeApi.getCommissionDesigners(); designerList.value = res.data || [] } catch {}
}
async function loadOverview() {
  try { const res = await financeApi.getCommissionOverview(); Object.assign(overview, res.data || {}) } catch {}
}
async function loadSummary() {
  summaryLoading.value = true
  try { const res = await financeApi.getCommissionSummary(); designerSummary.value = res.data || [] } catch { designerSummary.value = [] }
  finally { summaryLoading.value = false }
}
async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = { current: pagination.current, size: pagination.size }
    if (searchForm.designerId) params.designerId = searchForm.designerId
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.keyword) params.keyword = searchForm.keyword
    const res = await financeApi.getCommissionList(params)
    const page = res.data || {}
    tableData.value = page.records || []
    pagination.total = page.total || 0
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

function searchByDesigner(d: any) { searchForm.designerId = d.designerId; viewMode.value = 'detail'; pagination.current = 1; loadData() }
function resetSearch() { searchForm.designerId = null; searchForm.status = null; searchForm.keyword = ''; pagination.current = 1; viewMode.value === 'detail' ? loadData() : loadSummary() }

function showRateDialog() { rateForm.designerId = null; rateForm.rate = 5; rateForm.enabled = true; rateDialogVisible.value = true }
function editRate(row: any) { rateForm.designerId = row.designerId; rateForm.rate = 5; rateForm.enabled = true; rateDialogVisible.value = true }

async function submitRate() {
  if (!rateForm.designerId) { ElMessage.warning('请选择设计师'); return }
  rateSubmitting.value = true
  try {
    const res = await financeApi.setCommissionRate(rateForm.designerId, rateForm.rate, rateForm.enabled)
    const result = res.data || {}
    ElMessage.success(`已设置，重算 ${result.created || 0} 笔新增、${result.updated || 0} 笔更新`)
    rateDialogVisible.value = false; loadData(); loadSummary(); loadOverview()
  } catch (e: any) { ElMessage.error(e.message || '设置失败') }
  finally { rateSubmitting.value = false }
}

async function recalcAll() {
  recalcLoading.value = true
  try {
    const res = await financeApi.recalcCommission()
    const result = res.data || {}
    ElMessage.success(`重算完成：${result.created || 0} 笔新增，${result.updated || 0} 笔更新`)
    loadData(); loadSummary(); loadOverview()
  } catch (e: any) { ElMessage.error(e.message || '重算失败') }
  finally { recalcLoading.value = false }
}

async function settleRow(row: any) {
  try { await financeApi.updateCommissionStatus(row.id, 2); ElMessage.success('已结算'); loadData(); loadSummary(); loadOverview() }
  catch (e: any) { ElMessage.error(e.message || '操作失败') }
}
async function payRow(row: any) {
  try { await financeApi.updateCommissionStatus(row.id, 3); ElMessage.success('已打款'); loadData(); loadSummary(); loadOverview() }
  catch (e: any) { ElMessage.error(e.message || '操作失败') }
}
async function deleteRow(row: any) {
  try { await financeApi.deleteCommission(row.id); ElMessage.success('已删除'); loadData(); loadSummary(); loadOverview() }
  catch (e: any) { ElMessage.error(e.message || '删除失败') }
}

// ── 批量操作 ──

async function batchSettle(row: any) {
  try {
    const res = await financeApi.batchSettleCommission(row.designerId)
    const r = res.data || {}
    ElMessage.success(`已结算 ${r.count} 笔，共 ¥${(r.totalAmount || 0).toFixed(2)}（已记入财务支出）`)
    loadData(); loadSummary(); loadOverview()
  } catch (e: any) { ElMessage.error(e.message || '批量结算失败') }
}

async function batchPay(row: any) {
  try {
    const res = await financeApi.batchPayCommission(row.designerId)
    const r = res.data || {}
    ElMessage.success(`已打款 ${r.count} 笔，共 ¥${(r.totalAmount || 0).toFixed(2)}`)
    loadData(); loadSummary(); loadOverview()
  } catch (e: any) { ElMessage.error(e.message || '批量打款失败') }
}

async function batchSettleCurrent() {
  if (!searchForm.designerId) return
  await batchSettle({ designerId: searchForm.designerId })
}

async function batchPayCurrent() {
  if (!searchForm.designerId) return
  await batchPay({ designerId: searchForm.designerId })
}

onMounted(() => { loadDesigners(); loadOverview(); loadSummary(); loadData() })
</script>

<style scoped lang="scss">
.commission-page {
  padding: 24px 32px;
  min-height: 100vh;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
}

/* ── 顶栏 ── */
.top-bar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 24px;
}
.top-left {
  display: flex; align-items: center; gap: 14px;
}
.page-icon {
  width: 42px; height: 42px; border-radius: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
}
.page-title {
  font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0;
  line-height: 1.3;
}
.page-sub {
  font-size: 13px; color: #8c8ca1; margin: 2px 0 0;
}
.top-right {
  display: flex; gap: 10px;
}

/* ── 通用按钮 ── */
.action-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px; border-radius: 8px; border: none;
  font-size: 13px; font-weight: 500; cursor: pointer;
  transition: all .2s; white-space: nowrap;
  svg { flex-shrink: 0; }
  &.primary { background: #667eea; color: #fff; &:hover { background: #5a6fd6; box-shadow: 0 4px 12px rgba(102,126,234,.3); } }
  &.secondary { background: #fff; color: #4a4a6a; border: 1px solid #e2e4ec; &:hover { border-color: #667eea; color: #667eea; box-shadow: 0 2px 8px rgba(0,0,0,.05); } }
  &.ghost { background: transparent; color: #8c8ca1; &:hover { color: #4a4a6a; background: #f5f5f9; } }
  &.accent { background: linear-gradient(135deg, #f093fb, #f5576c); color: #fff; &:hover { box-shadow: 0 4px 14px rgba(245,87,108,.35); } }
  &.ok { background: #27ae60; color: #fff; &:hover { background: #219a52; } }
  &.warn { background: #e67e22; color: #fff; &:hover { background: #d35400; } }
  &:disabled { opacity: .5; cursor: not-allowed; box-shadow: none !important; }
}
.spinning { animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg) } }

/* ── 统计卡片 ── */
.stats-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px;
}
.stat-card {
  display: flex; align-items: center; gap: 14px; padding: 18px 20px;
  border-radius: 14px; position: relative; overflow: hidden;
  transition: all .3s;
  &:hover { transform: translateY(-2px); }
  &.glass { background: rgba(255,255,255,.85); backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,.6); box-shadow: 0 2px 12px rgba(0,0,0,.04); }
}
.stat-icon {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  color: #fff; box-shadow: 0 4px 10px rgba(0,0,0,.1);
}
.pending .stat-icon { background: linear-gradient(135deg, #f6a623, #f5af19); }
.settled .stat-icon { background: linear-gradient(135deg, #36d1dc, #5b86e5); }
.paid .stat-icon { background: linear-gradient(135deg, #11998e, #38ef7d); }
.total .stat-icon { background: linear-gradient(135deg, #9d50bb, #6e48aa); }
.stat-body { flex: 1; min-width: 0; }
.stat-amount { font-size: 22px; font-weight: 700; color: #1a1a2e; line-height: 1.2; }
.stat-meta { font-size: 12px; color: #8c8ca1; margin-top: 2px; }
.stat-trend {
  padding: 2px 8px; border-radius: 6px; font-size: 12px; font-weight: 600;
  &.up { background: #e6f7ed; color: #11998e; }
}

/* ── 筛选栏 ── */
.filter-bar {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-radius: 12px; padding: 12px 16px;
  margin-bottom: 16px; border: 1px solid #eaecf2; gap: 12px;
}
.filter-group {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
}
.filter-item {
  display: flex; align-items: center; gap: 6px;
  label { font-size: 12px; color: #8c8ca1; white-space: nowrap; }
  select, input {
    padding: 6px 10px; border: 1px solid #e2e4ec; border-radius: 6px;
    font-size: 13px; color: #333; background: #fafbfc; outline: none;
    transition: border-color .2s; min-width: 100px;
    &:focus { border-color: #667eea; background: #fff; }
  }
}
.filter-actions { flex-shrink: 0; }

/* ── 表格 ── */
.table-wrap {
  background: #fff; border-radius: 14px; padding: 4px;
  border: 1px solid #eaecf2; box-shadow: 0 2px 12px rgba(0,0,0,.03);
}
.data-table {
  width: 100%; border-collapse: collapse; font-size: 13px;
  thead th {
    padding: 14px 14px; background: #f8f9fd; color: #6b6b83;
    font-weight: 600; font-size: 12px; text-transform: uppercase;
    letter-spacing: .3px; border-bottom: 1px solid #eaecf2;
    &:first-child { border-radius: 14px 0 0 0; }
    &:last-child { border-radius: 0 14px 0 0; }
  }
  tbody td {
    padding: 12px 14px; border-bottom: 1px solid #f0f1f5; color: #333;
    vertical-align: middle;
  }
  tbody tr:last-child td { border-bottom: none; }
  tbody tr:hover td { background: #f8f9ff; }
}
.name-tag { font-weight: 600; color: #333; }
.count-badge {
  display: inline-flex; align-items: center; justify-content: center;
  width: 24px; height: 24px; border-radius: 50%;
  background: #f0f2f5; font-size: 12px; font-weight: 600; color: #4a4a6a;
}
.mono { font-family: 'SF Mono', 'Fira Code', monospace; }
.small { font-size: 12px; color: #8c8ca1; }
.dash { color: #c0c4cc; }
.commission-amount { font-weight: 700; color: #11998e; &.zero { color: #c0c4cc; } }
.rate-badge {
  display: inline-block; padding: 2px 10px; border-radius: 10px;
  background: #f0f2f5; font-size: 12px; font-weight: 600; color: #4a4a6a;
}
.order-no { font-family: 'SF Mono', monospace; font-size: 12px; color: #667eea; letter-spacing: .3px; }

/* ── 状态标签 ── */
.status-tag {
  display: inline-block; padding: 3px 12px; border-radius: 20px;
  font-size: 12px; font-weight: 600;
  &.pending { background: #fff3e0; color: #e65100; }
  &.settled { background: #e3f2fd; color: #1565c0; }
  &.paid { background: #e8f5e9; color: #2e7d32; }
}
.tag {
  display: inline-block; padding: 2px 10px; border-radius: 10px;
  font-size: 12px; font-weight: 600;
  &.pending { background: #fff3e0; color: #e65100; }
  &.settled { background: #e3f2fd; color: #1565c0; }
  &.paid { background: #e8f5e9; color: #2e7d32; }
}

/* ── 表格按钮 ── */
.tbl-btn {
  border: none; background: transparent; font-size: 12px; font-weight: 500;
  cursor: pointer; padding: 3px 10px; border-radius: 6px; color: #667eea;
  transition: all .2s;
  &:hover { background: #eef0ff; }
  &.warn { color: #e67e22; &:hover { background: #fef3e7; } }
  &.danger { color: #e74c3c; &:hover { background: #fde8e8; } }
  &.ok { color: #27ae60; &:hover { background: #e8f8f0; } }
}
.empty-state { text-align: center; padding: 40px 0 !important; color: #c0c4cc; font-size: 14px; }

/* ── 批量操作栏 ── */
.batch-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid #eaecf2;
  font-size: 13px; color: #4a4a6a;
  strong { color: #667eea; }
}
.batch-actions {
  display: flex; gap: 8px;
}

/* ── 分页 ── */
.pagination-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-top: 1px solid #eaecf2;
}
.page-info { font-size: 12px; color: #8c8ca1; }
.page-controls {
  display: flex; align-items: center; gap: 4px;
  button {
    width: 32px; height: 32px; border: 1px solid #e2e4ec; border-radius: 6px;
    background: #fff; cursor: pointer; font-size: 16px; color: #4a4a6a; display: flex; align-items: center; justify-content: center;
    transition: all .2s; &:hover:not(:disabled) { border-color: #667eea; color: #667eea; }
    &:disabled { opacity: .3; cursor: not-allowed; }
  }
}
.page-num {
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  border-radius: 6px; cursor: pointer; font-size: 13px; color: #4a4a6a; font-weight: 500;
  transition: all .2s; user-select: none;
  &:hover { background: #f0f2f5; }
  &.active { background: #667eea; color: #fff; }
}
.page-size {
  padding: 6px 10px; border: 1px solid #e2e4ec; border-radius: 6px;
  font-size: 12px; color: #4a4a6a; background: #fafbfc; outline: none;
}

/* ── 弹窗 ── */
.modal-overlay {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0,0,0,.35); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
}
.modal-card {
  background: #fff; border-radius: 16px; width: 460px;
  box-shadow: 0 20px 60px rgba(0,0,0,.15);
  overflow: hidden; animation: slideUp .3s ease;
}
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 22px; border-bottom: 1px solid #eaecf2;
  h3 { margin: 0; font-size: 16px; font-weight: 700; color: #1a1a2e; }
}
.close-btn {
  width: 32px; height: 32px; border: none; background: #f0f2f5;
  border-radius: 8px; cursor: pointer; font-size: 18px; color: #8c8ca1;
  display: flex; align-items: center; justify-content: center;
  transition: all .2s; &:hover { background: #e5e7ef; color: #333; }
}
.modal-body { padding: 20px 22px; }
.form-group {
  margin-bottom: 16px;
  label { display: block; font-size: 13px; font-weight: 600; color: #4a4a6a; margin-bottom: 6px; }
}
.switch-row {
  display: flex; align-items: center; gap: 12px;
  label:first-child { margin-bottom: 0; }
}
.modal-select, .modal-input {
  width: 100%; padding: 9px 12px; border: 1px solid #e2e4ec; border-radius: 8px;
  font-size: 13px; color: #333; background: #fafbfc; outline: none;
  transition: border-color .2s; box-sizing: border-box;
  &:focus { border-color: #667eea; background: #fff; }
}
.rate-input-wrap {
  position: relative; width: 200px;
  .modal-input.rate { padding-right: 40px; }
  .rate-unit { position: absolute; right: 12px; top: 50%; transform: translateY(-50%); color: #8c8ca1; font-size: 14px; font-weight: 600; }
}
.switch {
  position: relative; width: 44px; height: 24px; cursor: pointer;
  input { display: none; }
  .slider {
    position: absolute; inset: 0; border-radius: 12px; background: #ddd; transition: all .3s;
    &::before { content: ''; position: absolute; width: 20px; height: 20px; left: 2px; top: 2px; border-radius: 50%; background: #fff; transition: all .3s; }
  }
  input:checked + .slider { background: #667eea; &::before { transform: translateX(20px); } }
}
.form-hint {
  display: flex; align-items: flex-start; gap: 8px;
  font-size: 12px; color: #8c8ca1; line-height: 1.5;
  padding: 10px 12px; background: #f8f9fd; border-radius: 8px;
  svg { flex-shrink: 0; margin-top: 2px; }
  strong { color: #e74c3c; }
}
.modal-footer {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 14px 22px; border-top: 1px solid #eaecf2;
}

/* ── 加载态 ── */
[v-loading] { position: relative; }
[v-loading]::after {
  content: ''; position: absolute; inset: 0; z-index: 10;
  background: rgba(255,255,255,.6); display: flex; align-items: center; justify-content: center;
}
</style>
