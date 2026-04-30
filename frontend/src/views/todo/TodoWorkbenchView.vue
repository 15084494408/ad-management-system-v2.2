<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <span class="page-title">📋 待办工作台</span>
      <div class="page-actions">
        <el-button type="primary" @click="showAddDialog = true">
          <el-icon><Plus /></el-icon> 新增待办
        </el-button>
      </div>
    </div>

    <!-- 状态标签栏 -->
    <div class="status-tabs">
      <div
        class="status-tab"
        :class="{ active: filterStatus === null }"
        @click="filterStatus = null"
      >
        全部
        <span class="count">{{ totalCount }}</span>
      </div>
      <div
        v-for="s in statusList"
        :key="s.value"
        class="status-tab"
        :class="{ active: filterStatus === s.value }"
        @click="filterStatus = s.value"
      >
        {{ s.icon }} {{ s.label }}
        <span class="count">{{ s.count }}</span>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      正在加载...
    </div>

    <!-- 空状态 -->
    <div v-else-if="filteredItems.length === 0" class="empty-state">
      <div class="empty-icon">📋</div>
      <div class="empty-title">暂无待办</div>
      <div class="empty-desc">点击右上角「新增待办」收集客户需求</div>
    </div>

    <!-- 卡片网格 -->
    <div v-else class="card-grid">
      <div
        v-for="item in filteredItems"
        :key="item.id"
        class="todo-card"
        :class="'priority-' + item.priority"
        @click="openDetail(item)"
      >
        <!-- 卡片头部 -->
        <div class="card-header">
          <div class="customer-info">
            <div class="customer-avatar">{{ getAvatar(item.customerName) }}</div>
            <div class="customer-meta">
              <div class="customer-name">{{ item.customerName }}</div>
              <div class="customer-phone" v-if="item.contactPhone">
                📞 {{ item.contactPhone }}
              </div>
            </div>
          </div>
          <div class="card-badges">
            <span class="priority-badge" :class="'p' + item.priority">
              {{ item.priorityLabel }}
            </span>
            <span class="status-badge" :class="'s' + item.status">
              {{ item.statusLabel }}
            </span>
          </div>
        </div>

        <!-- 尺寸信息 -->
        <div class="card-section" v-if="item.dimensions">
          <div class="section-label">📐 量尺尺寸</div>
          <div class="section-value dimensions">{{ item.dimensions }}</div>
        </div>

        <!-- 需求描述 -->
        <div class="card-section" v-if="item.requirements">
          <div class="section-label">💬 客户需求</div>
          <div class="section-value requirements">{{ item.requirements }}</div>
        </div>

        <!-- AI报价 -->
        <div class="card-quote" v-if="item.quoteAmount">
          <span class="quote-label">🤖 AI报价：</span>
          <span class="quote-amount">¥{{ formatMoney(item.quoteAmount) }}</span>
        </div>

        <!-- 卡片底部 -->
        <div class="card-footer">
          <div class="card-meta">
            <span v-if="item.source" class="source-tag">📍 {{ item.source }}</span>
            <span class="date">{{ formatDate(item.createTime) }}</span>
          </div>
          <div class="card-actions">
            <template v-if="item.status === 1 || item.status === 2">
              <button class="action-btn ai-btn" @click.stop="generateQuote(item)" title="AI生成报价">
                🤖 AI报价
              </button>
            </template>
            <template v-if="item.status === 3">
              <button class="action-btn convert-btn" @click.stop="convertToOrder(item)" title="转为正式订单">
                📝 转订单
              </button>
            </template>
            <template v-if="item.status === 4">
              <button class="action-btn view-btn" @click.stop="viewOrder(item)" title="查看订单">
                🔗 查看订单
              </button>
            </template>
          </div>
        </div>

        <!-- 已转订单标记 -->
        <div v-if="item.status === 4 && item.orderNo" class="order-tag">
          → {{ item.orderNo }}
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div class="modal-overlay" v-if="showAddDialog" @click.self="showAddDialog = false">
      <div class="modal-container" style="max-width: 580px;">
        <div class="modal-header">
          <h3>{{ editingItem ? '编辑待办' : '📋 新增待办' }}</h3>
          <button class="modal-close" @click="showAddDialog = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label required">客户名称</label>
              <input type="text" v-model="form.customerName" class="form-input" placeholder="如：张三（奶茶店）" />
            </div>
            <div class="form-group">
              <label class="form-label">联系电话</label>
              <input type="text" v-model="form.contactPhone" class="form-input" placeholder="手机号" />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">量尺尺寸</label>
            <input type="text" v-model="form.dimensions" class="form-input"
              placeholder="如：门头宽80cm×高120cm×1块" />
          </div>
          <div class="form-group">
            <label class="form-label">客户需求</label>
            <textarea v-model="form.requirements" class="form-input" rows="3"
              placeholder="描述客户的具体需求、材质、工艺、交期要求等..."></textarea>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">来源</label>
              <select v-model="form.source" class="form-input">
                <option value="">请选择</option>
                <option value="门店">🏪 门店</option>
                <option value="电话">📞 电话</option>
                <option value="微信">💬 微信</option>
                <option value="上门">🚶 上门量尺</option>
                <option value="其他">📋 其他</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">优先级</label>
              <select v-model="form.priority" class="form-input">
                <option :value="1">普通</option>
                <option :value="2">🔴 紧急</option>
                <option :value="3">🟠 加急</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">备注</label>
            <input type="text" v-model="form.remark" class="form-input" placeholder="内部备注（可选）" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showAddDialog = false">取消</button>
          <button class="btn btn-primary" @click="saveItem" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div class="modal-overlay" v-if="showDetail" @click.self="showDetail = false">
      <div class="modal-container" style="max-width: 600px;">
        <div class="modal-header">
          <h3>📋 待办详情</h3>
          <button class="modal-close" @click="showDetail = false">&times;</button>
        </div>
        <div class="modal-body" v-if="currentItem">
          <div class="detail-section">
            <div class="detail-customer">
              <div class="detail-avatar">{{ getAvatar(currentItem.customerName) }}</div>
              <div>
                <div class="detail-name">{{ currentItem.customerName }}</div>
                <div class="detail-phone" v-if="currentItem.contactPhone">📞 {{ currentItem.contactPhone }}</div>
              </div>
            </div>
            <div class="detail-badges">
              <span class="status-badge" :class="'s' + currentItem.status">{{ currentItem.statusLabel }}</span>
              <span class="priority-badge" :class="'p' + currentItem.priority">{{ currentItem.priorityLabel }}</span>
              <span v-if="currentItem.source" class="source-tag">{{ currentItem.source }}</span>
            </div>
          </div>

          <div class="detail-grid">
            <div class="detail-item" v-if="currentItem.dimensions">
              <div class="detail-label">📐 量尺尺寸</div>
              <div class="detail-value">{{ currentItem.dimensions }}</div>
            </div>
            <div class="detail-item" v-if="currentItem.quoteAmount">
              <div class="detail-label">🤖 AI报价</div>
              <div class="detail-value quote">¥{{ formatMoney(currentItem.quoteAmount) }}</div>
            </div>
            <div class="detail-item" v-if="currentItem.orderNo">
              <div class="detail-label">📝 关联订单</div>
              <div class="detail-value link" @click="viewOrder(currentItem)">{{ currentItem.orderNo }}</div>
            </div>
            <div class="detail-item full" v-if="currentItem.requirements">
              <div class="detail-label">💬 客户需求</div>
              <div class="detail-value requirements">{{ currentItem.requirements }}</div>
            </div>
          </div>

          <div class="detail-timeline">
            <div class="timeline-item">
              <span class="timeline-dot"></span>
              <span>创建于 {{ formatDate(currentItem.createTime) }}</span>
            </div>
            <div class="timeline-item" v-if="currentItem.updateTime !== currentItem.createTime">
              <span class="timeline-dot"></span>
              <span>更新于 {{ formatDate(currentItem.updateTime) }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-danger" @click="deleteItem" style="margin-right:auto;">删除</button>
          <button class="btn btn-default" @click="showDetail = false">关闭</button>
          <button v-if="currentItem && (currentItem.status === 1 || currentItem.status === 2)"
            class="btn btn-primary" @click="generateQuote(currentItem)">
            🤖 AI报价
          </button>
          <button v-if="currentItem && currentItem.status === 3"
            class="btn btn-success" @click="convertToOrder(currentItem)">
            📝 转为订单
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { todoApi } from '@/api/modules/todo'

const router = useRouter()
const loading = ref(false)
const filterStatus = ref<number | null>(null)
const showAddDialog = ref(false)
const showDetail = ref(false)
const editingItem = ref<any>(null)
const currentItem = ref<any>(null)
const saving = ref(false)
const allItems = ref<any[]>([])

const statusList = ref([
  { value: 1, label: '新收集', icon: '💡', count: 0 },
  { value: 2, label: '分析中', icon: '🔍', count: 0 },
  { value: 3, label: '待确认', icon: '✅', count: 0 },
  { value: 4, label: '已转订单', icon: '📝', count: 0 },
])

const form = reactive({
  customerName: '',
  contactPhone: '',
  dimensions: '',
  requirements: '',
  source: '',
  priority: 1,
  remark: '',
})

const filteredItems = computed(() => {
  const items = filterStatus.value === null
    ? allItems.value
    : allItems.value.filter(it => it.status === filterStatus.value)
  // 已转订单(status=4)排到最后，未完成的排前面
  return [...items].sort((a, b) => {
    if (a.status === 4 && b.status !== 4) return 1
    if (a.status !== 4 && b.status === 4) return -1
    return 0
  })
})

const totalCount = computed(() => allItems.value.length)

function getAvatar(name: string) {
  return name ? name.charAt(0).toUpperCase() : '?'
}

function formatMoney(v: any) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(t: any) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

async function loadData() {
  loading.value = true
  try {
    const res = await todoApi.getList()
    const data = res.data?.[0] || {}
    allItems.value = data.items || []
    const counts = data.statusCount || {}

    statusList.value.forEach(s => {
      s.count = counts['status' + s.value] || 0
    })
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function openDetail(item: any) {
  currentItem.value = item
  showDetail.value = true
}

function resetForm() {
  Object.assign(form, {
    customerName: '', contactPhone: '', dimensions: '', requirements: '',
    source: '', priority: 1, remark: '',
  })
  editingItem.value = null
}

async function saveItem() {
  if (!form.customerName?.trim()) {
    ElMessage.warning('请填写客户名称')
    return
  }
  saving.value = true
  try {
    if (editingItem.value) {
      await todoApi.update(editingItem.value.id, { ...form })
      ElMessage.success('已更新')
    } else {
      await todoApi.create({ ...form })
      ElMessage.success('已新增')
    }
    showAddDialog.value = false
    resetForm()
    loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function deleteItem() {
  if (!currentItem.value) return
  try {
    await ElMessageBox.confirm('确定删除此待办？', '提示', { type: 'warning' })
    await todoApi.delete(currentItem.value.id)
    showDetail.value = false
    loadData()
    ElMessage.success('已删除')
  } catch {}
}

async function generateQuote(item: any) {
  // TODO: 后续接入 AI 自动生成报价
  // 目前先更新状态为"待确认"
  try {
    await todoApi.updateStatus(item.id, { status: 3 })
    await ElMessageBox.alert(
      '🤖 AI报价功能即将上线！\n\n' +
      '在此之前，您可以：\n' +
      '1. 手动估算报价金额\n' +
      '2. 点击「转为订单」手动创建正式报价',
      '功能预告'
    )
    loadData()
  } catch {}
}

async function convertToOrder(item: any) {
  // 跳转到创建订单页面，预填客户名和需求，带上待办ID
  showDetail.value = false
  router.push({
    name: 'OrderCreate',
    query: {
      fromTodo: '1',
      todoId: String(item.id),
      customerName: item.customerName,
      contactPhone: item.contactPhone,
      requirements: item.requirements,
      dimensions: item.dimensions,
    }
  })
}

function viewOrder(item: any) {
  if (item.orderId) {
    router.push({ name: 'OrderDetail', params: { id: item.orderId } })
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.page-container { padding: 20px 24px; }

.status-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.status-tab {
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  background: #f5f7fa;
  transition: all 0.25s;
  display: flex;
  align-items: center;
  gap: 6px;
  border: 2px solid transparent;
  &.active {
    background: #ecf5ff;
    color: #409eff;
    border-color: #409eff;
    font-weight: 700;
  }
  &:hover:not(.active) {
    background: #e4e7ed;
  }
  .count {
    background: rgba(0,0,0,0.08);
    padding: 1px 8px;
    border-radius: 10px;
    font-size: 11px;
    font-weight: 600;
  }
  &.active .count {
    background: rgba(64,158,255,0.15);
  }
}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
  .loading-spinner {
    width: 36px; height: 36px;
    border: 3px solid #e4e7ed;
    border-top-color: #409eff;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
    margin: 0 auto 16px;
  }
  .empty-icon { font-size: 48px; margin-bottom: 12px; }
  .empty-title { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 8px; }
  .empty-desc { font-size: 13px; }
}
@keyframes spin { to { transform: rotate(360deg); } }

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.todo-card {
  background: #fff;
  border-radius: 16px;
  padding: 18px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: all 0.25s;
  position: relative;
  border: 2px solid transparent;
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0,0,0,0.1);
    border-color: #d0e8ff;
  }
  &.priority-2 { border-left: 4px solid #f56c6c; }
  &.priority-3 { border-left: 4px solid #e6a23c; }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}
.customer-info { display: flex; gap: 10px; align-items: center; }
.customer-avatar {
  width: 42px; height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #67c23a);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700; font-size: 16px;
  flex-shrink: 0;
}
.customer-name { font-weight: 700; font-size: 15px; color: #303133; }
.customer-phone { font-size: 12px; color: #909399; margin-top: 2px; }
.card-badges { display: flex; gap: 4px; flex-wrap: wrap; justify-content: flex-end; }

.priority-badge {
  font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600;
  &.p1 { background: #f4f4f5; color: #909399; }
  &.p2 { background: #fef0f0; color: #f56c6c; }
  &.p3 { background: #fdf6ec; color: #e6a23c; }
}
.status-badge {
  font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600;
  &.s1 { background: #ecf5ff; color: #409eff; }
  &.s2 { background: #f0f9eb; color: #67c23a; }
  &.s3 { background: #fef0f0; color: #e6a23c; }
  &.s4 { background: #f0f9eb; color: #909399; }
}

.card-section { margin-bottom: 10px; }
.section-label { font-size: 11px; color: #909399; margin-bottom: 4px; font-weight: 600; }
.section-value {
  font-size: 13px; color: #606266; line-height: 1.5;
  &.dimensions { font-family: monospace; background: #f5f7fa; padding: 6px 10px; border-radius: 8px; }
  &.requirements {
    display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.card-quote {
  background: linear-gradient(135deg, #f0f9eb, #ecf5ff);
  border-radius: 8px; padding: 8px 12px; margin-bottom: 12px;
  display: flex; align-items: center; gap: 6px;
  .quote-label { font-size: 12px; color: #606266; }
  .quote-amount { font-size: 16px; font-weight: 700; color: #67c23a; }
}

.card-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 10px; border-top: 1px solid #f0f0f0;
}
.card-meta { display: flex; gap: 8px; align-items: center; }
.source-tag { font-size: 11px; background: #f5f7fa; color: #909399; padding: 2px 8px; border-radius: 6px; }
.date { font-size: 11px; color: #c0c4cc; }
.card-actions { display: flex; gap: 6px; }
.action-btn {
  border: none; cursor: pointer; padding: 5px 12px;
  border-radius: 16px; font-size: 12px; font-weight: 600; transition: all 0.2s;
  &.ai-btn { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
  &.ai-btn:hover { transform: scale(1.05); box-shadow: 0 3px 10px rgba(102,126,234,0.4); }
  &.convert-btn { background: linear-gradient(135deg, #67c23a, #85ce61); color: #fff; }
  &.convert-btn:hover { transform: scale(1.05); }
  &.view-btn { background: #ecf5ff; color: #409eff; }
}

.order-tag {
  position: absolute; bottom: 14px; right: 18px;
  font-size: 11px; color: #67c23a; font-weight: 600;
}

// 弹窗
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modal-container { background: #fff; border-radius: 16px; width: 90%; box-shadow: 0 20px 60px rgba(0,0,0,0.2); }
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px; border-bottom: 1px solid #f0f0f0;
  h3 { font-size: 16px; margin: 0; }
}
.modal-close { width: 30px; height: 30px; border: none; background: none; font-size: 22px; cursor: pointer; color: #909399; border-radius: 6px; &:hover { background: #f5f7fa; } }
.modal-body { padding: 20px 24px; }
.modal-footer { padding: 14px 24px; border-top: 1px solid #f0f0f0; display: flex; justify-content: flex-end; gap: 10px; }

.form-row { display: flex; gap: 16px; }
.form-group { margin-bottom: 14px; flex: 1; }
.form-label {
  display: block; font-size: 12px; color: #606266; margin-bottom: 6px; font-weight: 600;
  &.required::after { content: ' *'; color: #f56c6c; }
}
.form-input {
  width: 100%; border: 1px solid #dcdfe6; border-radius: 8px; padding: 8px 12px;
  font-size: 13px; outline: none; transition: border-color 0.2s; box-sizing: border-box;
  &:focus { border-color: #409eff; box-shadow: 0 0 0 2px rgba(64,158,255,0.1); }
  &[type="number"], &[type="text"], textarea { box-sizing: border-box; }
  textarea { resize: vertical; }
}
select.form-input { cursor: pointer; }

// 详情
.detail-section {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
}
.detail-customer { display: flex; gap: 12px; align-items: center; }
.detail-avatar {
  width: 48px; height: 48px; border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #67c23a);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700; font-size: 18px;
}
.detail-name { font-size: 18px; font-weight: 700; color: #303133; }
.detail-phone { font-size: 13px; color: #909399; margin-top: 2px; }
.detail-badges { display: flex; gap: 6px; flex-wrap: wrap; }
.detail-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 16px;
}
.detail-item {
  background: #f5f7fa; border-radius: 10px; padding: 12px 14px;
  &.full { grid-column: 1 / -1; }
}
.detail-label { font-size: 11px; color: #909399; margin-bottom: 6px; font-weight: 600; }
.detail-value { font-size: 14px; color: #303133; font-weight: 500; line-height: 1.5; }
.detail-value.quote { font-size: 20px; font-weight: 700; color: #67c23a; }
.detail-value.link { color: #409eff; cursor: pointer; text-decoration: underline; }
.detail-value.requirements { white-space: pre-wrap; }
.detail-timeline { padding-top: 12px; border-top: 1px solid #f0f0f0; display: flex; flex-direction: column; gap: 8px; }
.timeline-item { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #909399; }
.timeline-dot { width: 6px; height: 6px; background: #409eff; border-radius: 50%; }

.btn {
  border: none; padding: 8px 20px; border-radius: 8px; cursor: pointer; font-size: 13px;
  font-weight: 600; transition: all 0.2s;
  &.btn-primary { background: #409eff; color: #fff; &:hover { background: #66b1ff; } }
  &.btn-success { background: #67c23a; color: #fff; &:hover { background: #85ce61; } }
  &.btn-default { background: #f5f7fa; color: #606266; &:hover { background: #e4e7ed; } }
  &.btn-danger { background: #f56c6c; color: #fff; &:hover { background: #f78989; } }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
</style>
