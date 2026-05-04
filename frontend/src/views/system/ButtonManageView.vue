<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item">系统管理</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item active">按钮管理</span>
    </div>

    <div class="page-header">
      <span class="page-title">🎨 按钮管理</span>
      <div class="page-actions">
        <el-button type="primary" @click="showDialog()"><el-icon><Plus /></el-icon> 新增按钮</el-button>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="按钮名称">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:100px;">
            <el-option label="启用" value="1" />
            <el-option label="停用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 按钮列表 -->
    <div class="card">
      <div class="card-header-row"><span class="card-title">📋 系统按钮列表</span></div>
      <el-table :data="flatButtons" stripe v-loading="loading">
        <el-table-column prop="id" label="按钮ID" width="80" />
        <el-table-column prop="name" label="按钮名称" width="130" />
        <el-table-column prop="moduleName" label="所属模块" width="120" />
        <el-table-column prop="typeLabel" label="按钮类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.type==='primary'?'primary':row.type==='success'?'success':row.type==='warning'?'warning':'danger'" size="small">{{ row.typeLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="permission" label="权限编码" min-width="150">
          <template #default="{ row }"><el-tag size="small" type="info">{{ row.permission }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status===1?'success':'danger'" size="small">{{ row.status===1?'启用':'禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showDialog(row)">编辑</el-button>
            <el-button v-if="row.status===0" link type="success" size="small" @click="toggleStatus(row)">启用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 按钮样式预览 -->
    <div class="card" style="margin-top:20px;">
      <div class="card-header-row"><span class="card-title">🎨 按钮样式预览</span></div>
      <div class="btn-preview">
        <el-button type="primary">主要按钮</el-button>
        <el-button type="success">成功按钮</el-button>
        <el-button>默认按钮</el-button>
        <el-button type="danger">危险按钮</el-button>
        <el-button type="warning">警告按钮</el-button>
        <el-button type="info">信息按钮</el-button>
        <el-divider direction="vertical" />
        <el-button type="primary" size="small">小号按钮</el-button>
        <el-button size="small">小号默认</el-button>
        <el-divider direction="vertical" />
        <el-button link type="primary">查看</el-button>
        <el-button link type="warning">编辑</el-button>
        <el-button link type="danger">删除</el-button>
      </div>
    </div>

    <!-- 树形按钮权限管理 -->
    <div class="card" style="margin-top:20px;">
      <div class="card-header-row"><span class="card-title">🌳 按钮权限树</span></div>
      <el-table :data="tableData" stripe v-loading="loading" row-key="id" default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column prop="name" label="菜单/按钮" min-width="200" />
        <el-table-column prop="permission" label="权限标识" min-width="180">
          <template #default="{ row }"><el-tag size="small" type="info">{{ row.permission }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type==='button'?'primary':row.type==='menu'?'success':'warning'">
              {{ {button:'按钮',menu:'菜单',api:'接口'}[row.type] || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="toggleStatus(row)" /></template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="showDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="deleteItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit?'✏️ 编辑按钮':'🎨 新增按钮'" width="500px" destroy-on-close>
      <el-form :model="formData" label-width="100px">
        <el-form-item label="按钮名称"><el-input v-model="formData.name" placeholder="请输入" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="formData.permission" placeholder="如: system:user:add" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="formData.type" style="width:100%;">
            <el-option label="按钮" value="button" />
            <el-option label="菜单" value="menu" />
            <el-option label="接口" value="api" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属模块">
          <el-select v-model="formData.moduleName" placeholder="请选择" style="width:100%;" v-if="!isEdit || formData.type==='button'">
            <el-option label="用户管理" value="用户管理" />
            <el-option label="订单管理" value="订单管理" />
            <el-option label="每日流水" value="每日流水" />
            <el-option label="统计分析" value="统计分析" />
            <el-option label="财务管理" value="财务管理" />
          </el-select>
          <el-input v-else :model-value="formData.moduleName || formData.name" disabled />
        </el-form-item>
        <el-form-item label="上级菜单">
          <el-tree-select v-model="formData.parentId" :data="menuTree" :props="{label:'name',value:'id'}" check-strictly placeholder="请选择" style="width:100%;" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序"><el-input-number v-model="formData.sort" :min="0" style="width:100%;" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态"><el-switch v-model="formData.status" :active-value="1" :inactive-value="0" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref<any[]>([])
const searchForm = ref({ name: '', status: '' })
const formData = reactive({ id: 0, name: '', permission: '', type: 'button', parentId: null, sort: 0, status: 1, moduleName: '' })
const menuTree = ref<any[]>([])

// 扁平化的按钮列表（用于第一个表格展示）
const flatButtons = computed(() => {
  const list: any[] = []
  tableData.value.forEach((menu: any) => {
    list.push({ ...menu, typeLabel: '主要按钮' })
    ;(menu.children || []).forEach((child: any) => {
      list.push({ ...child, moduleName: menu.name, typeLabel: '功能按钮' })
    })
  })
  return list
})

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/system/buttons', { params: searchForm.value })
    tableData.value = res.data?.records || res.data || []
    menuTree.value = tableData.value.filter((i: any) => i.type === 'menu')
  } catch {}
  loading.value = false
}

function resetSearch() { searchForm.value = { name: '', status: '' }; loadData() }
function showDialog(row?: any) {
  isEdit.value = !!row
  if (row) Object.assign(formData, { ...row })
  else Object.assign(formData, { id: 0, name: '', permission: '', type: 'button', parentId: null, sort: 0, status: 1, moduleName: '' })
  dialogVisible.value = true
}

function toggleStatus(row: any) {
  request.put('/system/buttons/' + row.id + '/status', { status: row.status }).then(() => {
    ElMessage.success(row.status ? '已启用' : '已停用')
  }).catch(() => { row.status = row.status ? 0 : 1 })
}

async function deleteItem(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该按钮？', '确认删除', { type: 'warning' })
    await request.delete('/system/buttons/' + row.id)
    ElMessage.success('已删除')
    loadData()
  } catch {}
}

async function submitForm() {
  try {
    if (isEdit.value) await request.put('/system/buttons/' + formData.id, formData)
    else await request.post('/system/buttons', formData)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch {}
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.btn-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  align-items: center;
  padding: 20px;
}
</style>
