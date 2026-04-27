<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">📂 物料分类管理</span>
      <div class="page-actions">
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon> 新增分类
        </el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="150">
          <template #default="{ row }">
            <span class="category-name">
              <span class="category-icon">{{ row.icon || '📦' }}</span>
              {{ row.name }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="80">
          <template #default="{ row }">{{ row.icon || '📦' }}</template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="materialCount" label="物料数量" width="100" />
        <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="editRow(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="deleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editForm.id ? '编辑分类' : '新增分类'" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="分类名称" required>
          <el-input v-model="editForm.name" placeholder="如：纸张、油墨、耗材" />
        </el-form-item>
        <el-form-item label="分类图标">
          <div class="icon-selector">
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
          <el-input-number v-model="editForm.sortOrder" :min="1" :max="999" />
          <span class="form-tip">数值越小排越前</span>
        </el-form-item>
        <el-form-item label="分类说明">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="分类用途说明..." />
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
        <el-button type="primary" @click="saveCategory">保存</el-button>
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

const iconOptions = ['📄', '📃', '📑', '📋', '🖼️', '🎨', '🖌️', '🖊️', '📦', '📊', '📁', '📌', '✂️', '📏', '🔧', '⚙️']

const defaultForm = {
  id: null as number | null,
  name: '',
  icon: '📦',
  sortOrder: 1,
  description: '',
  status: 1
}

const editForm = reactive<any>({ ...defaultForm })

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/material/category')

    tableData.value = res.data || []
  } catch (e) {
    tableData.value = [
      { id: 1, name: '纸张类', icon: '📄', sortOrder: 1, materialCount: 25, description: '各类印刷用纸张', status: 1 },
      { id: 2, name: '油墨类', icon: '🎨', sortOrder: 2, materialCount: 12, description: '印刷油墨、墨水', status: 1 },
      { id: 3, name: '耗材类', icon: '📦', sortOrder: 3, materialCount: 35, description: '各类辅助耗材', status: 1 },
      { id: 4, name: '装订材料', icon: '📋', sortOrder: 4, materialCount: 8, description: '装订用材料', status: 1 },
      { id: 5, name: '覆膜材料', icon: '🖼️', sortOrder: 5, materialCount: 6, description: '覆膜、冷裱材料', status: 1 },
      { id: 6, name: '写真材料', icon: '🖌️', sortOrder: 6, materialCount: 15, description: '写真、喷绘材料', status: 1 }
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

const saveCategory = async () => {
  if (!editForm.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  try {
    if (editForm.id) {
      await request.put(`/material/category/${editForm.id}`, editForm)
      ElMessage.success('更新成功')
    } else {
      await request.post('/material/category', editForm)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const deleteRow = async (row: any) => {
  if (row.materialCount > 0) {
    ElMessage.warning('该分类下有物料，无法删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除分类"${row.name}"吗？`, '提示', { type: 'warning' })
    await request.delete(`/material/category/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.category-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-icon {
  font-size: 18px;
}

.icon-selector {
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
