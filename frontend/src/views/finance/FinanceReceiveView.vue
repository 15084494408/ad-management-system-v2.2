<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">💰 收款管理</span>
      <div class="page-actions">
        <el-button @click="exportData">
          <el-icon><Download /></el-icon> 导出
        </el-button>
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon> 登记收款
        </el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">💵</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ todayReceive?.toLocaleString() }}</div>
          <div class="stat-label">今日收款</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">✅</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ monthReceive?.toLocaleString() }}</div>
          <div class="stat-label">本月收款</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">⏳</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ pendingReceive?.toLocaleString() }}</div>
          <div class="stat-label">待收款</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">📋</div>
        <div class="stat-info">
          <div class="stat-value">{{ pendingCount }}</div>
          <div class="stat-label">待收款单数</div>
        </div>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="客户名称">
          <el-input v-model="searchForm.customerName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="收款状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="已收款" value="received" />
            <el-option label="待收款" value="pending" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="recordNo" label="流水编号" width="180" />
        <el-table-column prop="relatedName" label="关联单号" min-width="150">
          <template #default="{ row }">
            <el-link type="primary">{{ row.relatedName || '-' }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span class="received-amount">¥{{ row.amount?.toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="类别" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'income' ? 'success' : 'danger'">{{ row.category || row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="支付方式" width="110" />
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ String(row.createTime || '').replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="登记收款" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="关联订单" required>
          <el-select v-model="editForm.orderId" placeholder="请选择订单" filterable style="width: 100%;" @change="onOrderChange">
            <el-option v-for="o in orders" :key="o.id" :label="`${o.orderNo} - ${o.customerName}`" :value="o.id">
              <span>{{ o.orderNo }}</span>
              <span style="float: right; color: #8492a6; font-size: 12px;">待收 ¥{{ Math.max((o.totalAmount || 0) + (o.roundingAmount || 0) - (o.paidAmount || 0), 0).toFixed(2) }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="editForm.customerName" disabled />
        </el-form-item>
        <el-form-item label="订单金额">
          <el-input v-model="editForm.orderAmount" disabled />
        </el-form-item>
        <el-form-item label="待收金额">
          <el-input v-model="editForm.pendingAmount" disabled />
        </el-form-item>
        <el-form-item label="收款金额" required>
          <el-input-number v-model="editForm.receiveAmount" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="支付方式" required>
          <el-select v-model="editForm.paymentMethod" placeholder="请选择" style="width: 100%;">
            <el-option label="💵 现金" value="cash" />
            <el-option label="💚 微信支付" value="wechat" />
            <el-option label="💙 支付宝" value="alipay" />
            <el-option label="💳 银行卡转账" value="bank" />
            <el-option label="📱 其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="收款日期">
          <el-date-picker v-model="editForm.receiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" placeholder="收款备注..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveReceive">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="收款详情" width="550px">
      <el-descriptions :column="2" border v-if="detailData">
        <el-descriptions-item label="流水编号">{{ detailData.recordNo }}</el-descriptions-item>
        <el-descriptions-item label="关联单号">{{ detailData.relatedName }}</el-descriptions-item>
        <el-descriptions-item label="类别">
          <el-tag size="small" :type="detailData.type === 'income' ? 'success' : 'danger'">{{ detailData.category || detailData.type }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ String(detailData.createTime || '').replace('T', ' ').slice(0, 16) }}
        </el-descriptions-item>
        <el-descriptions-item label="收款金额">
          <span class="amount">¥{{ detailData.amount?.toLocaleString() }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ getPayMethodText(detailData.paymentMethod) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import { useFinanceStore } from '@/stores/finance'
import request from '@/api/request'
import { exportToExcel } from '@/utils/excelExport'

const financeStore = useFinanceStore()
// 全局财务数据变动时，自动刷新收款管理
watch(() => financeStore.lastFinanceUpdate, () => loadData())

const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const tableData = ref<any[]>([])
const orders = ref<any[]>([])
const detailData = ref<any>(null)

const searchForm = reactive({
  customerName: '',
  status: '',
  dateRange: [] as string[]
})

const pagination = reactive({ page: 1, size: 20, total: 0 })

const defaultForm = {
  orderId: null,
  customerName: '',
  orderAmount: 0,
  pendingAmount: 0,
  receiveAmount: 0,
  paymentMethod: '',
  receiveDate: '',
  remark: ''
}

const editForm = reactive<any>({ ...defaultForm })

const todayReceive = computed(() => {
  const today = new Date().toISOString().split('T')[0]
  return tableData.value.filter(t => String(t.createTime || '').startsWith(today)).reduce((sum, t) => sum + (t.amount || 0), 0)
})
const monthReceive = computed(() => tableData.value.reduce((sum, t) => sum + (t.amount || 0), 0))
const pendingReceive = computed(() => {
  // 待收款需要从订单维度计算，这里显示当前页 total
  return 0
})
const pendingCount = computed(() => 0)

const getPayMethodText = (method: string) => {
  const map: Record<string, string> = {
    cash: '💵 现金',
    wechat: '💚 微信',
    alipay: '💙 支付宝',
    bank: '💳 银行卡',
    other: '📱 其他'
  }
  return map[method] || method
}

const loadOrders = async () => {
  try {
    // ★ 修复：不限制 paymentStatus，让已部分付款的订单也能出现在下拉框中
    const res = await request.get('/orders', { params: { status: 1, current: 1, size: 200 } })
    const list = res.data?.records || res.data || []
    // 过滤出有未付余额的订单（已付清的不需要再登记收款）
    orders.value = list.filter((o: any) => {
      const paid = o.paidAmount || 0
      const total = o.totalAmount || 0
      const rounding = o.roundingAmount || 0
      return total + rounding > paid
    })
  } catch (e) {
    orders.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = { current: pagination.page, size: pagination.size, type: 'income' }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await request.get('/finance/records', { params })
    const data = res.data
    // 后端返回 PageResult 结构 {total, records, current, size}
    if (data.records) {
      tableData.value = data.records
      pagination.total = data.total
    } else if (data.list) {
      tableData.value = data.list
      pagination.total = data.total
    } else if (Array.isArray(data)) {
      tableData.value = data
      pagination.total = data.length
    }
  } catch (e) {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.customerName = ''
  searchForm.status = ''
  searchForm.dateRange = []
  loadData()
}

const showAddDialog = () => {
  Object.assign(editForm, defaultForm)
  editForm.receiveDate = new Date().toISOString().split('T')[0]
  dialogVisible.value = true
}

const onOrderChange = (orderId: number) => {
  const order = orders.value.find(o => o.id === orderId)
  if (order) {
    const total = order.totalAmount || 0
    const paid = order.paidAmount || 0
    const rounding = order.roundingAmount || 0
    const remaining = Math.max(total + rounding - paid, 0)
    editForm.customerName = order.customerName
    editForm.orderAmount = total
    editForm.pendingAmount = remaining
    editForm.receiveAmount = remaining
  }
}

const saveReceive = async () => {
  if (!editForm.orderId || !editForm.receiveAmount) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    // ★ 修复：调用订单收款接口（会自动同步写 fin_record）
    await request.post(`/orders/${editForm.orderId}/payment`, {
      amount: editForm.receiveAmount,
      method: editForm.paymentMethod,
      remark: editForm.remark
    })
    ElMessage.success('收款登记成功')
    dialogVisible.value = false
    loadOrders()
    loadData()
  } catch (e) {
    ElMessage.error('收款登记失败')
  }
}

const viewDetail = (row: any) => {
  detailData.value = row
  detailVisible.value = true
}

const exportData = () => {
  if (!tableData.value.length) { ElMessage.warning('暂无数据可导出'); return }
  exportToExcel({
    filename: '收款记录',
    header: ['流水编号', '关联单号', '类别', '金额(¥)', '支付方式', '备注', '创建时间'],
    data: tableData.value.map(r => [
      r.recordNo || '-',
      r.relatedName || '-',
      r.category || r.type || '-',
      r.amount,
      r.paymentMethod || '-',
      r.remark || '-',
      String(r.createTime || '').replace('T', ' ').slice(0, 16)
    ]),
  })
}

onMounted(() => {
  loadOrders()
  loadData()
})
</script>

<style scoped lang="scss">
.received-amount {
  color: #67c23a;
  font-weight: 600;
}

.pending-amount {
  color: #e6a23c;
  font-weight: 600;
}

.amount {
  color: #e6a23c;
  font-weight: 600;
}
</style>
