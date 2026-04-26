<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item">系统管理</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item active">数据字典</span>
    </div>

    <div class="page-header">
      <span class="page-title">📖 数据字典</span>
      <div class="page-actions">
        <el-button @click="exportDict"><el-icon><Download /></el-icon> 导出</el-button>
        <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增字典</el-button>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="字典名称"><el-input v-model="searchForm.name" placeholder="请输入字典名称" clearable /></el-form-item>
        <el-form-item label="字典编码"><el-input v-model="searchForm.code" placeholder="请输入字典编码" clearable /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px;">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="handleSearch">🔍 搜索</el-button><el-button @click="resetSearch">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="字典ID" width="80" />
        <el-table-column prop="name" label="字典名称" width="140" />
        <el-table-column prop="code" label="字典编码" width="140" />
        <el-table-column prop="itemCount" label="字典项数量" width="110">
          <template #default="{ row }"><el-tag size="small" type="info">{{ row.itemCount || 0 }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status===1?'success':'danger'" size="small">{{ row.status===1?'启用':'禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewItems(row)">查看</el-button>
            <el-button link type="primary" size="small" @click="editDict(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="toggleDictStatus(row)">{{ row.status===1?'禁用':'启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" :total="pagination.total"
          layout="total, prev, pager, next" @size-change="loadData" @current-change="loadData" />
      </div>
    </div>

    <!-- 字典项查看弹窗 -->
    <el-dialog v-model="showItemDialog" title="📋 字典项" width="600px">
      <div v-if="currentDict">
        <div style="margin-bottom:15px;font-weight:600;">{{ currentDict.name }}（{{ currentDict.code }}）</div>
        <el-table :data="dictItems" size="small" stripe>
          <el-table-column prop="label" label="显示标签" />
          <el-table-column prop="value" label="值" width="120" />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" type="primary">编辑</el-button>
              <el-button size="small" type="danger">删</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer><el-button @click="showItemDialog=false">关闭</el-button></template>
    </el-dialog>

    <!-- 新增/编辑字典弹窗 -->
    <el-dialog v-model="showEditDialog" :title="editForm.id?'✏️ 编辑字典':'📖 新增字典'" width="550px" destroy-on-close>
      <el-form :model="editForm" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="字典名称"><el-input v-model="editForm.name" placeholder="如：客户等级" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字典编码"><el-input v-model="editForm.code" placeholder="如：customer_level" :disabled="!!editForm.id" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="字典描述"><el-input v-model="editForm.description" type="textarea" :rows="2" placeholder="字典描述信息..." /></el-form-item>
        <el-form-item label="字典项">
          <div v-for="(item, idx) in editForm.items" :key="idx" style="display:flex;gap:8px;margin-bottom:8px;">
            <el-input v-model="item.label" placeholder="标签" style="flex:1;" />
            <el-input v-model="item.value" placeholder="值" style="flex:1;" />
            <el-button size="small" type="danger" circle @click="editForm.items.splice(idx,1)">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <el-button size="small" @click="editForm.items.push({label:'',value:''})">+ 添加字典项</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog=false">取消</el-button>
        <el-button type="primary" @click="saveDict" :loading="saving">{{ editForm.id ? '💾 保存修改' : '💾 创建字典' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Close } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const showEditDialog = ref(false)
const showItemDialog = ref(false)
const tableData = ref<any[]>([])
const dictItems = ref<any[]>([])
const currentDict = ref<any>(null)
const searchForm = reactive({ name: '', code: '', status: null as number | null })
const pagination = reactive({ page: 1, size: 20, total: 0 })
const editForm = reactive<any>({ id: null, name: '', code: '', description: '', items: [{ label: '', value: '' }] })

async function loadData() {
  loading.value = true
  try {
    const params: any = { current: pagination.page, size: pagination.size }
    if (searchForm.name) params.name = searchForm.name
    if (searchForm.code) params.code = searchForm.code
    if (searchForm.status !== null) params.status = searchForm.status
    const res = await request.get('/system/dict', { params })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch { tableData.value = [] }
  loading.value = false
}

function handleSearch() { pagination.page = 1; loadData() }
function resetSearch() { searchForm.name = ''; searchForm.code = ''; searchForm.status = null; loadData() }

function openAdd() {
  Object.assign(editForm, { id: null, name: '', code: '', description: '', items: [{ label: '', value: '' }] })
  showEditDialog.value = true
}

function editDict(row: any) {
  Object.assign(editForm, { id: row.id, name: row.name, code: row.code, description: row.description || '', items: row.items || [{ label: '', value: '' }] })
  showEditDialog.value = true
}

async function saveDict() {
  if (!editForm.name || !editForm.code) { ElMessage.warning('请填写字典名称和编码'); return }
  saving.value = true
  try {
    if (editForm.id) {
      await request.put(`/system/dict/${editForm.id}`, editForm)
      ElMessage.success('字典已保存！')
    } else {
      await request.post('/system/dict', editForm)
      ElMessage.success('字典创建成功！')
    }
    showEditDialog.value = false
    loadData()
  } catch { ElMessage.error('操作失败') }
  saving.value = false
}

async function toggleDictStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await request.put(`/system/dict/${row.id}/status`, null, { params: { status: newStatus } })
    ElMessage.success(`${action}成功`)
    loadData()
  } catch {}
}

async function viewItems(row: any) {
  currentDict.value = row
  try {
    const res = await request.get(`/system/dict/${row.id}/items`)
    dictItems.value = res.data || []
  } catch { dictItems.value = [] }
  showItemDialog.value = true
}

function exportDict() { ElMessage.info('正在导出字典数据...') }

onMounted(loadData)
</script>
