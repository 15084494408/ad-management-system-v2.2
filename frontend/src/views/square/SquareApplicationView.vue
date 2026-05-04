<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">📋 需求申请</span>
      <div class="page-actions">
        <el-button type="primary" @click="showDialog()"><el-icon><Plus /></el-icon> 提交申请</el-button>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="申请标题">
          <el-input v-model="searchForm.title" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="审批状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px;">
            <el-option label="待审批" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
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
        <el-table-column prop="applyNo" label="申请编号" width="140" />
        <el-table-column prop="title" label="申请标题" min-width="180" />
        <el-table-column prop="type" label="申请类型" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ { design:'设计需求', material:'物料采购', finance:'费用报销', other:'其他' }[row.type] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="amount" label="涉及金额" width="120">
          <template #default="{ row }">¥{{ row.amount?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]" size="small">{{ { pending:'待审批', approved:'已通过', rejected:'已驳回' }[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewDetail(row)">查看</el-button>
            <el-button v-if="row.status === 'pending'" size="small" type="success" link @click="approve(row)">通过</el-button>
            <el-button v-if="row.status === 'pending'" size="small" type="danger" link @click="reject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="提交申请" width="500px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="申请标题"><el-input v-model="formData.title" placeholder="请输入" /></el-form-item>
        <el-form-item label="申请类型">
          <el-select v-model="formData.type" style="width:100%;">
            <el-option label="设计需求" value="design" />
            <el-option label="物料采购" value="material" />
            <el-option label="费用报销" value="finance" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="涉及金额"><el-input-number v-model="formData.amount" :min="0" :precision="2" style="width:100%;" /></el-form-item>
        <el-form-item label="申请说明"><el-input v-model="formData.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const dialogVisible = ref(false)
const tableData = ref<any[]>([])
const searchForm = ref({ title: '', status: '' })
const formData = reactive({ title: '', type: 'design', amount: 0, description: '' })
const statusMap: Record<string, string> = { pending: 'warning', approved: 'success', rejected: 'danger' }

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/square/my-apply', { params: searchForm.value })
    tableData.value = res.data?.records || res.data || []
  } catch {}
  loading.value = false
}

function resetSearch() { searchForm.value = { title: '', status: '' }; loadData() }
function showDialog() { dialogVisible.value = true }
function viewDetail(row: any) { ElMessage.info('查看: ' + row.applyNo) }
function approve(row: any) { request.put('/square/apply/' + row.id + '/accept').then(() => { ElMessage.success('已通过'); loadData() }).catch(() => {}) }
function reject(row: any) { request.put('/square/apply/' + row.id + '/reject', null, { params: { reason: '驳回' } }).then(() => { ElMessage.warning('已驳回'); loadData() }).catch(() => {}) }
async function submitForm() {
  try { await request.post('/square/apply', formData); ElMessage.success('提交成功'); dialogVisible.value = false; loadData() } catch {}
}
onMounted(loadData)
</script>
