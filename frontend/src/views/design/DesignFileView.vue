<template>
  <div class="page-container">
    <div class="page-header"><span class="page-title">📁 设计文件</span><div class="page-actions"><el-button type="primary" @click="showUpload=true"><el-icon><Upload /></el-icon> 上传文件</el-button></div></div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="文件名"><el-input v-model="searchForm.name" placeholder="请输入" clearable /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px;">
            <el-option label="待审核" :value="1" />
            <el-option label="通过" :value="2" />
            <el-option label="驳回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">搜索</el-button><el-button @click="resetSearch">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="originalName" label="文件名" min-width="200" />
        <el-table-column prop="extension" label="类型" width="80">
          <template #default="{ row }"><el-tag size="small" type="info">{{ row.extension?.toUpperCase().replace('.','') }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="size" label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="70">
          <template #default="{ row }">V{{ row.version }}</template>
        </el-table-column>
        <el-table-column prop="uploaderName" label="上传人" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status===1?'info':row.status===2?'success':'danger'" size="small">
              {{ ['','待审核','通过','驳回'][row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewFile(row)">查看</el-button>
            <el-button link type="success" size="small" @click="viewVersions(row)">版本</el-button>
            <el-button link type="warning" size="small" @click="newVersion(row)">新版本</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" :total="pagination.total"
          layout="total, prev, pager, next" @size-change="loadData" @current-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="showUpload" title="上传文件" width="450px">
      <el-upload ref="uploadRef" class="upload-demo" drag action="/api/design/file/upload" multiple :auto-upload="false" :on-success="()=>{ElMessage.success('上传成功');showUpload=false;loadData()}">
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">拖拽文件或 <em>点击上传</em></div>
        <template #tip><div class="el-upload__tip">支持 AI、PSD、PDF、JPG、PNG 等格式，单文件最大 200MB</div></template>
      </el-upload>
      <el-form-item label="关联订单" style="margin-top:15px;">
        <el-select v-model="uploadForm.orderId" placeholder="可选" clearable style="width:100%;"><el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" /></el-select>
      </el-form-item>
      <el-form-item label="描述"><el-input v-model="uploadForm.description" type="textarea" rows="2" /></el-form-item>
      <template #footer><el-button @click="showUpload=false">取消</el-button><el-button type="primary" @click="submitUpload">上传</el-button></template>
    </el-dialog>

    <el-dialog v-model="showVersion" title="版本历史" width="500px">
      <el-timeline>
        <el-timeline-item v-for="v in versions" :key="v.id" :timestamp="v.createTime" placement="top">
          <el-card><p><strong>V{{ v.version }}</strong> - {{ v.changeDesc || '无说明' }}</p><p style="color:#909399;font-size:12px;">{{ formatSize(v.size) }} · {{ v.operatorName }}</p></el-card>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, UploadFilled } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const showUpload = ref(false)
const showVersion = ref(false)
const uploadRef = ref()
const tableData = ref<any[]>([])
const versions = ref<any[]>([])
const orders = ref<any[]>([])
const searchForm = reactive({ name: '', status: null })
const pagination = reactive({ page: 1, size: 20, total: 0 })
const uploadForm = reactive({ orderId: null, description: '' })

async function loadData() {
  loading.value = true
  try {
    const params: any = { current: pagination.page, size: pagination.size }
    if (searchForm.name) params.name = searchForm.name
    if (searchForm.status) params.status = searchForm.status
    const res = await request.get('/design/file', { params })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch { tableData.value = [] }
  loading.value = false
}
function resetSearch() { searchForm.name = ''; searchForm.status = null; loadData() }
function viewFile(row: any) { window.open(row.url || '#') }
async function viewVersions(row: any) { const res = await request.get(`/design/file/${row.id}/versions`); versions.value = res.data || []; showVersion.value = true }
function newVersion(row: any) { ElMessage.info('请使用上传功能上传新版本') }
async function submitUpload() { uploadRef.value?.submit() }
function formatSize(bytes: number) { if (!bytes) return '0 B'; const k = 1024; const sizes = ['B','KB','MB','GB']; const i = Math.floor(Math.log(bytes)/Math.log(k)); return parseFloat((bytes/Math.pow(k,i)).toFixed(2)) + ' ' + sizes[i]; }
onMounted(() => { loadData(); request.get('/orders', { params: { current: 1, size: 100 } }).then(r => orders.value = (r.data?.records || r.data || [])).catch(() => {}) })
</script>
