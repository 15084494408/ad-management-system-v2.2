<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">📁 设计文件</span>
      <div class="page-actions">
        <el-button type="primary" @click="showUpload=true"><el-icon><Upload /></el-icon> 上传文件</el-button>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="文件名"><el-input v-model="searchForm.name" placeholder="请输入" clearable /></el-form-item>
        <el-form-item label="订单ID">
          <el-input v-model="searchForm.orderId" placeholder="输入订单号" clearable style="width:150px;" type="number" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px;">
            <el-option label="待审核" :value="1" />
            <el-option label="通过" :value="2" />
            <el-option label="驳回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="extension" label="类型" width="80">
          <template #default="{ row }"><el-tag size="small" type="info">{{ formatExt(row.extension) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="size" label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column label="关联订单" width="160">
          <template #default="{ row }">
            <el-tag v-if="row.orderId" size="small" type="primary" style="cursor:pointer;" @click="goToOrder(row.orderId)">
              订单 #{{ row.orderId }}
            </el-tag>
            <span v-else style="color:#c0c4cc;">未关联</span>
          </template>
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
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewFile(row)">查看</el-button>
            <el-button link type="success" size="small" @click="viewVersions(row)">版本</el-button>
            <el-button link type="danger" size="small" @click="deleteFile(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" :total="pagination.total"
          layout="total, prev, pager, next" @size-change="loadData" @current-change="loadData" />
      </div>
    </div>

    <!-- 上传文件弹窗 -->
    <el-dialog v-model="showUpload" title="上传文件" width="500px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="文件" required>
          <el-upload ref="uploadRef" class="upload-demo" drag multiple :auto-upload="false"
            :on-change="onFileChange" :on-remove="onFileRemove" :file-list="fileList">
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">拖拽文件或 <em>点击上传</em></div>
            <template #tip><div class="el-upload__tip">支持 CDR、PSD、AI、PDF、PNG、JPG 等格式</div></template>
          </el-upload>
        </el-form-item>
        <el-form-item label="关联订单" required>
          <el-select v-model="uploadForm.orderId" placeholder="请选择订单" style="width:100%;">
            <el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="uploadForm.description" type="textarea" rows="2" placeholder="选填，文件说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload=false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 版本历史弹窗 -->
    <el-dialog v-model="showVersion" title="版本历史" width="500px">
      <el-timeline>
        <el-timeline-item v-for="v in versions" :key="v.id" :timestamp="v.createTime" placement="top">
          <el-card>
            <p><strong>V{{ v.version }}</strong> - {{ v.changeDesc || '无说明' }}</p>
            <p style="color:#909399;font-size:12px;">{{ formatSize(v.size) }} · {{ v.operatorName }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>

    <!-- 文件预览弹窗 -->
    <el-dialog v-model="showPreview" :title="previewData?.originalName || '文件预览'" width="650px" top="5vh">
      <div v-if="previewData" class="file-preview-body">
        <!-- 图片预览 -->
        <div v-if="isImageFile(previewData.extension)" class="preview-image-wrap">
          <img :src="getDownloadUrl(previewData)" class="preview-image" alt="预览" @error="previewImgError=true" />
          <div v-if="previewImgError" class="preview-fallback">
            <span class="fallback-icon">🖼</span>
            <p>图片加载失败，请尝试下载查看</p>
          </div>
        </div>
        <!-- 非图片预览：文件信息卡片 -->
        <div v-else class="preview-file-info">
          <div class="preview-file-icon">
            <span class="pf-icon">{{ getFileIcon(previewData.extension) }}</span>
            <span class="pf-ext">{{ formatExt(previewData.extension) }}</span>
          </div>
          <div class="preview-file-details">
            <div class="pf-row"><span class="pf-label">文件名</span><span class="pf-value">{{ previewData.originalName || previewData.name }}</span></div>
            <div class="pf-row"><span class="pf-label">类型</span><span class="pf-value">{{ getFileTypeName(previewData.extension) }}</span></div>
            <div class="pf-row"><span class="pf-label">大小</span><span class="pf-value">{{ formatSize(previewData.size) }}</span></div>
            <div class="pf-row"><span class="pf-label">版本</span><span class="pf-value">V{{ previewData.version }}</span></div>
            <div class="pf-row"><span class="pf-label">上传人</span><span class="pf-value">{{ previewData.uploaderName }}</span></div>
            <div class="pf-row"><span class="pf-label">上传时间</span><span class="pf-value">{{ previewData.createTime }}</span></div>
            <div class="pf-row" v-if="previewData.description"><span class="pf-label">描述</span><span class="pf-value">{{ previewData.description }}</span></div>
            <div class="pf-row" v-if="previewData.orderId"><span class="pf-label">关联订单</span><span class="pf-value">订单 #{{ previewData.orderId }}</span></div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="downloadPreviewFile">📥 下载文件</el-button>
        <el-button @click="showPreview=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, UploadFilled } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'

const router = useRouter()
const loading = ref(false)
const showUpload = ref(false)
const showVersion = ref(false)
const showPreview = ref(false)
const uploading = ref(false)
const previewData = ref<any>(null)
const previewImgError = ref(false)
const uploadRef = ref()
const tableData = ref<any[]>([])
const versions = ref<any[]>([])
const orders = ref<any[]>([])
const fileList = ref<any[]>([])
const fileMap = new Map<string, File>() // 文件名 → File

// 图片扩展名
const IMAGE_EXTS = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg']

const searchForm = reactive({ name: '', orderId: null as number | null, status: null })
const pagination = reactive({ page: 1, size: 20, total: 0 })
const uploadForm = reactive({ orderId: null as number | null, description: '' })

// ===== 数据加载 =====
async function loadData() {
  loading.value = true
  try {
    const params: any = { current: pagination.page, size: pagination.size }
    if (searchForm.name) params.name = searchForm.name
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.orderId) params.orderId = searchForm.orderId
    const res = await request.get('/design/file', { params })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch { tableData.value = [] }
  loading.value = false
}

function resetSearch() {
  searchForm.name = ''
  searchForm.orderId = null
  searchForm.status = null
  loadData()
}

// ===== 路由跳转 =====
function goToOrder(orderId: number) {
  router.push({ name: 'OrderDetail', params: { id: orderId } })
}

// ===== 文件操作 =====
function viewFile(row: any) {
  previewData.value = row
  previewImgError.value = false
  showPreview.value = true
}

function getDownloadUrl(row: any): string {
  const baseUrl = (import.meta.env.VITE_API_BASE || '').replace(/\/+$/, '')
  return `${baseUrl}/api/design/file/download/${row.id}`
}

function downloadPreviewFile() {
  if (previewData.value) {
    window.open(getDownloadUrl(previewData.value), '_blank')
  }
}

function isImageFile(ext: string): boolean {
  return IMAGE_EXTS.includes((ext || '').toLowerCase())
}

function getFileIcon(ext: string): string {
  const e = (ext || '').replace('.', '').toLowerCase()
  const icons: Record<string, string> = {
    cdr: '🎨', ai: '🎨', psd: '🎨', eps: '🎨', indd: '📐',
    pdf: '📄', doc: '📝', docx: '📝', xls: '📊', xlsx: '📊',
    zip: '📦', rar: '📦', '7z': '📦',
    txt: '📃',
  }
  return icons[e] || '📁'
}

function getFileTypeName(ext: string): string {
  const e = (ext || '').replace('.', '').toLowerCase()
  const names: Record<string, string> = {
    cdr: 'CorelDRAW 设计文件', ai: 'Adobe Illustrator 设计文件',
    psd: 'Adobe Photoshop 设计文件', eps: 'EPS 矢量文件',
    indd: 'Adobe InDesign 排版文件',
    pdf: 'PDF 文档', doc: 'Word 文档', docx: 'Word 文档',
    xls: 'Excel 表格', xlsx: 'Excel 表格',
    zip: '压缩包', rar: '压缩包', '7z': '压缩包',
    png: 'PNG 图片', jpg: 'JPG 图片', jpeg: 'JPG 图片',
    gif: 'GIF 动图', bmp: 'BMP 位图', webp: 'WebP 图片', svg: 'SVG 矢量图',
    txt: '文本文件',
  }
  return names[e] || `${(ext || '').toUpperCase()} 文件`
}

async function viewVersions(row: any) {
  const res = await request.get(`/design/file/${row.id}/versions`)
  versions.value = res.data || []
  showVersion.value = true
}

async function deleteFile(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除文件 "${row.originalName || row.name}"？此操作不可恢复。`, '确认删除', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await request.delete(`/design/file/${row.id}`)
    ElMessage.success('已删除')
    loadData()
  } catch {
    // 用户取消或操作失败
  }
}

// ===== 上传文件 =====
function onFileChange(uploadFile: any) {
  if (uploadFile.raw) {
    fileMap.set(uploadFile.name, uploadFile.raw)
  }
}

function onFileRemove(uploadFile: any) {
  fileMap.delete(uploadFile.name)
}

async function submitUpload() {
  // 验证：必须选择文件
  if (!fileMap.size) {
    ElMessage.warning('请先选择要上传的文件')
    return
  }
  // 验证：必须关联订单
  if (!uploadForm.orderId) {
    ElMessage.warning('请选择关联订单')
    return
  }

  uploading.value = true
  const token = localStorage.getItem('token') || ''
  const baseUrl = (import.meta.env.VITE_API_BASE || '').replace(/\/+$/, '')
  let successCount = 0
  let failCount = 0

  for (const file of fileMap.values()) {
    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('orderId', String(uploadForm.orderId))
      if (uploadForm.description) formData.append('description', uploadForm.description)

      const res = await fetch(`${baseUrl}/api/design/file/upload`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData,
      })
      if (res.ok) successCount++
      else failCount++
    } catch {
      failCount++
    }
  }

  uploading.value = false

  // 重置上传状态
  showUpload.value = false
  fileList.value = []
  fileMap.clear()
  uploadForm.orderId = null
  uploadForm.description = ''
  uploadRef.value?.clearFiles()

  if (failCount === 0) {
    ElMessage.success(`成功上传 ${successCount} 个文件`)
  } else {
    ElMessage.warning(`上传完成：${successCount} 成功，${failCount} 失败`)
  }
  loadData()
}

// ===== 工具函数 =====
function formatExt(ext: string) {
  return (ext || '').replace('.', '').toUpperCase()
}

function formatSize(bytes: number) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

onMounted(() => {
  loadData()
  // 加载订单列表用于上传选择
  request.get('/orders', { params: { current: 1, size: 200 } })
    .then(r => orders.value = (r.data?.records || r.data || []))
    .catch(() => {})
})
</script>

<style scoped>
/* ===== 文件预览弹窗 ===== */
.file-preview-body {
  min-height: 200px;
}
.preview-image-wrap {
  text-align: center;
  max-height: 500px;
  overflow: auto;
}
.preview-image {
  max-width: 100%;
  max-height: 480px;
  border-radius: 6px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}
.preview-fallback {
  text-align: center;
  padding: 60px 0;
  color: #909399;
}
.fallback-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}
.preview-file-info {
  display: flex;
  gap: 30px;
  padding: 20px 10px;
}
.preview-file-icon {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  width: 100px;
}
.pf-icon {
  font-size: 56px;
  line-height: 1;
}
.pf-ext {
  font-size: 12px;
  font-weight: 700;
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 12px;
  border-radius: 4px;
}
.preview-file-details {
  flex: 1;
}
.pf-row {
  display: flex;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}
.pf-row:last-child {
  border-bottom: none;
}
.pf-label {
  width: 80px;
  color: #909399;
  flex-shrink: 0;
}
.pf-value {
  color: #303133;
  word-break: break-all;
}
</style>