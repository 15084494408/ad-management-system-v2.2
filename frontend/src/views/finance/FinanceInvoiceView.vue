<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">🧾 发票管理</span>
      <div class="page-actions">
        <el-button @click="exportData"><el-icon><Download /></el-icon> 导出</el-button>
        <el-button type="primary" @click="showDialog()"><el-icon><Plus /></el-icon> 开具发票</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">🧾</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalCount }}</div>
          <div class="stat-label">发票总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ completedCount }}</div>
          <div class="stat-label">已开票</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">⏳</div>
        <div class="stat-info">
          <div class="stat-value">{{ pendingCount }}</div>
          <div class="stat-label">待开票</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">💰</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalAmount?.toLocaleString() }}</div>
          <div class="stat-label">开票总额</div>
        </div>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="发票编号">
          <el-input v-model="searchForm.invoiceNo" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="searchForm.customerName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="发票类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable style="width:140px;">
            <el-option label="增值税专票" value="special" />
            <el-option label="增值税普票" value="normal" />
            <el-option label="收据" value="receipt" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:100px;">
            <el-option label="已开具" value="completed" />
            <el-option label="待开具" value="pending" />
            <el-option label="已作废" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="invoiceNo" label="发票编号" width="150" />
        <el-table-column prop="customerName" label="客户名称" min-width="120" />
        <el-table-column prop="type" label="发票类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'special' ? 'primary' : row.type === 'normal' ? 'success' : 'info'">
              {{ { special:'增值税专票', normal:'增值税普票', receipt:'收据' }[row.type] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="发票金额" width="120">
          <template #default="{ row }">¥{{ row.amount?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="taxRate" label="税率" width="80">
          <template #default="{ row }">{{ row.taxRate }}%</template>
        </el-table-column>
        <el-table-column prop="taxAmount" label="税额" width="100">
          <template #default="{ row }">¥{{ row.taxAmount?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]" size="small">{{ { completed:'已开具', pending:'待开具', cancelled:'已作废' }[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="issueDate" label="开具日期" width="120" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewDetail(row)">查看</el-button>
            <el-button v-if="row.status === 'pending'" size="small" type="success" link @click="issueInvoice(row)">开具</el-button>
            <el-button v-if="row.status === 'completed'" size="small" type="danger" link @click="cancelInvoice(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑发票' : '开具发票'" width="500px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="客户名称"><el-input v-model="formData.customerName" placeholder="请输入" /></el-form-item>
        <el-form-item label="发票类型">
          <el-select v-model="formData.type" style="width:100%;">
            <el-option label="增值税专票" value="special" />
            <el-option label="增值税普票" value="normal" />
            <el-option label="收据" value="receipt" />
          </el-select>
        </el-form-item>
        <el-form-item label="发票金额"><el-input-number v-model="formData.amount" :min="0" :precision="2" style="width:100%;" /></el-form-item>
        <el-form-item label="税率"><el-input-number v-model="formData.taxRate" :min="0" :max="100" style="width:100%;" />%</el-form-item>
        <el-form-item label="备注"><el-input v-model="formData.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Download, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const totalCount = ref(0)
const completedCount = ref(0)
const pendingCount = ref(0)
const totalAmount = ref(0)
const tableData = ref<any[]>([])
const searchForm = ref({ invoiceNo: '', customerName: '', type: '', status: '' })
const formData = reactive({ customerName: '', type: 'normal', amount: 0, taxRate: 13, remark: '' })
const statusMap: Record<string, string> = { completed: 'success', pending: 'warning', cancelled: 'danger' }

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/finance/invoice/list', { params: searchForm.value })
    tableData.value = res.data?.records || res.data || []
  } catch {
    ElMessage.error('加载发票数据失败')
  }
  loading.value = false
}

function resetSearch() { searchForm.value = { invoiceNo: '', customerName: '', type: '', status: '' }; loadData() }
function exportData() { window.open('/api/finance/invoice/export', '_blank') }
function showDialog(row?: any) {
  isEdit.value = !!row
  if (row) Object.assign(formData, row)
  else Object.assign(formData, { customerName: '', type: 'normal', amount: 0, taxRate: 13, remark: '' })
  dialogVisible.value = true
}
function viewDetail(row: any) { ElMessage.info('查看发票: ' + row.invoiceNo) }
function issueInvoice(row: any) { ElMessage.success('发票已开具: ' + row.invoiceNo) }
function cancelInvoice(row: any) { ElMessage.warning('发票已作废: ' + row.invoiceNo) }
async function submitForm() {
  try { await request.post('/finance/invoice/save', formData); ElMessage.success('保存成功'); dialogVisible.value = false; loadData() } catch {}
}
onMounted(loadData)
</script>
