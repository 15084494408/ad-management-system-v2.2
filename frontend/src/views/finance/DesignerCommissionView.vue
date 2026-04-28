<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">🎨 设计提成</span>
      <div class="page-actions">
        <el-button @click="switchView">
          {{ viewMode === 'detail' ? '📊 汇总视图' : '📋 明细视图' }}
        </el-button>
        <el-button @click="exportData"><el-icon><Download /></el-icon> 导出</el-button>
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon> 新增提成
        </el-button>
      </div>
    </div>

    <!-- 总览卡片 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon orange">⏳</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ overview.pendingAmount?.toFixed(2) }}</div>
          <div class="stat-label">待结算（{{ overview.pendingCount }}笔）</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon blue">✅</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ overview.settledAmount?.toFixed(2) }}</div>
          <div class="stat-label">已结算（{{ overview.settledCount }}笔）</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">💰</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ overview.paidAmount?.toFixed(2) }}</div>
          <div class="stat-label">已打款（{{ overview.paidCount }}笔）</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">📊</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ overview.totalAmount?.toFixed(2) }}</div>
          <div class="stat-label">累计提成（{{ overview.totalCount }}笔）</div>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="设计师">
          <el-select v-model="searchForm.designerId" placeholder="全部" clearable filterable style="width:150px">
            <el-option v-for="d in designerSummary" :key="d.designerId" :label="d.designerName" :value="d.designerId" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="待结算" :value="1" />
            <el-option label="已结算" :value="2" />
            <el-option label="已打款" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索">
          <el-input v-model="searchForm.keyword" placeholder="订单号/设计师/备注" clearable style="width:200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 汇总视图 -->
    <div v-if="viewMode === 'summary'" class="card">
      <el-table :data="designerSummary" v-loading="summaryLoading" stripe>
        <el-table-column prop="designerName" label="设计师" min-width="120" />
        <el-table-column prop="count" label="订单数" width="80" align="center" />
        <el-table-column prop="totalBase" label="计算基数" width="120" align="right">
          <template #default="{ row }">¥{{ row.totalBase?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="totalCommission" label="总提成" width="120" align="right">
          <template #default="{ row }">
            <span style="color:#67c23a;font-weight:600;">¥{{ row.totalCommission?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="待结算" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.pendingCount > 0" type="warning" size="small">{{ row.pendingCount }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="已结算" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.settledCount > 0" type="primary" size="small">{{ row.settledCount }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="已打款" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.paidCount > 0" type="success" size="small">{{ row.paidCount }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="searchByDesigner(row)">查看明细</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 明细视图 -->
    <div v-else class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="designerName" label="设计师" width="100" />
        <el-table-column prop="orderNo" label="订单编号" width="160">
          <template #default="{ row }">
            <span v-if="row.orderNo" class="order-link">{{ row.orderNo }}</span>
            <span v-else class="no-data">无关联订单</span>
          </template>
        </el-table-column>
        <el-table-column prop="baseAmount" label="计算基数" width="120" align="right">
          <template #default="{ row }">¥{{ row.baseAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="commissionRate" label="提成比例" width="90" align="center">
          <template #default="{ row }">{{ row.commissionRate }}%</template>
        </el-table-column>
        <el-table-column prop="commissionAmount" label="提成金额" width="120" align="right">
          <template #default="{ row }">
            <span style="color:#67c23a;font-weight:600;">¥{{ row.commissionAmount?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editRow(row)">编辑</el-button>
            <el-button v-if="row.status === 1" link type="success" @click="settleRow(row)">结算</el-button>
            <el-button v-if="row.status === 2" link type="warning" @click="payRow(row)">打款</el-button>
            <el-popconfirm title="确定删除？" @confirm="deleteRow(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑提成' : '新增提成'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="设计师" required>
          <el-input v-model="form.designerName" placeholder="设计师姓名" />
        </el-form-item>
        <el-form-item label="订单编号">
          <el-input v-model="form.orderNo" placeholder="关联订单编号（可选）" />
        </el-form-item>
        <el-form-item label="计算基数" required>
          <el-input-number v-model="form.baseAmount" :min="0" :precision="2" :controls="false" style="width:100%" placeholder="订单金额" />
        </el-form-item>
        <el-form-item label="提成比例" required>
          <el-input-number v-model="form.commissionRate" :min="0" :max="100" :precision="2" :step="0.5" style="width:200px" />
          <span style="margin-left:8px;color:#909399;">%</span>
        </el-form-item>
        <el-form-item label="提成金额">
          <div class="computed-amount">
            <span>¥{{ computedAmount }}</span>
            <span class="computed-hint" v-if="form.baseAmount > 0 && form.commissionRate > 0">自动计算 = 基数 × 比例</span>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Plus } from '@element-plus/icons-vue'
import { financeApi } from '@/api'
import { exportToExcel } from '@/utils/excelExport'

const viewMode = ref<'detail' | 'summary'>('summary')
const loading = ref(false)
const summaryLoading = ref(false)

// 总览数据
const overview = reactive({
  pendingAmount: 0, pendingCount: 0,
  settledAmount: 0, settledCount: 0,
  paidAmount: 0, paidCount: 0,
  totalAmount: 0, totalCount: 0,
})

// 汇总数据
const designerSummary = ref<any[]>([])

// 明细数据
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, size: 10, total: 0 })
const searchForm = reactive({
  designerId: null as number | null,
  status: null as number | null,
  keyword: '',
})

// 弹窗
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const submitting = ref(false)
const form = reactive({
  designerName: '',
  orderNo: '',
  baseAmount: 0,
  commissionRate: 5,
  commissionAmount: 0,
  remark: '',
})

const computedAmount = computed(() => {
  if (form.baseAmount > 0 && form.commissionRate > 0) {
    return (form.baseAmount * form.commissionRate / 100).toFixed(2)
  }
  return '0.00'
})

function statusText(s: number) {
  return { 1: '待结算', 2: '已结算', 3: '已打款' }[s] || '未知'
}
function statusType(s: number) {
  return { 1: 'warning', 2: 'primary', 3: 'success' }[s] || 'info'
}

function switchView() {
  viewMode.value = viewMode.value === 'detail' ? 'summary' : 'detail'
  if (viewMode.value === 'detail') loadData()
  else loadSummary()
}

async function loadOverview() {
  try {
    const res = await financeApi.getCommissionOverview()
    Object.assign(overview, res.data || {})
  } catch { /* silent */ }
}

async function loadSummary() {
  summaryLoading.value = true
  try {
    const res = await financeApi.getCommissionSummary(searchForm.status ? { status: searchForm.status } : undefined)
    designerSummary.value = res.data || []
  } catch {
    designerSummary.value = []
  } finally {
    summaryLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await financeApi.getCommissionList({
      current: pagination.current,
      size: pagination.size,
      ...searchForm,
    })
    const page = res.data || {}
    tableData.value = page.records || []
    pagination.total = page.total || 0
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function searchByDesigner(d: any) {
  searchForm.designerId = d.designerId
  viewMode.value = 'detail'
  pagination.current = 1
  loadData()
}

function resetSearch() {
  searchForm.designerId = null
  searchForm.status = null
  searchForm.keyword = ''
  pagination.current = 1
  if (viewMode.value === 'detail') loadData()
  else loadSummary()
}

function showAddDialog() {
  editId.value = null
  form.designerName = ''
  form.orderNo = ''
  form.baseAmount = 0
  form.commissionRate = 5
  form.commissionAmount = 0
  form.remark = ''
  dialogVisible.value = true
}

function editRow(row: any) {
  editId.value = row.id
  form.designerName = row.designerName
  form.orderNo = row.orderNo || ''
  form.baseAmount = row.baseAmount
  form.commissionRate = row.commissionRate
  form.commissionAmount = row.commissionAmount
  form.remark = row.remark || ''
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.designerName.trim()) {
    ElMessage.warning('请输入设计师姓名')
    return
  }
  if (form.baseAmount <= 0) {
    ElMessage.warning('计算基数必须大于0')
    return
  }
  submitting.value = true
  try {
    const data = {
      designerName: form.designerName,
      orderNo: form.orderNo || null,
      baseAmount: form.baseAmount,
      commissionRate: form.commissionRate,
      commissionAmount: parseFloat(computedAmount.value),
      remark: form.remark || null,
    }
    if (editId.value) {
      await financeApi.updateCommission(editId.value, data)
      ElMessage.success('更新成功')
    } else {
      await financeApi.createCommission(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
    loadSummary()
    loadOverview()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function settleRow(row: any) {
  try {
    await financeApi.updateCommissionStatus(row.id, 2)
    ElMessage.success('已结算')
    loadData()
    loadSummary()
    loadOverview()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function payRow(row: any) {
  try {
    await financeApi.updateCommissionStatus(row.id, 3)
    ElMessage.success('已打款')
    loadData()
    loadSummary()
    loadOverview()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function deleteRow(row: any) {
  try {
    await financeApi.deleteCommission(row.id)
    ElMessage.success('已删除')
    loadData()
    loadSummary()
    loadOverview()
  } catch (e: any) {
    ElMessage.error(e.message || '删除失败')
  }
}

function exportData() {
  if (viewMode.value === 'summary') {
    exportToExcel({
      filename: '设计提成汇总',
      header: ['设计师', '订单数', '计算基数', '总提成', '待结算', '已结算', '已打款'],
      data: designerSummary.value.map(d => [
        d.designerName, d.count,
        `¥${(d.totalBase || 0).toFixed(2)}`, `¥${(d.totalCommission || 0).toFixed(2)}`,
        d.pendingCount || '-', d.settledCount || '-', d.paidCount || '-',
      ]),
      infoRows: [[`导出时间：${new Date().toLocaleString()}`], [`共 ${designerSummary.value.length} 位设计师`]],
    })
  } else {
    exportToExcel({
      filename: '设计提成明细',
      header: ['设计师', '订单编号', '计算基数', '提成比例', '提成金额', '状态', '备注', '创建时间'],
      data: tableData.value.map(r => [
        r.designerName, r.orderNo || '-',
        `¥${(r.baseAmount || 0).toFixed(2)}`, `${r.commissionRate}%`,
        `¥${(r.commissionAmount || 0).toFixed(2)}`,
        statusText(r.status), r.remark || '-',
        (r.createTime || '').toString().slice(0, 16),
      ]),
      infoRows: [[`导出时间：${new Date().toLocaleString()}`], [`共 ${tableData.value.length} 条记录`]],
    })
  }
}

onMounted(() => {
  loadOverview()
  loadSummary()
  loadData()
})
</script>

<style scoped lang="scss">
.page-container {
  padding: 20px;
}
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
}
.page-title {
  font-size: 20px; font-weight: 700; color: #303133;
}
.page-actions {
  display: flex; gap: 10px;
}

.stat-grid {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 16px; margin-bottom: 20px;
}
.stat-card {
  background: #fff; border-radius: 12px; padding: 18px 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
  border: 1px solid rgba(0,0,0,0.04);
  transition: all 0.3s;
  &:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,0.1); }
}
.stat-icon {
  width: 48px; height: 48px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
  &.orange { background: linear-gradient(135deg, #e6a23c, #ebb563); }
  &.blue { background: linear-gradient(135deg, #409eff, #66b1ff); }
  &.green { background: linear-gradient(135deg, #67c23a, #85ce61); }
  &.purple { background: linear-gradient(135deg, #9b59b6, #c39bd3); }
}
.stat-value {
  font-size: 22px; font-weight: 700; color: #303133;
}
.stat-label {
  font-size: 12px; color: #909399; margin-top: 4px;
}

.search-form {
  background: #fff; border-radius: 12px; padding: 16px 20px;
  margin-bottom: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.06);
  border: 1px solid rgba(0,0,0,0.04);
}

.card {
  background: #fff; border-radius: 12px; padding: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
  border: 1px solid rgba(0,0,0,0.04);
}

.pagination-wrap {
  display: flex; justify-content: flex-end; margin-top: 16px;
}

.order-link {
  color: #409eff; cursor: pointer;
  &:hover { text-decoration: underline; }
}
.no-data {
  color: #c0c4cc;
}

.computed-amount {
  font-size: 24px; font-weight: 700; color: #67c23a;
}
.computed-hint {
  display: block; font-size: 11px; color: #909399;
  font-weight: normal; margin-top: 4px;
}
</style>
