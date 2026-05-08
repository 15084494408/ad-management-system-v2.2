<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <span class="page-title">📋 待办工作台</span>
      <div class="page-actions" v-if="activeTopTab === 'todo'">
        <el-button type="primary" @click="showAddDialog = true">
          <el-icon><Plus /></el-icon> 新增待办
        </el-button>
      </div>
    </div>

    <!-- 顶部大Tab：待办 / 待收款 / 未对账 -->
    <div class="top-tabs">
      <div
        class="top-tab"
        :class="{ active: activeTopTab === 'todo' }"
        @click="activeTopTab = 'todo'"
      >
        📋 需求待办
        <span class="count">{{ todoTotalCount }}</span>
      </div>
      <div
        class="top-tab"
        :class="{ active: activeTopTab === 'payment' }"
        @click="switchToPaymentTab"
      >
        📦 待处理订单
        <span class="count" :class="{ warn: pendingPaymentCount > 0 }">{{ pendingPaymentCount }}</span>
      </div>
      <div
        class="top-tab"
        :class="{ active: activeTopTab === 'bill' }"
        @click="switchToBillTab"
      >
        🧾 未对账账单
        <span class="count" :class="{ warn: pendingBillCount > 0 }">{{ pendingBillCount }}</span>
      </div>
    </div>

    <!-- ===== 待办事项 Tab ===== -->
    <template v-if="activeTopTab === 'todo'">
      <!-- 状态标签栏 -->
      <div class="status-tabs">
        <div
          class="status-tab"
          :class="{ active: filterStatus === null }"
          @click="filterStatus = null"
        >
          全部
          <span class="count">{{ todoTotalCount }}</span>
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
      <div v-if="todoLoading" class="loading-state">
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
    </template>

    <!-- ===== 待收款 Tab ===== -->
    <template v-if="activeTopTab === 'payment'">
      <!-- 加载状态 -->
      <div v-if="paymentLoading" class="loading-state">
        <div class="loading-spinner"></div>
        正在加载未完成订单...
      </div>

      <!-- 空状态 -->
      <div v-else-if="pendingPayments.length === 0" class="empty-state">
        <div class="empty-icon">✅</div>
        <div class="empty-title">暂无待处理订单</div>
        <div class="empty-desc">所有订单都已处理完成，干得漂亮！</div>
      </div>

      <!-- 未完成订单列表 -->
      <div v-else class="payment-list">
        <div class="payment-summary">
          <span>共 <strong>{{ pendingPayments.length }}</strong> 笔待处理订单</span>
          <span class="sep">|</span>
          <span>待收款 <strong class="text-danger">{{ unpaidCount }}</strong> 笔，</span>
          <span>合计 <strong class="text-danger">¥{{ formatMoney(totalUnpaid) }}</strong></span>
        </div>
        <div class="payment-grid">
          <div
            v-for="p in pendingPayments"
            :key="p.id"
            class="payment-card"
            :class="'main-status-' + getMainStatusClass(p.mainStatus)"
            @click="viewOrderDetail(p)"
          >
            <div class="payment-header">
              <div class="order-no">📄 {{ p.orderNo }}</div>
              <div class="main-status-tag" :class="'mst-' + getMainStatusClass(p.mainStatus)">
                {{ getMainStatusIcon(p.mainStatus) }} {{ p.mainStatus }}
              </div>
            </div>
            <div class="payment-customer">
              <div class="customer-avatar-sm">{{ getAvatar(p.customerName) }}</div>
              <span>{{ p.customerName }}</span>
            </div>
            <div class="payment-title" v-if="p.title">{{ p.title }}</div>

            <div v-if="hasUnpaidAmount(p)" class="payment-amounts">
              <div class="amount-row">
                <span class="amount-label">应收</span>
                <span class="amount-value">¥{{ formatMoney(p.totalAmount) }}</span>
              </div>
              <div class="amount-row">
                <span class="amount-label">已收</span>
                <span class="amount-value paid">¥{{ formatMoney(p.paidAmount) }}</span>
              </div>
              <div class="amount-row" v-if="p.roundingAmount && p.roundingAmount > 0">
                <span class="amount-label">抹零</span>
                <span class="amount-value discount">-¥{{ formatMoney(p.roundingAmount) }}</span>
              </div>
              <div class="amount-row" v-if="p.discountAmount && p.discountAmount > 0">
                <span class="amount-label">优惠</span>
                <span class="amount-value discount">-¥{{ formatMoney(p.discountAmount) }}</span>
              </div>
              <div class="amount-row total">
                <span class="amount-label">待收</span>
                <span class="amount-value unpaid">¥{{ formatMoney(p.unpaidAmount) }}</span>
              </div>
            </div>
            <div v-else class="payment-amounts">
              <div class="amount-row total">
                <span class="amount-label">订单金额</span>
                <span class="amount-value">¥{{ formatMoney(p.totalAmount) }}</span>
              </div>
              <div class="amount-row" v-if="p.paidAmount && p.paidAmount > 0">
                <span class="amount-label">已付清</span>
                <span class="amount-value paid">¥{{ formatMoney(p.paidAmount) }}</span>
              </div>
            </div>

            <div class="payment-footer">
              <span class="date">{{ formatDate(p.createTime) }}</span>
              <button
                v-if="hasUnpaidAmount(p)"
                class="action-btn collect-btn"
                @click.stop="collectPayment(p)"
              >
                💰 收款
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ===== 未对账账单 Tab ===== -->
    <template v-if="activeTopTab === 'bill'">
      <div v-if="billLoading" class="loading-state">
        <div class="loading-spinner"></div>
        正在加载未对账账单...
      </div>

      <div v-else-if="pendingBills.length === 0" class="empty-state">
        <div class="empty-icon">🧾</div>
        <div class="empty-title">暂无未对账账单</div>
        <div class="empty-desc">所有账单都已对账完成</div>
      </div>

      <div v-else class="bill-list">
        <div class="bill-summary">
          <span>共 <strong>{{ pendingBills.length }}</strong> 笔未对账账单</span>
          <span class="sep">|</span>
          <span>合计金额 <strong class="text-purple">¥{{ formatMoney(totalBillAmount) }}</strong></span>
        </div>
        <div class="bill-grid">
          <div
            v-for="b in pendingBills"
            :key="b.id"
            class="bill-card"
            :class="b.billType === 1 ? 'bill-factory' : 'bill-customer'"
            @click="viewBillDetail(b)"
          >
            <div class="bill-header">
              <div class="bill-no">📄 {{ b.billNo }}</div>
              <span class="bill-type-tag" :class="b.billType === 1 ? 'factory' : 'customer'">
                {{ b.billType === 1 ? '🏭 工厂' : '👤 客户' }}
              </span>
            </div>
            <div class="bill-customer-name">
              <div class="customer-avatar-sm bill-avatar">{{ getAvatar(b.factoryName) }}</div>
              <span>{{ b.factoryName || '-' }}</span>
            </div>
            <div class="bill-month" v-if="b.month">📅 {{ b.month }}</div>
            <div class="bill-amounts">
              <div class="amount-row total">
                <span class="amount-label">账单金额</span>
                <span class="amount-value bill-amount">¥{{ formatMoney(b.totalAmount) }}</span>
              </div>
              <div class="amount-row" v-if="b.paidAmount && b.paidAmount > 0">
                <span class="amount-label">已付</span>
                <span class="amount-value paid">¥{{ formatMoney(b.paidAmount) }}</span>
              </div>
            </div>
            <div class="bill-footer">
              <span class="date">{{ formatDate(b.createTime) }}</span>
              <button class="action-btn reconcile-btn" @click.stop="reconcileBill(b)">
                ✅ 对账
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

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

    <!-- 待办详情弹窗 -->
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

    <!-- 收款弹窗 -->
    <div class="modal-overlay" v-if="showPaymentDialog" @click.self="showPaymentDialog = false">
      <div class="modal-container" style="max-width: 500px;">
        <div class="modal-header">
          <h3>💰 登记收款</h3>
          <button class="modal-close" @click="showPaymentDialog = false">&times;</button>
        </div>
        <div class="modal-body" v-if="currentPaymentOrder">
          <div class="payment-order-info">
            <div>订单号：<strong>{{ currentPaymentOrder.orderNo }}</strong></div>
            <div>客户：<strong>{{ currentPaymentOrder.customerName }}</strong></div>
            <div class="payment-amounts-summary">
              <span>应收：¥{{ formatMoney(currentPaymentOrder.totalAmount) }}</span>
              <span>已收：¥{{ formatMoney(currentPaymentOrder.paidAmount) }}</span>
              <span class="unpaid">待收：¥{{ formatMoney(currentPaymentOrder.unpaidAmount) }}</span>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label required">收款金额</label>
            <input type="number" v-model="paymentForm.amount" class="form-input" :placeholder="'本次收款，最大 ¥' + formatMoney(currentPaymentOrder.unpaidAmount)" />
          </div>
          <div class="form-group">
            <label class="form-label">抹零结清</label>
            <label class="checkbox-label">
              <input type="checkbox" v-model="paymentForm.writeOff" />
              <span>差额作为抹零（{{ currentPaymentOrder.unpaidAmount > 0 ? '待收 ¥' + formatMoney(currentPaymentOrder.unpaidAmount) + ' 将记为抹零' : '' }}）</span>
            </label>
          </div>
          <div class="form-group">
            <label class="form-label">备注</label>
            <input type="text" v-model="paymentForm.remark" class="form-input" placeholder="备注（可选）" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showPaymentDialog = false">取消</button>
          <button class="btn btn-primary" @click="submitPayment" :disabled="paymentSubmitting">
            {{ paymentSubmitting ? '提交中...' : '确认收款' }}
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
import { orderApi } from '@/api/modules/order'
import { factoryApi } from '@/api/modules/factory'

const router = useRouter()
const activeTopTab = ref<'todo' | 'payment' | 'bill'>('todo')
const todoLoading = ref(false)
const paymentLoading = ref(false)
const billLoading = ref(false)
const filterStatus = ref<number | null>(null)
const showAddDialog = ref(false)
const showDetail = ref(false)
const showPaymentDialog = ref(false)
const editingItem = ref<any>(null)
const currentItem = ref<any>(null)
const currentPaymentOrder = ref<any>(null)
const saving = ref(false)
const paymentSubmitting = ref(false)
const allItems = ref<any[]>([])
const pendingPayments = ref<any[]>([])
const pendingPaymentCount = ref(0)
const pendingBills = ref<any[]>([])
const pendingBillCount = ref(0)

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

const paymentForm = reactive({
  amount: '',
  writeOff: false,
  remark: '',
})

const filteredItems = computed(() => {
  const items = filterStatus.value === null
    ? allItems.value
    : allItems.value.filter(it => it.status === filterStatus.value)
  return [...items].sort((a, b) => {
    if (a.status === 4 && b.status !== 4) return 1
    if (a.status !== 4 && b.status === 4) return -1
    return 0
  })
})

const todoTotalCount = computed(() => allItems.value.length)

const totalUnpaid = computed(() => {
  return pendingPayments.value.reduce((sum, p) => {
    const amt = typeof p.unpaidAmount === 'number' ? p.unpaidAmount : parseFloat(p.unpaidAmount || 0)
    return sum + (isNaN(amt) ? 0 : amt)
  }, 0)
})

const unpaidCount = computed(() => {
  return pendingPayments.value.filter(p => {
    const amt = typeof p.unpaidAmount === 'number' ? p.unpaidAmount : parseFloat(p.unpaidAmount || 0)
    return amt > 0
  }).length
})

const totalBillAmount = computed(() => {
  return pendingBills.value.reduce((sum, b) => {
    const amt = typeof b.totalAmount === 'number' ? b.totalAmount : parseFloat(b.totalAmount || 0)
    return sum + (isNaN(amt) ? 0 : amt)
  }, 0)
})

function hasUnpaidAmount(p: any) {
  const amt = typeof p.unpaidAmount === 'number' ? p.unpaidAmount : parseFloat(p.unpaidAmount || 0)
  return amt > 0
}

function getMainStatusClass(mainStatus: string) {
  switch (mainStatus) {
    case '待收款': return 'unpaid'
    case '部分收款': return 'partial'
    case '进行中': return 'processing'
    case '待处理': return 'pending'
    default: return 'pending'
  }
}

function getMainStatusIcon(mainStatus: string) {
  switch (mainStatus) {
    case '待收款': return '💰'
    case '部分收款': return '💴'
    case '进行中': return '🔧'
    case '待处理': return '📋'
    default: return '📋'
  }
}

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

async function loadTodoData() {
  todoLoading.value = true
  try {
    const res = await todoApi.getList()
    const data = res.data?.[0] || {}
    allItems.value = data.items || []
    const counts = data.statusCount || {}
    statusList.value.forEach(s => {
      s.count = counts['status' + s.value] || 0
    })
  } catch {
    ElMessage.error('加载待办失败')
  } finally {
    todoLoading.value = false
  }
}

async function loadPendingPayments() {
  paymentLoading.value = true
  try {
    const res = await orderApi.getPendingPayment()
    const data = res.data || res || []
    pendingPayments.value = Array.isArray(data) ? data : []
    pendingPaymentCount.value = pendingPayments.value.length
  } catch (e: any) {
    ElMessage.error('加载待处理订单失败：' + (e.message || '未知错误'))
    pendingPayments.value = []
    pendingPaymentCount.value = 0
  } finally {
    paymentLoading.value = false
  }
}

async function loadPendingBills() {
  billLoading.value = true
  try {
    const res = await factoryApi.getBills({ status: 1, current: 1, size: 50 })
    const records = res.data?.records || []
    pendingBills.value = records
    pendingBillCount.value = records.length
  } catch (e: any) {
    ElMessage.error('加载账单失败：' + (e.message || '未知错误'))
    pendingBills.value = []
    pendingBillCount.value = 0
  } finally {
    billLoading.value = false
  }
}

function switchToPaymentTab() {
  activeTopTab.value = 'payment'
  if (pendingPayments.value.length === 0 && !paymentLoading.value) {
    loadPendingPayments()
  }
}

function switchToBillTab() {
  activeTopTab.value = 'bill'
  if (pendingBills.value.length === 0 && !billLoading.value) {
    loadPendingBills()
  }
}

function openDetail(item: any) {
  currentItem.value = item
  showDetail.value = true
}

function viewOrderDetail(p: any) {
  router.push({ name: 'OrderDetail', params: { id: p.id } })
}

function viewBillDetail(b: any) {
  router.push({ name: b.billType === 1 ? 'FactoryBills' : 'CustomerBills' })
}

function collectPayment(p: any) {
  currentPaymentOrder.value = p
  paymentForm.amount = p.unpaidAmount ? String(Number(p.unpaidAmount).toFixed(2)) : ''
  paymentForm.writeOff = false
  paymentForm.remark = ''
  showPaymentDialog.value = true
}

async function reconcileBill(b: any) {
  try {
    await ElMessageBox.confirm(`确认对账「${b.billNo}」？`, '对账确认', { type: 'info' })
    await factoryApi.reconcile(b.id)
    ElMessage.success('对账成功')
    await loadPendingBills()
  } catch {}
}

async function submitPayment() {
  if (!currentPaymentOrder.value) return
  const amount = parseFloat(paymentForm.amount)
  if (isNaN(amount) || amount <= 0) {
    ElMessage.warning('请输入有效的收款金额')
    return
  }
  paymentSubmitting.value = true
  try {
    await orderApi.addPayment(currentPaymentOrder.value.id, {
      amount,
      writeOff: paymentForm.writeOff,
      remark: paymentForm.remark,
    })
    ElMessage.success('收款成功！')
    showPaymentDialog.value = false
    await loadPendingPayments()
  } catch (e: any) {
    ElMessage.error('收款失败：' + (e.message || '未知错误'))
  } finally {
    paymentSubmitting.value = false
  }
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
    loadTodoData()
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
    loadTodoData()
    ElMessage.success('已删除')
  } catch {}
}

async function generateQuote(item: any) {
  try {
    await todoApi.updateStatus(item.id, { status: 3 })
    await ElMessageBox.alert(
      '🤖 AI报价功能即将上线！\n\n' +
      '在此之前，您可以：\n' +
      '1. 手动估算报价金额\n' +
      '2. 点击「转为订单」手动创建正式报价',
      '功能预告'
    )
    loadTodoData()
  } catch {}
}

async function convertToOrder(item: any) {
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

onMounted(() => {
  loadTodoData()
})
</script>

<style scoped lang="scss">
.page-container { padding: 20px 24px; }

.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.page-title { font-size: 20px; font-weight: 700; color: #303133; }
.page-actions { display: flex; gap: 8px; }

/* 顶部大Tab */
.top-tabs {
  display: flex; gap: 8px; margin-bottom: 20px;
  padding: 6px; background: #f5f7fa; border-radius: 14px;
}
.top-tab {
  flex: 1; padding: 12px 16px; border-radius: 10px;
  cursor: pointer; font-size: 14px; font-weight: 600;
  color: #606266; background: transparent;
  transition: all 0.25s; text-align: center;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  &.active {
    background: #fff; color: #409eff;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  }
  .count {
    background: rgba(0,0,0,0.06); padding: 1px 10px;
    border-radius: 10px; font-size: 11px;
    &.warn { background: #fef0f0; color: #f56c6c; }
  }
}

/* 待办状态Tab */
.status-tabs {
  display: flex; gap: 8px; margin-bottom: 20px;
  padding: 12px 16px; background: #fff;
  border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.status-tab {
  padding: 8px 16px; border-radius: 20px; cursor: pointer;
  font-size: 13px; font-weight: 500; color: #606266;
  background: #f5f7fa; transition: all 0.25s;
  display: flex; align-items: center; gap: 6px;
  border: 2px solid transparent;
  &.active { background: #ecf5ff; color: #409eff; border-color: #409eff; font-weight: 700; }
  &:hover:not(.active) { background: #e4e7ed; }
  .count { background: rgba(0,0,0,0.08); padding: 1px 8px; border-radius: 10px; font-size: 11px; }
  &.active .count { background: rgba(64,158,255,0.15); }
}

.loading-state, .empty-state {
  text-align: center; padding: 60px 20px; color: #909399;
  .loading-spinner {
    width: 36px; height: 36px; border: 3px solid #e4e7ed;
    border-top-color: #409eff; border-radius: 50%;
    animation: spin 0.8s linear infinite; margin: 0 auto 16px;
  }
  .empty-icon { font-size: 48px; margin-bottom: 12px; }
  .empty-title { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 8px; }
  .empty-desc { font-size: 13px; }
}
@keyframes spin { to { transform: rotate(360deg); } }

/* 待办卡片 */
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.todo-card {
  background: #fff; border-radius: 16px; padding: 18px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06); cursor: pointer;
  transition: all 0.25s; position: relative; border: 2px solid transparent;
  &:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.1); border-color: #d0e8ff; }
  &.priority-2 { border-left: 4px solid #f56c6c; }
  &.priority-3 { border-left: 4px solid #e6a23c; }
}
.card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px; }
.customer-info { display: flex; gap: 10px; align-items: center; }
.customer-avatar {
  width: 42px; height: 42px; border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #67c23a);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700; font-size: 16px; flex-shrink: 0;
}
.customer-name { font-weight: 700; font-size: 15px; color: #303133; }
.customer-phone { font-size: 12px; color: #909399; margin-top: 2px; }
.card-badges { display: flex; gap: 4px; flex-wrap: wrap; justify-content: flex-end; }
.priority-badge { font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600;
  &.p1 { background: #f4f4f5; color: #909399; }
  &.p2 { background: #fef0f0; color: #f56c6c; }
  &.p3 { background: #fdf6ec; color: #e6a23c; }
}
.status-badge { font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600;
  &.s1 { background: #ecf5ff; color: #409eff; }
  &.s2 { background: #f0f9eb; color: #67c23a; }
  &.s3 { background: #fef0f0; color: #e6a23c; }
  &.s4 { background: #f0f9eb; color: #909399; }
}
.card-section { margin-bottom: 10px; }
.section-label { font-size: 11px; color: #909399; margin-bottom: 4px; font-weight: 600; }
.section-value { font-size: 13px; color: #606266; line-height: 1.5;
  &.dimensions { font-family: monospace; background: #f5f7fa; padding: 6px 10px; border-radius: 8px; }
  &.requirements { display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
}
.card-quote { background: linear-gradient(135deg, #f0f9eb, #ecf5ff); border-radius: 8px; padding: 8px 12px; margin-bottom: 12px; display: flex; align-items: center; gap: 6px;
  .quote-label { font-size: 12px; color: #606266; }
  .quote-amount { font-size: 16px; font-weight: 700; color: #67c23a; }
}
.card-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 10px; border-top: 1px solid #f0f0f0; }
.card-meta { display: flex; gap: 8px; align-items: center; }
.source-tag { font-size: 11px; background: #f5f7fa; color: #909399; padding: 2px 8px; border-radius: 6px; }
.date { font-size: 11px; color: #c0c4cc; }
.card-actions { display: flex; gap: 6px; }
.action-btn {
  border: none; cursor: pointer; padding: 5px 12px; border-radius: 16px;
  font-size: 12px; font-weight: 600; transition: all 0.2s;
  &.ai-btn { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
  &.ai-btn:hover { transform: scale(1.05); box-shadow: 0 3px 10px rgba(102,126,234,0.4); }
  &.convert-btn { background: linear-gradient(135deg, #67c23a, #85ce61); color: #fff; }
  &.convert-btn:hover { transform: scale(1.05); }
  &.view-btn { background: #ecf5ff; color: #409eff; }
  &.collect-btn { background: linear-gradient(135deg, #e6a23c, #f56c6c); color: #fff; }
  &.collect-btn:hover { transform: scale(1.05); }
  &.reconcile-btn { background: linear-gradient(135deg, #8b5cf6, #a78bfa); color: #fff; }
  &.reconcile-btn:hover { transform: scale(1.05); box-shadow: 0 3px 10px rgba(139,92,246,0.4); }
}
.order-tag { position: absolute; bottom: 14px; right: 18px; font-size: 11px; color: #67c23a; font-weight: 600; }

/* 待收款/未完成订单列表 */
.payment-list { margin-top: 4px; }
.payment-summary, .bill-summary {
  background: #fff; border-radius: 12px; padding: 14px 20px;
  margin-bottom: 16px; font-size: 14px; color: #606266;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  strong { color: #303133; }
  .sep { margin: 0 8px; color: #dcdfe6; }
  .text-danger { color: #f56c6c; }
  .text-purple { color: #8b5cf6; }
}
.payment-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 16px; }
.payment-card {
  background: #fff; border-radius: 14px; padding: 18px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06); cursor: pointer;
  transition: all 0.25s; border: 2px solid transparent;
  &:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.1); border-color: #d0e8ff; }
  &.main-status-unpaid { border-left: 4px solid #f56c6c; }
  &.main-status-partial { border-left: 4px solid #e6a23c; }
  &.main-status-processing { border-left: 4px solid #409eff; }
  &.main-status-pending { border-left: 4px solid #909399; }
}
.payment-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.order-no { font-size: 14px; font-weight: 700; color: #303133; }

.main-status-tag {
  font-size: 12px; padding: 3px 12px; border-radius: 12px; font-weight: 700;
  &.mst-unpaid { background: #fef0f0; color: #f56c6c; }
  &.mst-partial { background: #fdf6ec; color: #e6a23c; }
  &.mst-processing { background: #ecf5ff; color: #409eff; }
  &.mst-pending { background: #f4f4f5; color: #909399; }
}
.payment-customer { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 14px; font-weight: 500; color: #303133; }
.customer-avatar-sm {
  width: 30px; height: 30px; border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #67c23a);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700; font-size: 12px; flex-shrink: 0;
}
.payment-title { font-size: 12px; color: #909399; margin-bottom: 10px; }
.payment-amounts { background: #fafafa; border-radius: 10px; padding: 10px 12px; }
.amount-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 4px 0; font-size: 13px;
  &:not(:last-child) { border-bottom: 1px dashed #f0f0f0; }
  &.total { font-weight: 700; padding-top: 8px; border-top: 1px solid #e4e7ed; }
}
.amount-label { color: #909399; }
.amount-value { font-weight: 600; color: #303133;
  &.paid { color: #67c23a; }
  &.discount { color: #e6a23c; }
  &.unpaid { color: #f56c6c; font-size: 15px; }
  &.bill-amount { color: #8b5cf6; font-size: 15px; }
}
.payment-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 10px; margin-top: 10px; border-top: 1px solid #f0f0f0; }

/* 未对账账单 */
.bill-list { margin-top: 4px; }
.bill-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.bill-card {
  background: #fff; border-radius: 14px; padding: 18px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06); cursor: pointer;
  transition: all 0.25s; border: 2px solid transparent;
  &:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.1); border-color: #e0d4f7; }
  &.bill-factory { border-left: 4px solid #f59e0b; }
  &.bill-customer { border-left: 4px solid #8b5cf6; }
}
.bill-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.bill-no { font-size: 14px; font-weight: 700; color: #303133; }
.bill-type-tag {
  font-size: 12px; padding: 3px 12px; border-radius: 12px; font-weight: 600;
  &.factory { background: #fdf6ec; color: #e6a23c; }
  &.customer { background: #f3e8ff; color: #8b5cf6; }
}
.bill-customer-name { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 14px; font-weight: 500; color: #303133; }
.bill-avatar {
  background: linear-gradient(135deg, #8b5cf6, #a78bfa) !important;
}
.bill-month { font-size: 12px; color: #909399; margin-bottom: 10px; }
.bill-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 10px; margin-top: 10px; border-top: 1px solid #f0f0f0; }

/* 弹窗 */
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
.form-label { display: block; font-size: 12px; color: #606266; margin-bottom: 6px; font-weight: 600;
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
.checkbox-label { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #606266; cursor: pointer; }

/* 详情 */
.detail-section { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
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
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 16px; }
.detail-item { background: #f5f7fa; border-radius: 10px; padding: 12px 14px;
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

.payment-order-info { background: #f5f7fa; border-radius: 10px; padding: 14px; margin-bottom: 16px; font-size: 13px; color: #606266;
  strong { color: #303133; }
  .payment-amounts-summary { display: flex; gap: 16px; margin-top: 8px; flex-wrap: wrap;
    .unpaid { color: #f56c6c; font-weight: 700; }
  }
}

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
