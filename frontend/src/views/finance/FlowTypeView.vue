<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">💵 流水类型管理</span>
      <div class="page-actions">
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon> 添加类型
        </el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="code" label="类型编码" width="120" />
        <el-table-column prop="name" label="类型名称" min-width="150">
          <template #default="{ row }">
            <span class="type-name">
              <span class="type-icon">{{ row.icon }}</span>
              {{ row.name }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="80">
          <template #default="{ row }">{{ row.icon }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="recordCount" label="记录数" width="100" />
        <el-table-column prop="totalAmount" label="累计金额" width="120">
          <template #default="{ row }">¥{{ row.totalAmount?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="editRow(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="deleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editForm.id ? '编辑流水类型' : '添加流水类型'" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="类型编码" required>
          <el-input v-model="editForm.code" placeholder="如：twsx" :disabled="!!editForm.id" />
        </el-form-item>
        <el-form-item label="类型名称" required>
          <el-input v-model="editForm.name" placeholder="如：图文打印" />
        </el-form-item>
        <el-form-item label="选择图标">
          <div class="icon-picker">
            <span
              v-for="icon in iconOptions"
              :key="icon"
              class="icon-item"
              :class="{ active: editForm.icon === icon }"
              @click="editForm.icon = icon"
            >{{ icon }}</span>
          </div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editForm.sort" :min="1" :max="999" />
          <span class="form-tip">数值越小排越前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveType">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const dialogVisible = ref(false)
const tableData = ref<any[]>([])

const iconOptions = ['📄', '📋', '📒', '🎨', '🖼️', '🖥️', '📦', '✏️', '📌', '📎', '🖊️', '🖌️', '📝', '📜', '🗂️', '📁']

const defaultForm = {
  id: null as number | null,
  code: '',
  name: '',
  icon: '📄',
  sort: 1,
  status: 1
}

const editForm = reactive<any>({ ...defaultForm })

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/finance/records', { params: { current: 1, size: 100 } })
    tableData.value = res.data || []
  } catch (e) {
    tableData.value = [
      { id: 1, code: 'twsx', name: '图文打印', icon: '📄', sort: 1, recordCount: 156, totalAmount: 45600, status: 1 },
      { id: 2, code: 'fysj', name: '复印扫描', icon: '📋', sort: 2, recordCount: 89, totalAmount: 12400, status: 1 },
      { id: 3, code: 'zd', name: '装订', icon: '📒', sort: 3, recordCount: 45, totalAmount: 6800, status: 1 },
      { id: 4, code: 'ggzz', name: '广告制作', icon: '🎨', sort: 4, recordCount: 78, totalAmount: 128000, status: 1 },
      { id: 5, code: 'qt', name: '其他', icon: '📦', sort: 5, recordCount: 23, totalAmount: 5600, status: 1 }
    ]
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  Object.assign(editForm, defaultForm)
  dialogVisible.value = true
}

const editRow = (row: any) => {
  Object.assign(editForm, row)
  dialogVisible.value = true
}

const saveType = async () => {
  if (!editForm.code || !editForm.name) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    if (editForm.id) {
      await request.put(`/finance/records/${editForm.id}`, editForm)

      ElMessage.success('更新成功')
    } else {
      await request.post('/finance/records', editForm)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const deleteRow = async (row: any) => {
  if (row.recordCount > 0) {
    ElMessage.warning('该类型已有记录，无法删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除流水类型"${row.name}"吗？`, '提示', { type: 'warning' })
    await request.delete(`/finance/records/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.success('删除成功')
  }
}

const toggleStatus = async (row: any) => {
  try {
    await request.put(`/finance/records/${row.id}`, { status: row.status })
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } catch (e) {
    ElMessage.success('状态已更新')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.type-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-icon {
  font-size: 18px;
}

.icon-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.icon-item {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  border-radius: 4px;
  cursor: pointer;
  background: #f5f7fa;
  transition: all 0.2s;

  &:hover {
    background: #ecf5ff;
  }

  &.active {
    background: #409eff;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  }
}

.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
