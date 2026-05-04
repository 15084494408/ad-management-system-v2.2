<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item">客户管理</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">客户等级</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">🏆 客户等级管理</h1>
      <div class="page-actions">
        <button class="btn btn-primary" @click="openAddModal">+ 新增等级</button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="card">
      <table class="data-table">
        <thead>
          <tr>
            <th>等级ID</th>
            <th>等级名称</th>
            <th>等级图标</th>
            <th>消费门槛</th>
            <th>折扣比例</th>
            <th>客户数量</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in tableData" :key="row.id">
            <td>{{ row.id }}</td>
            <td>
              <span :style="{ color: levelColor(row.level), fontWeight: 600 }">
                {{ levelIcon(row.level) }} {{ row.name }}
              </span>
            </td>
            <td>{{ levelIcon(row.level) }}</td>
            <td>¥{{ (row.minAmount || 0).toLocaleString() }}+</td>
            <td>{{ row.discount >= 100 ? '原价' : row.discount / 10 + '折' }}</td>
            <td>{{ row.customerCount || 0 }}</td>
            <td class="action-btns">
              <button class="action-btn edit" @click="editRow(row)">编辑</button>
            </td>
          </tr>
          <tr v-if="tableData.length === 0">
            <td colspan="7" style="text-align:center;padding:40px;color:#c0c4cc;">暂无等级数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div class="modal-overlay" :class="{ show: showModal }" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">{{ editForm.id ? '✏️ 编辑客户等级' : '🏆 新增客户等级' }}</span>
          <button class="modal-close" @click="showModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">等级名称 *</label>
              <input v-model="editForm.name" type="text" class="form-input" placeholder="如: 铂金会员">
            </div>
            <div class="form-group">
              <label class="form-label">等级权重</label>
              <input v-model="editForm.level" type="number" class="form-input" placeholder="数值越大等级越高" min="1">
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">消费门槛 (¥)</label>
              <input v-model="editForm.minAmount" type="number" class="form-input" placeholder="最低消费金额" min="0">
            </div>
            <div class="form-group">
              <label class="form-label">折扣比例 (%)</label>
              <input v-model="editForm.discount" type="number" class="form-input" placeholder="如: 90表示9折" min="0" max="100">
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">等级说明</label>
            <textarea v-model="editForm.description" class="form-input" placeholder="等级特权说明..."></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showModal = false">取消</button>
          <button class="btn btn-primary" @click="saveLevel">💾 保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const showModal = ref(false)
const tableData = ref<any[]>([])

const defaultForm = {
  id: null as number | null,
  name: '',
  level: 10,
  discount: 100,
  minAmount: 0,
  description: ''
}

const editForm = reactive<any>({ ...defaultForm })

function levelColor(level: number): string {
  if (level >= 90) return '#f56c6c'
  if (level >= 70) return '#e6a23c'
  if (level >= 50) return '#409eff'
  return '#909399'
}

function levelIcon(level: number): string {
  if (level >= 90) return '💎'
  if (level >= 70) return '🌟'
  if (level >= 50) return '⭐'
  return '🥉'
}

async function loadData() {
  try {
    const res = await request.get('/customers/levels')
    tableData.value = res.data || []
  } catch {
    tableData.value = []
  }
}

function openAddModal() {
  Object.assign(editForm, defaultForm)
  showModal.value = true
}

function editRow(row: any) {
  Object.assign(editForm, row)
  showModal.value = true
}

async function saveLevel() {
  if (!editForm.name) {
    ElMessage.warning('请输入等级名称')
    return
  }
  try {
    if (editForm.id) {
      await request.put(`/customers/levels/${editForm.id}`, editForm)
    } else {
      await request.post('/customers/levels', editForm)
    }
    ElMessage.success('保存成功')
    showModal.value = false
    loadData()
  } catch {
    ElMessage.error('保存失败')
  }
}

onMounted(loadData)
</script>
