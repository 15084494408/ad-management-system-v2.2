<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">📤 设计上传</span>
      <div class="page-actions">
        <el-button type="primary" @click="showUploadDialog">
          <el-icon><Upload /></el-icon> 上传设计
        </el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">📁</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalFiles }}</div>
          <div class="stat-label">文件总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ approvedCount }}</div>
          <div class="stat-label">已审核</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">⏳</div>
        <div class="stat-info">
          <div class="stat-value">{{ pendingCount }}</div>
          <div class="stat-label">待审核</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">💾</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalSize }}</div>
          <div class="stat-label">占用空间</div>
        </div>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="文件名称">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="文件类型">
          <el-select v-model="searchForm.fileType" placeholder="全部" clearable style="width:120px;">
            <el-option label="PSD" value="psd" />
            <el-option label="AI" value="ai" />
            <el-option label="PDF" value="pdf" />
            <el-option label="CDR" value="cdr" />
            <el-option label="图片" value="image" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:100px;">
            <el-option label="待审核" value="pending" />
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
        <el-table-column prop="fileName" label="文件名" min-width="180" />
        <el-table-column prop="fileType" label="类型" width="80" />
        <el-table-column prop="fileSize" label="大小" width="100" />
        <el-table-column prop="orderId" label="关联订单" width="140" />
        <el-table-column prop="designerName" label="设计师" width="100" />
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]" size="small">{{ { pending:'待审核', approved:'已通过', rejected:'已驳回' }[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="previewFile(row)">预览</el-button>
            <el-button size="small" type="success" link @click="downloadFile(row)">下载</el-button>
            <el-button v-if="row.status === 'pending'" size="small" type="warning" link @click="reviewFile(row, 'approved')">通过</el-button>
            <el-button v-if="row.status === 'pending'" size="small" type="danger" link @click="reviewFile(row, 'rejected')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="uploadDialogVisible" title="上传设计文件" width="500px">
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="关联订单">
          <el-input v-model="uploadForm.orderId" placeholder="请输入订单编号" />
        </el-form-item>
        <el-form-item label="设计文件">
          <el-upload drag action="/api/design/files/upload" :on-success="onUploadSuccess" :auto-upload="false" ref="uploadRef">
            <el-icon style="font-size:40px;color:#c0c4cc;"><UploadFilled /></el-icon>
            <div style="color:#909399;">拖拽文件到此处或点击上传</div>
            <template #tip><div class="el-upload__tip">支持 PSD/AI/PDF/CDR/图片，单文件不超过 100MB</div></template>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="uploadForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUpload">确认上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Upload, UploadFilled } from '@element-plus/icons-vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const uploadDialogVisible = ref(false)
const totalFiles = ref(0)
const approvedCount = ref(0)
const pendingCount = ref(0)
const totalSize = ref('0 B')
const tableData = ref<any[]>([])
const searchForm = ref({ name: '', fileType: '', status: '' })
const uploadForm = reactive({ orderId: '', remark: '' })
const uploadRef = ref()
const statusMap: Record<string, string> = { pending: 'warning', approved: 'success', rejected: 'danger' }

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/design/file', { params: searchForm.value })
    tableData.value = res.data?.records || res.data || []
  } catch {
    ElMessage.error('加载设计文件失败')
  }
  loading.value = false
}

function resetSearch() { searchForm.value = { name: '', fileType: '', status: '' }; loadData() }
function showUploadDialog() { uploadDialogVisible.value = true }
function previewFile(row: any) { ElMessage.info('预览: ' + row.fileName) }
function downloadFile(row: any) { ElMessage.info('下载: ' + row.fileName) }
function reviewFile(row: any, status: string) {
  request.put(`/design/file/${row.id}/status`, null, { params: { status, remark: '' } }).then(() => { ElMessage.success('操作成功'); loadData() }).catch(() => {})
}
function onUploadSuccess() { ElMessage.success('上传成功'); uploadDialogVisible.value = false; loadData() }
function submitUpload() { uploadRef.value?.submit() }

onMounted(loadData)
</script>
