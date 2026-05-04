<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item">系统管理</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item active">用户管理</span>
    </div>

    <div class="page-header">
      <span class="page-title">👥 用户管理</span>
      <div class="page-actions">
        <el-button @click="exportData"><el-icon><Download /></el-icon> 导出</el-button>
        <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增用户</el-button>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名"><el-input v-model="searchForm.username" placeholder="请输入用户名" clearable /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.roleId" placeholder="全部角色" clearable style="width:150px;">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px;">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="handleSearch">🔍 搜索</el-button><el-button @click="resetSearch">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column type="selection" width="40" />
        <el-table-column prop="id" label="用户ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="110" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="roleIds" label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="r in (row.roleIds || [])" :key="r" size="small" type="primary" style="margin-right:4px;">{{ getRoleName(r) }}</el-tag>
            <span v-if="!row.roleIds || row.roleIds.length === 0" style="color:#c0c4cc;font-size:12px;">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status===1?'success':'danger'" size="small">{{ row.status===1?'正常':'禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="editUser(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="resetPwd(row)">重置密码</el-button>
            <el-button link :type="row.status===1?'danger':'success'" size="small" @click="toggleStatus(row)">{{ row.status===1?'禁用':'启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" :total="pagination.total"
          layout="total, sizes, prev, pager, next" @size-change="loadData" @current-change="loadData" />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showDialog" :title="editForm.id?'✏️ 编辑用户':'👤 新增用户'" width="560px" destroy-on-close>
      <el-form :model="editForm" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" v-if="!editForm.id">
              <el-input v-model="editForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="用户名" v-else>
              <el-input :model-value="editForm.username" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名"><el-input v-model="editForm.realName" placeholder="请输入真实姓名" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号码"><el-input v-model="editForm.phone" placeholder="请输入手机号" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户角色">
              <el-select v-model="editForm.roleIds" multiple placeholder="请选择角色" style="width:100%;">
                <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" v-if="!editForm.id">
          <el-col :span="12">
            <el-form-item label="初始密码"><el-input v-model="editForm.password" type="password" placeholder="请输入初始密码" show-password /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码"><el-input v-model="editForm.confirmPassword" type="password" placeholder="请确认密码" show-password /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="editForm.remark" type="textarea" :rows="2" placeholder="可选，填写用户备注信息..." /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog=false">取消</el-button>
        <el-button type="primary" @click="saveUser" :loading="saving">{{ editForm.id ? '💾 保存修改' : '💾 创建用户' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import request from '@/api/request'
import { exportToExcel } from '@/utils/excelExport'

const loading = ref(false)
const showDialog = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const roles = ref<any[]>([])
const roleMap = ref<Record<number, string>>({})

function getRoleName(roleId: any): string {
  return roleMap.value[Number(roleId)] || `角色${roleId}`
}
const searchForm = reactive({ username: '', roleId: null as number | null, status: null as number | null })
const pagination = reactive({ page: 1, size: 20, total: 0 })
const editForm = reactive<any>({ id: null, username: '', realName: '', phone: '', roleIds: [] as number[], password: '', confirmPassword: '', remark: '' })

async function loadData() {
  loading.value = true
  try {
    const params: any = { current: pagination.page, size: pagination.size }
    if (searchForm.username) params.username = searchForm.username
    if (searchForm.roleId) params.roleId = searchForm.roleId
    if (searchForm.status !== null) params.status = searchForm.status
    const res = await request.get('/system/users', { params })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch { tableData.value = [] }
  loading.value = false
}

function handleSearch() { pagination.page = 1; loadData() }
function resetSearch() { searchForm.username = ''; searchForm.roleId = null; searchForm.status = null; loadData() }

function openAdd() {
  Object.assign(editForm, { id: null, username: '', realName: '', phone: '', roleIds: [], password: '', confirmPassword: '', remark: '' })
  showDialog.value = true
}

function editUser(row: any) {
  Object.assign(editForm, { id: row.id, username: row.username, realName: row.realName, phone: row.phone, roleIds: row.roleIds || [], password: '', confirmPassword: '', remark: row.remark || '' })
  showDialog.value = true
}

async function saveUser() {
  if (!editForm.realName) { ElMessage.warning('请输入真实姓名'); return }
  if (!editForm.id && editForm.password !== editForm.confirmPassword) { ElMessage.warning('两次密码不一致'); return }
  saving.value = true
  try {
    if (editForm.id) {
      await request.put(`/system/users/${editForm.id}`, editForm)
      ElMessage.success('用户信息已保存！')
    } else {
      await request.post('/system/users', editForm)
      ElMessage.success('用户创建成功！')
    }
    showDialog.value = false
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
  saving.value = false
}

async function resetPwd(row: any) {
  try {
    await ElMessageBox.confirm(
      '确认重置密码？重置后密码将变为 123456，请通知用户及时修改！',
      '🔑 重置密码',
      { type: 'warning' }
    )
    await request.put(`/system/users/${row.id}/password`)
    ElMessage.success('密码已重置为 123456')
  } catch {}
}

async function toggleStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    if (newStatus === 0) {
      const { value } = await ElMessageBox.prompt('请填写禁用原因', '⚠️ 禁用用户', {
        confirmButtonText: '确认禁用',
        cancelButtonText: '取消',
        type: 'warning',
        inputValidator: (v: string) => v && v.trim() ? true : '请填写禁用原因',
      })
      await request.put(`/system/users/${row.id}/status`, null, { params: { status: newStatus, reason: value } })
    } else {
      await ElMessageBox.confirm(`确认启用该用户？`, action, { type: 'info' })
      await request.put(`/system/users/${row.id}/status`, null, { params: { status: newStatus } })
    }
    ElMessage.success(`${action}成功`)
    loadData()
  } catch {}
}

function exportData() {
  exportToExcel({
    filename: '用户列表',
    header: ['用户ID', '用户名', '真实姓名', '手机号', '角色', '状态', '最后登录'],
    data: tableData.value.map(row => [
      row.id, row.username, row.realName || '-', row.phone || '-',
      (row.roleIds || []).map((r: any) => roleMap.value[Number(r)] || `角色${r}`).join(', '),
      row.status === 1 ? '正常' : '禁用',
      (row.lastLoginTime || '').toString().slice(0, 16),
    ]),
    infoRows: [[`导出时间：${new Date().toLocaleString()}`], [`共 ${tableData.value.length} 条记录`]],
  })
}

onMounted(async () => {
  loadData()
  try { const res = await request.get('/system/roles'); roles.value = res.data || []; roleMap.value = Object.fromEntries((res.data || []).map((r: any) => [r.id, r.roleName])) } catch {}
})
</script>
