<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">🔔 消息中心</span>
      <div class="header-actions">
        <el-button @click="markAllRead" :loading="markingAll" size="small">
          <el-icon><Check /></el-icon> 全部已读
        </el-button>
        <el-button type="primary" @click="showSettingDialog = true" size="small">
          <el-icon><Setting /></el-icon> 通知设置
        </el-button>
      </div>
    </div>

    <div class="main-layout">
      <!-- 左侧统计 -->
      <div class="stats-panel card">
        <div class="stat-item" @click="filterRead = null; loadData()" :class="{ active: filterRead === null }">
          <div class="stat-num">{{ stats.total }}</div>
          <div class="stat-label">全部通知</div>
        </div>
        <div class="stat-item unread" @click="filterRead = 0; loadData()" :class="{ active: filterRead === 0 }">
          <div class="stat-num">{{ stats.unread }}</div>
          <div class="stat-label">未读通知</div>
        </div>
        <div class="stat-item read" @click="filterRead = 1; loadData()" :class="{ active: filterRead === 1 }">
          <div class="stat-num">{{ stats.read }}</div>
          <div class="stat-label">已读通知</div>
        </div>

        <el-divider />

        <div class="type-filter">
          <div
            v-for="t in typeOptions"
            :key="t.value"
            class="type-item"
            :class="{ active: activeType === t.value }"
            @click="activeType = t.value; loadData()"
          >
            <span class="type-icon">{{ t.icon }}</span>
            <span>{{ t.label }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧通知列表 -->
      <div class="notice-panel card">
        <!-- 搜索栏 -->
        <div class="search-bar">
          <el-input
            v-model="keyword"
            placeholder="搜索通知标题..."
            clearable
            @change="loadData"
            style="width: 240px"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="filterLevel" placeholder="优先级" clearable @change="loadData" style="width: 120px">
            <el-option label="普通" :value="1" />
            <el-option label="重要" :value="2" />
            <el-option label="紧急" :value="3" />
          </el-select>
        </div>

        <!-- 通知列表 -->
        <div v-loading="loading" class="notice-list">
          <div
            v-for="n in tableData"
            :key="n.id"
            class="notice-item"
            :class="{ unread: !n.isRead }"
            @click="viewNotice(n)"
          >
            <div
              class="notice-icon"
              :style="{ background: getTypeStyle(n.type).bg, color: getTypeStyle(n.type).color }"
            >
              {{ getTypeIcon(n.type) }}
            </div>
            <div class="notice-content">
              <div class="notice-title">
                <el-tag v-if="n.level === 3" type="danger" size="small" style="margin-right:6px">紧急</el-tag>
                <el-tag v-else-if="n.level === 2" type="warning" size="small" style="margin-right:6px">重要</el-tag>
                {{ n.title }}
              </div>
              <div class="notice-desc">{{ n.content }}</div>
              <div class="notice-time">{{ formatTime(n.createTime) }}</div>
            </div>
            <div class="notice-actions">
              <el-tag v-if="!n.isRead" type="danger" size="small" effect="dark">未读</el-tag>
              <el-tag v-else size="small" type="info">已读</el-tag>
              <el-button
                v-if="!n.isRead"
                link
                size="small"
                @click.stop="markOneRead(n)"
              >标记已读</el-button>
              <el-button link size="small" type="danger" @click.stop="deleteNotice(n)">删除</el-button>
            </div>
          </div>
          <el-empty v-if="!loading && !tableData.length" description="暂无通知" />
        </div>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadData"
            @current-change="loadData"
          />
        </div>
      </div>
    </div>

    <!-- 通知详情弹窗 -->
    <el-dialog v-model="showDetail" title="通知详情" width="520px">
      <div v-if="currentNotice" class="notice-detail">
        <div class="detail-header">
          <span class="detail-icon" :style="{ background: getTypeStyle(currentNotice.type).bg, color: getTypeStyle(currentNotice.type).color }">
            {{ getTypeIcon(currentNotice.type) }}
          </span>
          <div>
            <div class="detail-title">{{ currentNotice.title }}</div>
            <div class="detail-meta">
              <el-tag size="small">{{ getTypeName(currentNotice.type) }}</el-tag>
              <el-tag v-if="currentNotice.level === 3" type="danger" size="small">紧急</el-tag>
              <el-tag v-else-if="currentNotice.level === 2" type="warning" size="small">重要</el-tag>
              <span class="detail-time">{{ formatTime(currentNotice.createTime) }}</span>
            </div>
          </div>
        </div>
        <el-divider />
        <div class="detail-content">{{ currentNotice.content }}</div>
      </div>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 通知设置弹窗 -->
    <el-dialog v-model="showSettingDialog" title="通知设置" width="420px">
      <div v-loading="settingLoading">
        <el-form label-width="120px" :model="setting">
          <el-form-item label="订单通知">
            <el-switch v-model="setting.orderNotify" :active-value="1" :inactive-value="0" />
            <span class="setting-desc">订单新增、状态变更时通知</span>
          </el-form-item>
          <el-form-item label="财务通知">
            <el-switch v-model="setting.financeNotify" :active-value="1" :inactive-value="0" />
            <span class="setting-desc">收款、付款等财务事件通知</span>
          </el-form-item>
          <el-form-item label="系统通知">
            <el-switch v-model="setting.systemNotify" :active-value="1" :inactive-value="0" />
            <span class="setting-desc">系统公告、维护等通知</span>
          </el-form-item>
          <el-form-item label="预警通知">
            <el-switch v-model="setting.warningNotify" :active-value="1" :inactive-value="0" />
            <span class="setting-desc">库存预警、异常预警通知</span>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showSettingDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSetting" :loading="savingSettings">保存设置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Setting, Search } from '@element-plus/icons-vue'
import request from '@/api/request'

// ─── 状态 ───
const loading = ref(false)
const markingAll = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })
const activeType = ref('all')
const filterRead = ref<number | null>(null)
const filterLevel = ref<number | null>(null)
const keyword = ref('')

const showDetail = ref(false)
const currentNotice = ref<any>(null)

const showSettingDialog = ref(false)
const settingLoading = ref(false)
const savingSettings = ref(false)
const setting = reactive({
  orderNotify: 1,
  financeNotify: 1,
  systemNotify: 1,
  warningNotify: 1,
})

// ─── 统计 ───
const stats = reactive({ total: 0, unread: 0, read: 0 })

// ─── 类型配置 ───
const typeOptions = [
  { value: 'all', label: '全部类型', icon: '📋' },
  { value: 'system', label: '系统通知', icon: '🖥️' },
  { value: 'system_order', label: '订单提醒', icon: '📦' },
  { value: 'finance', label: '财务通知', icon: '💰' },
  { value: 'warning', label: '预警通知', icon: '⚠️' },
]

const typeStyleMap: Record<string, { bg: string; color: string }> = {
  system: { bg: '#ecf5ff', color: '#409eff' },
  system_order: { bg: '#fdf6ec', color: '#e6a23c' },
  finance: { bg: '#f0f9eb', color: '#67c23a' },
  warning: { bg: '#fef0f0', color: '#f56c6c' },
}

function getTypeStyle(type: string) {
  return typeStyleMap[type] || { bg: '#f5f7fa', color: '#909399' }
}
function getTypeIcon(type: string) {
  const t = typeOptions.find(o => o.value === type)
  return t ? t.icon : '📋'
}
function getTypeName(type: string) {
  const t = typeOptions.find(o => o.value === type)
  return t ? t.label : type
}

// ─── 格式化时间 ───
function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return t.slice(0, 16)
}

// ─── 加载数据 ───
async function loadData() {
  loading.value = true
  try {
    const params: any = {
      current: pagination.page,
      size: pagination.size,
    }
    if (activeType.value !== 'all') params.type = activeType.value
    if (filterRead.value !== null) params.isRead = filterRead.value
    const res = await request.get('/notice', { params })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0

    // 刷新统计
    updateStats(res.data?.total || 0)
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

async function updateStats(total: number) {
  try {
    const res = await request.get('/notice/unread-count')
    stats.unread = res.data || 0
    stats.total = total
    stats.read = total - stats.unread
  } catch { /* ignore */ }
}

// ─── 查看通知详情 ───
async function viewNotice(n: any) {
  currentNotice.value = n
  showDetail.value = true
  if (!n.isRead) {
    await markOneRead(n)
  }
}

// ─── 标记单条已读 ───
async function markOneRead(n: any) {
  try {
    await request.put(`/notice/${n.id}/read`)
    n.isRead = 1
    stats.unread = Math.max(0, stats.unread - 1)
    stats.read += 1
  } catch {
    ElMessage.error('操作失败')
  }
}

// ─── 全部已读 ───
async function markAllRead() {
  markingAll.value = true
  try {
    await request.put('/notice/read-all')
    ElMessage.success('已全部标记为已读')
    await loadData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    markingAll.value = false
  }
}

// ─── 删除通知 ───
async function deleteNotice(n: any) {
  await ElMessageBox.confirm(`确认删除通知"${n.title}"?`, '提示', { type: 'warning' })
  await request.delete(`/notice/${n.id}`)
  ElMessage.success('删除成功')
  await loadData()
}

// ─── 加载通知设置 ───
async function loadSetting() {
  settingLoading.value = true
  try {
    const res = await request.get('/notice/setting')
    Object.assign(setting, res.data)
  } catch { /* ignore */ } finally {
    settingLoading.value = false
  }
}

// ─── 保存通知设置 ───
async function saveSetting() {
  savingSettings.value = true
  try {
    await request.put('/notice/setting', setting)
    ElMessage.success('设置已保存')
    showSettingDialog.value = false
  } catch {
    ElMessage.error('保存失败')
  } finally {
    savingSettings.value = false
  }
}

onMounted(() => {
  loadData()
  loadSetting()
})
</script>

<style scoped lang="scss">
.main-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

// ── 左侧统计面板 ──
.stats-panel {
  width: 180px;
  flex-shrink: 0;
  padding: 20px 16px;
}

.stat-item {
  text-align: center;
  padding: 12px 8px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: all 0.2s;
  &:hover { background: #f5f7fa; }
  &.active { background: #ecf5ff; }
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
}

.stat-item.unread .stat-num { color: #f56c6c; }
.stat-item.read .stat-num { color: #67c23a; }

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.type-filter { display: flex; flex-direction: column; gap: 4px; }

.type-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  transition: all 0.2s;
  &:hover { background: #f5f7fa; }
  &.active { background: #ecf5ff; color: #409eff; font-weight: 500; }
}

.type-icon { font-size: 16px; }

// ── 右侧通知面板 ──
.notice-panel {
  flex: 1;
  min-width: 0;
  padding: 20px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.notice-list { min-height: 400px; }

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
  border-radius: 4px;
  &:hover { background: #f9f9f9; }
  &.unread {
    background: #fefaf0;
    border-left: 3px solid #e6a23c;
  }
}

.notice-icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.notice-content { flex: 1; min-width: 0; }

.notice-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 5px;
  display: flex;
  align-items: center;
}

.notice-desc {
  font-size: 13px;
  color: #606266;
  margin-bottom: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notice-time { font-size: 12px; color: #909399; }

.notice-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

// ── 通知详情 ──
.notice-detail {}

.detail-header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.detail-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-time { font-size: 12px; color: #909399; }

.detail-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

// ── 通知设置 ──
.setting-desc {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}
</style>
