<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">📝 报价管理</span>
      <div class="page-actions">
        <el-button @click="exportData"><el-icon><Download /></el-icon> 导出</el-button>
        <el-button type="primary" @click="showDialog()"><el-icon><Plus /></el-icon> 新建报价</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">📝</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalCount }}</div>
          <div class="stat-label">报价总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ acceptedCount }}</div>
          <div class="stat-label">已采纳</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">⏳</div>
        <div class="stat-info">
          <div class="stat-value">{{ pendingCount }}</div>
          <div class="stat-label">待确认</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">💰</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalAmount?.toLocaleString() }}</div>
          <div class="stat-label">报价总额</div>
        </div>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="客户名称">
          <el-input v-model="searchForm.customerName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="报价状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px;">
            <el-option label="待确认" value="pending" />
            <el-option label="已采纳" value="accepted" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="已过期" value="expired" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="quoteNo" label="报价编号" width="140" />
        <el-table-column prop="customerName" label="客户名称" min-width="120" />
        <el-table-column prop="projectName" label="项目名称" min-width="150" />
        <el-table-column prop="totalAmount" label="报价金额" width="120">
          <template #default="{ row }">¥{{ row.totalAmount?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="discount" label="折扣" width="80">
          <template #default="{ row }">{{ row.discount }}%</template>
        </el-table-column>
        <el-table-column prop="finalAmount" label="最终金额" width="120">
          <template #default="{ row }"><span style="color:#409eff;font-weight:600;">¥{{ row.finalAmount?.toLocaleString() }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]" size="small">{{ { pending:'待确认', accepted:'已采纳', rejected:'已拒绝', expired:'已过期' }[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="validUntil" label="有效期至" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewDetail(row)">查看</el-button>
            <el-button size="small" type="warning" link @click="showDialog(row)">编辑</el-button>
            <el-button v-if="row.status === 'pending'" size="small" type="success" link @click="convertToOrder(row)">转订单</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑报价' : '新建报价'" width="600px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="客户名称"><el-input v-model="formData.customerName" placeholder="请输入" /></el-form-item>
        <el-form-item label="项目名称"><el-input v-model="formData.projectName" placeholder="请输入" /></el-form-item>
        <el-form-item label="报价金额"><el-input-number v-model="formData.totalAmount" :min="0" :precision="2" style="width:100%;" /></el-form-item>
        <el-form-item label="折扣(%)"><el-input-number v-model="formData.discount" :min="0" :max="100" style="width:100%;" /></el-form-item>
        <el-form-item label="有效期至"><el-date-picker v-model="formData.validUntil" type="date" value-format="YYYY-MM-DD" style="width:100%;" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="formData.remark" type="textarea" :rows="3" /></el-form-item>
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
const acceptedCount = ref(0)
const pendingCount = ref(0)
const totalAmount = ref(0)
const tableData = ref<any[]>([])
const searchForm = ref({ customerName: '', status: '', dateRange: [] })
const formData = reactive({ customerName: '', projectName: '', totalAmount: 0, discount: 100, validUntil: '', remark: '' })
const statusMap: Record<string, string> = { pending: 'warning', accepted: 'success', rejected: 'danger', expired: 'info' }

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/finance/quote/list', { params: searchForm.value })
    tableData.value = res.data?.records || res.data || []
  } catch {
    ElMessage.error('加载报价数据失败')
  }
  loading.value = false
}

function resetSearch() { searchForm.value = { customerName: '', status: '', dateRange: [] }; loadData() }
function exportData() { window.open('/api/finance/quote/export', '_blank') }
function showDialog(row?: any) {
  isEdit.value = !!row
  if (row) Object.assign(formData, row)
  else Object.assign(formData, { customerName: '', projectName: '', totalAmount: 0, discount: 100, validUntil: '', remark: '' })
  dialogVisible.value = true
}
function viewDetail(row: any) { ElMessage.info('查看报价: ' + row.quoteNo) }
function convertToOrder(row: any) { ElMessage.success('已转为订单: ' + row.quoteNo) }
async function submitForm() {
  try { await request.post('/finance/quote/save', formData); ElMessage.success('保存成功'); dialogVisible.value = false; loadData() } catch {}
}
onMounted(loadData)
</script>
