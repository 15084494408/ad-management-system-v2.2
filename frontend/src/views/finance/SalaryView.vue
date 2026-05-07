<template>
  <div class="salary-page">
    <!-- 顶栏 -->
    <div class="top-bar">
      <div class="top-left">
        <div class="page-icon">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
        </div>
        <div>
          <h1 class="page-title">工资管理</h1>
          <p class="page-sub">按月管理员工工资，自动汇总提成，发放后记入财务支出</p>
        </div>
      </div>
      <div class="top-right">
        <button class="action-btn accent" @click="showCreateDialog">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新建工资单
        </button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-group">
        <div class="filter-item">
          <label>月份</label>
          <input type="month" v-model="searchForm.month" @change="onSearch" class="month-input" />
        </div>
        <div class="filter-item">
          <label>员工</label>
          <select v-model="searchForm.employeeId" @change="onSearch">
            <option :value="null">全部员工</option>
            <option v-for="e in employeeList" :key="e.id" :value="e.id">{{ e.real_name || e.username }}</option>
          </select>
        </div>
        <div class="filter-item">
          <label>状态</label>
          <select v-model="searchForm.status" @change="onSearch">
            <option :value="null">全部状态</option>
            <option :value="1">待发放</option>
            <option :value="2">已发放</option>
          </select>
        </div>
        <button class="action-btn primary" @click="onSearch">搜索</button>
        <button class="action-btn ghost" @click="resetSearch">重置</button>
      </div>
      <div class="filter-actions">
        <button class="action-btn ok" v-if="searchForm.month" :disabled="batchPaying" @click="batchPay">
          批量发放 {{ searchForm.month }} 工资
        </button>
      </div>
    </div>

    <!-- 汇总 -->
    <div class="stats-row" v-if="tableData.length > 0">
      <div class="stat-card">
        <div class="stat-icon pending">📋</div>
        <div class="stat-body">
          <div class="stat-amount">{{ tableData.length }}</div>
          <div class="stat-meta">本月工资单数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon money">💰</div>
        <div class="stat-body">
          <div class="stat-amount">¥{{ totalPendingAmount.toFixed(2) }}</div>
          <div class="stat-meta">待发放总额</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon paid">✅</div>
        <div class="stat-body">
          <div class="stat-amount">¥{{ totalPaidAmount.toFixed(2) }}</div>
          <div class="stat-meta">已发放总额</div>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-wrap">
      <table class="data-table" v-loading="loading">
        <thead><tr>
          <th>员工</th><th>月份</th><th style="text-align:right">基本工资</th>
          <th style="text-align:right">提成</th><th style="text-align:right">奖金</th>
          <th style="text-align:right">扣款</th><th style="text-align:right">实发合计</th>
          <th style="text-align:center">状态</th><th>备注</th><th style="text-align:center">操作</th>
        </tr></thead>
        <tbody>
          <tr v-for="row in tableData" :key="row.id">
            <td><span class="name-tag">{{ row.employeeName }}</span></td>
            <td><span class="month-badge">{{ row.month }}</span></td>
            <td style="text-align:right" class="mono">¥{{ (row.baseSalary || 0).toFixed(2) }}</td>
            <td style="text-align:right"><span class="comm-amount">¥{{ (row.commissionAmount || 0).toFixed(2) }}</span></td>
            <td style="text-align:right" class="mono">¥{{ (row.bonus || 0).toFixed(2) }}</td>
            <td style="text-align:right;color:#e74c3c" class="mono">¥{{ (row.deduction || 0).toFixed(2) }}</td>
            <td style="text-align:right"><span class="total-amount">¥{{ (row.totalAmount || 0).toFixed(2) }}</span></td>
            <td style="text-align:center"><span class="status-tag" :class="row.status === 2 ? 'paid' : 'pending'">{{ row.status === 2 ? '已发放' : '待发放' }}</span></td>
            <td><span class="remark-text">{{ row.remark || '-' }}</span></td>
            <td style="text-align:center">
              <button v-if="row.status === 1" class="tbl-btn ok" @click="payRow(row)">发放</button>
              <button v-if="row.status === 1" class="tbl-btn" @click="editRow(row)">编辑</button>
              <button class="tbl-btn warn" @click="syncCommission(row)">同步提成</button>
              <button v-if="row.status === 1" class="tbl-btn danger" @click="deleteRow(row)">删除</button>
            </td>
          </tr>
          <tr v-if="tableData.length === 0"><td colspan="10" class="empty-state">暂无工资记录，点击「新建工资单」添加</td></tr>
        </tbody>
      </table>
      <div class="pagination-bar" v-if="pagination.total > 0">
        <div class="page-info">共 {{ pagination.total }} 条记录</div>
        <div class="page-controls">
          <button :disabled="pagination.current <= 1" @click="goPage(pagination.current - 1)">‹</button>
          <span class="page-num" v-for="p in pageNumbers" :key="p" :class="{ active: p === pagination.current }" @click="goPage(p)">{{ p }}</span>
          <button :disabled="pagination.current >= totalPages" @click="goPage(pagination.current + 1)">›</button>
        </div>
      </div>
    </div>

    <!-- 新建/编辑弹窗 -->
    <div class="modal-overlay" v-if="dialogVisible" @click.self="dialogVisible = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>{{ editId ? '编辑工资单' : '新建工资单' }}</h3>
          <button class="close-btn" @click="dialogVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group" style="flex:1">
              <label>员工 <span class="req">*</span></label>
              <select v-model="form.employeeId" class="modal-select" :disabled="!!editId">
                <option :value="null" disabled>请选择</option>
                <option v-for="e in employeeList" :key="e.id" :value="e.id">{{ e.real_name || e.username }}</option>
              </select>
            </div>
            <div class="form-group" style="flex:1">
              <label>月份 <span class="req">*</span></label>
              <input type="month" v-model="form.month" class="modal-input" :disabled="!!editId" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group" style="flex:1">
              <label>基本工资</label>
              <input type="number" v-model.number="form.baseSalary" min="0" step="0.01" class="modal-input" @input="calcTotal" />
            </div>
            <div class="form-group" style="flex:1">
              <label>提成（自动汇总）</label>
              <div class="comm-display">
                <span class="comm-value">¥{{ form.commissionAmount.toFixed(2) }}</span>
                <button class="tiny-btn" @click="fetchCommission" :title="'重新汇总' + form.month + '已结算提成'">↻</button>
              </div>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group" style="flex:1">
              <label>奖金</label>
              <input type="number" v-model.number="form.bonus" min="0" step="0.01" class="modal-input" @input="calcTotal" />
            </div>
            <div class="form-group" style="flex:1">
              <label>扣款</label>
              <input type="number" v-model.number="form.deduction" min="0" step="0.01" class="modal-input" @input="calcTotal" />
            </div>
          </div>
          <div class="total-preview">
            <span>实发合计</span>
            <span class="total-value">¥{{ form.totalAmount.toFixed(2) }}</span>
          </div>
          <div class="form-group">
            <label>备注</label>
            <input v-model="form.remark" placeholder="可选备注" class="modal-input" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="action-btn ghost" @click="dialogVisible = false">取消</button>
          <button class="action-btn primary" :disabled="submitting" @click="submitForm">{{ submitting ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const submitting = ref(false)
const batchPaying = ref(false)

const employeeList = ref<any[]>([])
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, size: 20, total: 0 })
const searchForm = reactive({ month: '', employeeId: null as number | null, status: null as number | null })

const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const form = reactive({
  employeeId: null as number | null,
  employeeName: '',
  month: '',
  baseSalary: 0,
  commissionAmount: 0,
  bonus: 0,
  deduction: 0,
  totalAmount: 0,
  remark: '',
})

const totalPages = computed(() => Math.ceil(pagination.total / pagination.size) || 1)
const pageNumbers = computed(() => {
  const tp = totalPages.value; const cur = pagination.current
  if (tp <= 7) return Array.from({ length: tp }, (_, i) => i + 1)
  if (cur <= 4) return [1, 2, 3, 4, 5, -1, tp]
  if (cur >= tp - 3) return [1, -1, tp - 4, tp - 3, tp - 2, tp - 1, tp]
  return [1, -1, cur - 1, cur, cur + 1, -1, tp]
})

const totalPendingAmount = computed(() =>
  tableData.value.filter(r => r.status === 1).reduce((s, r) => s + (r.totalAmount || 0), 0))
const totalPaidAmount = computed(() =>
  tableData.value.filter(r => r.status === 2).reduce((s, r) => s + (r.totalAmount || 0), 0))

function goPage(p: number) { if (p >= 1 && p <= totalPages.value) { pagination.current = p; loadData() } }
function onSearch() { pagination.current = 1; loadData() }
function resetSearch() { searchForm.month = ''; searchForm.employeeId = null; searchForm.status = null; pagination.current = 1; loadData() }

async function loadEmployees() {
  try { const res = await request.get('/finance/salary/employees'); employeeList.value = res.data || [] } catch {}
}
async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = { current: pagination.current, size: pagination.size }
    if (searchForm.month) params.month = searchForm.month
    if (searchForm.employeeId) params.employeeId = searchForm.employeeId
    if (searchForm.status) params.status = searchForm.status
    const res = await request.get('/finance/salary', { params })
    const p = res.data || {}
    tableData.value = p.records || []
    pagination.total = p.total || 0
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

function calcTotal() {
  form.totalAmount = Math.max(form.baseSalary + form.commissionAmount + form.bonus - form.deduction, 0)
}

async function fetchCommission() {
  if (!form.employeeId || !form.month) { ElMessage.warning('请先选择员工和月份'); return }
  try {
    const res = await request.get('/finance/salary/commission', {
      params: { employeeId: form.employeeId, month: form.month }
    })
    form.commissionAmount = res.data || 0
    calcTotal()
  } catch { ElMessage.error('汇总提成失败') }
}

async function showCreateDialog() {
  editId.value = null
  form.employeeId = null; form.employeeName = ''
  form.month = new Date().toISOString().slice(0, 7)
  form.baseSalary = 0; form.commissionAmount = 0; form.bonus = 0; form.deduction = 0; form.totalAmount = 0
  form.remark = ''
  dialogVisible.value = true
}

function editRow(row: any) {
  editId.value = row.id
  form.employeeId = row.employeeId; form.employeeName = row.employeeName
  form.month = row.month
  form.baseSalary = row.baseSalary || 0; form.commissionAmount = row.commissionAmount || 0
  form.bonus = row.bonus || 0; form.deduction = row.deduction || 0; form.totalAmount = row.totalAmount || 0
  form.remark = row.remark || ''
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.employeeId) { ElMessage.warning('请选择员工'); return }
  if (!form.month) { ElMessage.warning('请选择月份'); return }
  submitting.value = true
  try {
    const data = {
      employeeId: form.employeeId,
      employeeName: employeeList.value.find((e: any) => e.id === form.employeeId)?.real_name
                  || employeeList.value.find((e: any) => e.id === form.employeeId)?.username || '',
      month: form.month,
      baseSalary: form.baseSalary || 0,
      commissionAmount: form.commissionAmount || 0,
      bonus: form.bonus || 0,
      deduction: form.deduction || 0,
      totalAmount: form.totalAmount,
      remark: form.remark || null,
    }
    if (editId.value) {
      await request.put(`/finance/salary/${editId.value}`, data)
      ElMessage.success('更新成功')
    } else {
      await request.post('/finance/salary', data)
      ElMessage.success('新建成功')
    }
    dialogVisible.value = false; loadData()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
  finally { submitting.value = false }
}

async function payRow(row: any) {
  try {
    await request.put(`/finance/salary/${row.id}/pay`)
    ElMessage.success(`已发放 ${row.employeeName} ${row.month} 工资 ¥${(row.totalAmount || 0).toFixed(2)}（已记入财务支出）`)
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '发放失败') }
}

async function batchPay() {
  batchPaying.value = true
  try {
    const res = await request.post('/finance/salary/batch-pay', null, { params: { month: searchForm.month } })
    const r = res.data || {}
    ElMessage.success(`已批量发放 ${r.count} 笔，共 ¥${(r.totalAmount || 0).toFixed(2)}（已记入财务支出）`)
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '批量发放失败') }
  finally { batchPaying.value = false }
}

async function syncCommission(row: any) {
  try {
    const res = await request.post(`/finance/salary/${row.id}/sync-commission`)
    ElMessage.success(`提成已同步：¥${(res.data || 0).toFixed(2)}`)
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '同步失败') }
}

async function deleteRow(row: any) {
  try {
    await request.delete(`/finance/salary/${row.id}`)
    ElMessage.success('已删除'); loadData()
  } catch (e: any) { ElMessage.error(e.message || '删除失败') }
}

onMounted(() => { loadEmployees(); loadData() })
</script>

<style scoped lang="scss">
.salary-page {
  padding: 24px 32px; min-height: 100vh; background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
.top-bar {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px;
}
.top-left { display: flex; align-items: center; gap: 14px; }
.page-icon {
  width: 42px; height: 42px; border-radius: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(102,126,234,.35);
}
.page-title { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0; line-height: 1.3; }
.page-sub { font-size: 13px; color: #8c8ca1; margin: 2px 0 0; }
.top-right { display: flex; gap: 10px; }

.action-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px; border-radius: 8px; border: none;
  font-size: 13px; font-weight: 500; cursor: pointer;
  transition: all .2s; white-space: nowrap;
  svg { flex-shrink: 0; }
  &.primary { background: #667eea; color: #fff; &:hover { background: #5a6fd6; box-shadow: 0 4px 12px rgba(102,126,234,.3); } }
  &.ghost { background: transparent; color: #8c8ca1; &:hover { color: #4a4a6a; background: #f5f5f9; } }
  &.accent { background: linear-gradient(135deg, #f093fb, #f5576c); color: #fff; &:hover { box-shadow: 0 4px 14px rgba(245,87,108,.35); } }
  &.ok { background: #27ae60; color: #fff; &:hover { background: #219a52; } }
  &:disabled { opacity: .5; cursor: not-allowed; }
}

.filter-bar {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-radius: 12px; padding: 12px 16px;
  margin-bottom: 16px; border: 1px solid #eaecf2;
}
.filter-group { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.filter-item {
  display: flex; align-items: center; gap: 6px;
  label { font-size: 12px; color: #8c8ca1; white-space: nowrap; }
  select, input, .month-input {
    padding: 6px 10px; border: 1px solid #e2e4ec; border-radius: 6px;
    font-size: 13px; color: #333; background: #fafbfc; outline: none;
    transition: border-color .2s; min-width: 100px;
    &:focus { border-color: #667eea; background: #fff; }
  }
}
.filter-actions { flex-shrink: 0; }

.stats-row {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px;
}
.stat-card {
  display: flex; align-items: center; gap: 12px; padding: 14px 16px;
  background: #fff; border-radius: 12px; border: 1px solid #eaecf2;
}
.stat-icon {
  width: 40px; height: 40px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px;
  &.pending { background: #fff3e0; }
  &.money { background: #e3f2fd; }
  &.paid { background: #e8f5e9; }
}
.stat-body { flex: 1; }
.stat-amount { font-size: 20px; font-weight: 700; color: #1a1a2e; }
.stat-meta { font-size: 12px; color: #8c8ca1; margin-top: 2px; }

.table-wrap {
  background: #fff; border-radius: 14px; padding: 4px;
  border: 1px solid #eaecf2; box-shadow: 0 2px 12px rgba(0,0,0,.03);
}
.data-table {
  width: 100%; border-collapse: collapse; font-size: 13px;
  thead th {
    padding: 12px 14px; background: #f8f9fd; color: #6b6b83;
    font-weight: 600; font-size: 12px; letter-spacing: .3px;
    border-bottom: 1px solid #eaecf2;
    &:first-child { border-radius: 14px 0 0 0; }
    &:last-child { border-radius: 0 14px 0 0; }
  }
  tbody td { padding: 10px 14px; border-bottom: 1px solid #f0f1f5; color: #333; vertical-align: middle; }
  tbody tr:last-child td { border-bottom: none; }
  tbody tr:hover td { background: #f8f9ff; }
}
.name-tag { font-weight: 600; }
.month-badge {
  display: inline-block; padding: 2px 10px; border-radius: 8px;
  background: #f0f2f5; font-size: 12px; font-weight: 600; color: #4a4a6a;
  font-family: monospace;
}
.mono { font-family: 'SF Mono', 'Fira Code', monospace; font-size: 12px; }
.comm-amount { color: #667eea; font-weight: 600; }
.total-amount { font-weight: 700; color: #27ae60; }
.remark-text { font-size: 12px; color: #8c8ca1; max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: inline-block; }

.status-tag {
  display: inline-block; padding: 3px 12px; border-radius: 20px; font-size: 12px; font-weight: 600;
  &.pending { background: #fff3e0; color: #e65100; }
  &.paid { background: #e8f5e9; color: #2e7d32; }
}

.tbl-btn {
  border: none; background: transparent; font-size: 12px; font-weight: 500;
  cursor: pointer; padding: 3px 8px; border-radius: 6px; color: #667eea;
  transition: all .2s;
  &:hover { background: #eef0ff; }
  &.warn { color: #e67e22; &:hover { background: #fef3e7; } }
  &.danger { color: #e74c3c; &:hover { background: #fde8e8; } }
  &.ok { color: #27ae60; &:hover { background: #e8f8f0; } }
}
.empty-state { text-align: center; padding: 40px 0 !important; color: #c0c4cc; font-size: 14px; }

.pagination-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-top: 1px solid #eaecf2;
}
.page-info { font-size: 12px; color: #8c8ca1; }
.page-controls { display: flex; align-items: center; gap: 4px; button { width: 32px; height: 32px; border: 1px solid #e2e4ec; border-radius: 6px; background: #fff; cursor: pointer; font-size: 16px; color: #4a4a6a; &:disabled { opacity: .3; cursor: not-allowed; } &:hover:not(:disabled) { border-color: #667eea; color: #667eea; } } }
.page-num { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border-radius: 6px; cursor: pointer; font-size: 13px; color: #4a4a6a; &.active { background: #667eea; color: #fff; } }

/* ── 弹窗 ── */
.modal-overlay {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0,0,0,.35); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
}
.modal-card {
  background: #fff; border-radius: 16px; width: 560px;
  box-shadow: 0 20px 60px rgba(0,0,0,.15); animation: slideUp .3s ease;
}
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 22px; border-bottom: 1px solid #eaecf2; h3 { margin: 0; font-size: 16px; font-weight: 700; } }
.close-btn { width: 32px; height: 32px; border: none; background: #f0f2f5; border-radius: 8px; cursor: pointer; font-size: 18px; color: #8c8ca1; &:hover { background: #e5e7ef; } }
.modal-body { padding: 20px 22px; }
.form-row { display: flex; gap: 14px; margin-bottom: 12px; }
.form-group { margin-bottom: 12px; label { display: block; font-size: 13px; font-weight: 600; color: #4a4a6a; margin-bottom: 5px; } }
.req { color: #e74c3c; }
.modal-select, .modal-input {
  width: 100%; padding: 9px 12px; border: 1px solid #e2e4ec; border-radius: 8px;
  font-size: 13px; color: #333; background: #fafbfc; outline: none; transition: border-color .2s; box-sizing: border-box;
  &:focus { border-color: #667eea; background: #fff; }
}
.comm-display { display: flex; align-items: center; gap: 8px; }
.comm-value { font-size: 16px; font-weight: 700; color: #667eea; }
.tiny-btn { width: 28px; height: 28px; border: 1px solid #e2e4ec; border-radius: 6px; background: #fff; cursor: pointer; font-size: 14px; color: #667eea; &:hover { border-color: #667eea; } }
.total-preview {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; background: #f8f9fd; border-radius: 10px; margin-bottom: 12px;
  font-size: 14px; font-weight: 600; color: #4a4a6a;
}
.total-value { font-size: 20px; font-weight: 700; color: #27ae60; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 22px; border-top: 1px solid #eaecf2; }
</style>
