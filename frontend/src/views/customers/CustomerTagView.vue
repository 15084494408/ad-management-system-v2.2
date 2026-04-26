<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item">客户管理</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">客户标签</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">🏷️ 客户标签管理</h1>
      <div class="page-actions">
        <button class="btn btn-primary" @click="openAddModal">+ 新增标签</button>
      </div>
    </div>

    <!-- 标签云 -->
    <div class="card">
      <div class="tag-cloud">
        <div
          v-for="tag in tableData" :key="tag.id"
          class="tag-pill"
          :style="{ background: tag.color || '#ecf5ff' }"
        >
          <span>{{ tag.icon || '🏷️' }} {{ tag.name }}</span>
          <span class="tag-count" :style="{ color: tag.color }">({{ tag.customerCount || 0 }})</span>
          <button class="tag-del" @click.stop="deleteTag(tag)">✕</button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div class="modal-overlay" :class="{ show: showModal }" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">{{ editForm.id ? '✏️ 编辑标签' : '🏷️ 新增标签' }}</span>
          <button class="modal-close" @click="showModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">标签名称 *</label>
              <input v-model="editForm.name" type="text" class="form-input" placeholder="如：优质客户、重点客户">
            </div>
            <div class="form-group">
              <label class="form-label">图标</label>
              <div class="icon-picker">
                <span
                  v-for="icon in iconOptions" :key="icon"
                  class="icon-option" :class="{ active: editForm.icon === icon }"
                  @click="editForm.icon = icon"
                >{{ icon }}</span>
              </div>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">标签颜色</label>
            <div class="color-picker">
              <div
                v-for="c in colorOptions" :key="c"
                class="color-option" :class="{ active: editForm.color === c }"
                :style="{ background: c }"
                @click="editForm.color = c"
              ></div>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">标签说明</label>
            <textarea v-model="editForm.description" class="form-input" placeholder="标签用途说明..."></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showModal = false">取消</button>
          <button class="btn btn-primary" @click="saveTag">💾 保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const showModal = ref(false)
const tableData = ref<any[]>([])

const colorOptions = [
  '#f0f9eb', '#ecf5ff', '#fdf6ec', '#fef0f0', '#f3e5f5',
  '#e0f7fa', '#fff3e0', '#f1f8e9', '#fce4ec', '#e8eaf6'
]

const iconOptions = ['⭐', '💎', '👑', '🔥', '🎯', '💰', '🌟', '✨', '📌', '🏅', '🏭', '💼']

const defaultForm = {
  id: null as number | null,
  name: '', color: '#ecf5ff', icon: '⭐', description: '', customerCount: 0
}

const editForm = reactive<any>({ ...defaultForm })

async function loadData() {
  try {
    const res = await request.get('/customers/tags')
    tableData.value = res.data || []
  } catch {
    tableData.value = []
  }
}

function openAddModal() {
  Object.assign(editForm, defaultForm)
  showModal.value = true
}

async function saveTag() {
  if (!editForm.name) {
    ElMessage.warning('请输入标签名称')
    return
  }
  try {
    if (editForm.id) {
      await request.put(`/customers/tags/${editForm.id}`, editForm)
    } else {
      await request.post('/customers/tags', editForm)
    }
    ElMessage.success('保存成功')
    showModal.value = false
    loadData()
  } catch {
    ElMessage.error('保存失败')
  }
}

async function deleteTag(tag: any) {
  try {
    await ElMessageBox.confirm(`确定删除标签"${tag.name}"吗？`, '提示', { type: 'warning' })
    if (tag.id) await request.delete(`/customers/tags/${tag.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  padding: 20px;
}

.tag-pill {
  padding: 15px 25px;
  border-radius: 25px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.tag-pill:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.tag-count {
  font-size: 12px;
  opacity: 0.7;
}

.tag-del {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.08);
  cursor: pointer;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  transition: all 0.2s;
  opacity: 0;
}

.tag-pill:hover .tag-del {
  opacity: 1;
}

.tag-del:hover {
  background: #f56c6c;
  color: #fff;
}

.icon-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.icon-option {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  background: #f5f7fa;
}

.icon-option:hover { background: #ecf5ff; }
.icon-option.active {
  background: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.color-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.color-option {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.color-option:hover { transform: scale(1.1); }
.color-option.active {
  box-shadow: 0 0 0 2px #fff, 0 0 0 4px #409eff;
}
</style>
