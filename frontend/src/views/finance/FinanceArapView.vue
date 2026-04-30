<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">📊 应收应付</span>
      <div class="page-actions">
        <el-button @click="exportData"><el-icon><Download /></el-icon> 导出</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">💵</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ receivableTotal?.toLocaleString() }}</div>
          <div class="stat-label">应收总额</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">✅</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ receivedTotal?.toLocaleString() }}</div>
          <div class="stat-label">已收总额</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">⏳</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ payableTotal?.toLocaleString() }}</div>
          <div class="stat-label">应付总额</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">🔥</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ overdueTotal?.toLocaleString() }}</div>
          <div class="stat-label">逾期金额</div>
        </div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="card">
      <el-tab-pane label="应收明细" name="receivable">
        <div class="search-form">
          <el-form :inline="true" :model="searchForm">
            <el-form-item label="客户名称">
              <el-input v-model="searchForm.customerName" placeholder="请输入" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px;">
                <el-option label="未收款" value="pending" />
                <el-option label="部分收款" value="partial" />
                <el-option label="已收清" value="completed" />
                <el-option label="已逾期" value="overdue" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadData">搜索</el-button>
              <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <el-table :data="receivableList" stripe v-loading="loading">
          <el-table-column prop="orderNo" label="订单编号" width="150" />
          <el-table-column prop="customerName" label="客户名称" min-width="120" />
          <el-table-column prop="totalAmount" label="应收金额" width="120">
            <template #default="{ row }">¥{{ row.totalAmount?.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="receivedAmount" label="已收金额" width="120">
            <template #default="{ row }">¥{{ row.receivedAmount?.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="remainingAmount" label="待收金额" width="120">
            <template #default="{ row }"><span style="color:#f56c6c;font-weight:600;">¥{{ row.remainingAmount?.toLocaleString() }}</span></template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deadline" label="截止日期" width="120" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="应付明细" name="payable">
        <div class="search-form">
          <el-form :inline="true" :model="searchForm">
            <el-form-item label="供应商">
              <el-input v-model="searchForm.supplier" placeholder="请输入" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadData">搜索</el-button>
              <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <el-table :data="payableList" stripe v-loading="loading">
          <el-table-column prop="billNo" label="账单编号" width="150" />
          <el-table-column prop="supplierName" label="供应商" min-width="120" />
          <el-table-column prop="totalAmount" label="应付金额" width="120">
            <template #default="{ row }">¥{{ row.totalAmount?.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="paidAmount" label="已付金额" width="120">
            <template #default="{ row }">¥{{ row.paidAmount?.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="remainingAmount" label="待付金额" width="120">
            <template #default="{ row }"><span style="color:#e6a23c;font-weight:600;">¥{{ row.remainingAmount?.toLocaleString() }}</span></template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'completed' ? 'success' : row.status === 'overdue' ? 'danger' : 'warning'" size="small">{{ { pending:'未付款', partial:'部分付', completed:'已付清', overdue:'已逾期' }[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deadline" label="截止日期" width="120" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import request from '@/api/request'
import { exportToExcel } from '@/utils/excelExport'

const activeTab = ref('receivable')
const loading = ref(false)
const receivableTotal = ref(0)
const receivedTotal = ref(0)
const payableTotal = ref(0)
const overdueTotal = ref(0)
const receivableList = ref<any[]>([])
const payableList = ref<any[]>([])
const searchForm = ref({ customerName: '', status: '', supplier: '' })

const statusMap: Record<string, { label: string; type: string }> = {
  pending: { label: '未收款', type: 'info' },
  partial: { label: '部分收款', type: 'warning' },
  completed: { label: '已收清', type: 'success' },
  overdue: { label: '已逾期', type: 'danger' },
}

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/finance/arap/summary')
    const d = res.data || {}
    receivableTotal.value = d.receivableTotal || 0
    receivedTotal.value = d.receivedTotal || 0
    payableTotal.value = d.payableTotal || 0
    overdueTotal.value = d.overdueTotal || 0
  } catch {}
  try {
    const res = await request.get('/finance/arap/receivable', { params: searchForm.value })
    const rawList = res.data?.records || res.data || []
    // ★ 修复：后端返回 Order 实体，字段为 totalAmount/paidAmount，
    // 需映射为前端使用的 receivedAmount/remainingAmount，并计算 status
    receivableList.value = rawList.map((o: any) => {
      const total = o.totalAmount || 0
      const paid = o.paidAmount || 0
      const rounding = o.roundingAmount || 0
      const remaining = Math.max(total + rounding - paid, 0)
      let status = 'pending'
      if (paid <= 0) status = 'pending'
      else if (paid >= total + rounding) status = 'completed'
      else status = 'partial'
      return {
        ...o,
        totalAmount: total,
        receivedAmount: paid,
        remainingAmount: remaining,
        status
      }
    })
  } catch {}
  try {
    const res = await request.get('/finance/arap/payable', { params: searchForm.value })
    const rawList = res.data?.records || res.data || []
    // ★ 修复：应付明细同理映射字段
    payableList.value = rawList.map((b: any) => {
      const total = b.totalAmount || 0
      const paid = b.paidAmount || 0
      const remaining = Math.max(total - paid, 0)
      let status = 'pending'
      if (paid <= 0) status = 'pending'
      else if (paid >= total) status = 'completed'
      else status = 'partial'
      return {
        ...b,
        totalAmount: total,
        paidAmount: paid,
        remainingAmount: remaining,
        status
      }
    })
  } catch {}
  loading.value = false
}

function resetSearch() { searchForm.value = { customerName: '', status: '', supplier: '' }; loadData() }
function exportData() {
  const isReceivable = activeTab.value === 'receivable'
  const list = isReceivable ? receivableList.value : payableList.value
  const header = isReceivable
    ? ['订单编号', '客户名称', '应收金额', '已收金额', '待收金额', '状态', '截止日期']
    : ['账单编号', '供应商', '应付金额', '已付金额', '待付金额', '状态', '截止日期']
  const data = list.map(row => {
    if (isReceivable) {
      return [row.orderNo, row.customerName, row.totalAmount, row.receivedAmount, row.remainingAmount, statusMap[row.status]?.label || row.status, row.deadline]
    }
    return [row.billNo, row.supplierName, row.totalAmount, row.paidAmount, row.remainingAmount, { pending:'未付款', partial:'部分付', completed:'已付清', overdue:'已逾期' }[row.status] || row.status, row.deadline]
  })
  exportToExcel({
    filename: '应收应付明细',
    header,
    data,
    title: (isReceivable ? '应收' : '应付') + '明细表',
    infoRows: [['导出时间：', new Date().toLocaleString()]],
  })
}

onMounted(loadData)
</script>
