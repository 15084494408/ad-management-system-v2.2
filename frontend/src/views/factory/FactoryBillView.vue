<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item">客户管理</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">工厂账单</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">🏭 工厂账单管理 <span class="v2-badge">V2.1</span></h1>
      <div class="page-actions">
        <button class="btn btn-default" @click="showExportReport = true">📊 对账报表</button>
        <button class="btn btn-primary" @click="showCreate = true">+ 生成账单</button>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:220px 1fr;gap:24px;align-items:start;">
      <!-- 工厂列表侧边栏 -->
      <div class="card" style="padding:12px;">
        <div
          class="config-sidebar-item"
          :class="{ active: selectedFactory === 'all' }"
          @click="selectFactory('all')"
        >
          <span>🏢 全部工厂</span>
        </div>
        <div
          v-for="f in factories" :key="f.id"
          class="config-sidebar-item"
          :class="{ active: selectedFactory === String(f.id) }"
          @click="selectFactory(String(f.id))"
        >
          <span>🏢 {{ f.factoryName || f.name }}</span>
        </div>
        <div style="margin-top:12px;padding:12px;background:var(--bg);border-radius:6px;">
          <div style="font-size:12px;color:var(--text2);margin-bottom:8px;">💡 快捷操作</div>
          <button class="btn btn-primary btn-sm" style="width:100%;font-size:13px;" @click="showCreate = true">+ 生成账单</button>
        </div>
      </div>

      <!-- 账单内容 -->
      <div class="card">
        <div class="card-header">
          <div class="card-title">{{ cardTitle }}</div>
          <div style="display:flex;gap:10px;">
            <select v-model="monthFilter" class="form-control" style="width:130px;font-size:13px;" @change="loadBills">
              <option value="">全部月份</option>
              <option v-for="m in months" :key="m" :value="m">{{ m }}</option>
            </select>
            <select v-model="statusFilter" class="form-control" style="width:120px;font-size:13px;" @change="loadBills">
              <option value="">全部状态</option>
              <option value="未对账">未对账</option>
              <option value="已对账">已对账</option>
              <option value="已结清">已结清</option>
            </select>
          </div>
        </div>

        <!-- 统计卡片 -->
        <div class="stats-grid" style="margin-bottom:20px;">
          <div class="stat-card">
            <div class="stat-icon blue">📋</div>
            <div class="stat-info">
              <h3>{{ bills.length }}</h3>
              <p>账单数量</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon orange">💰</div>
            <div class="stat-info">
              <h3>¥{{ stats.totalAmount }}</h3>
              <p>应付总额</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon green">✅</div>
            <div class="stat-info">
              <h3>¥{{ stats.totalPaid }}</h3>
              <p>已付金额</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon red">⏳</div>
            <div class="stat-info">
              <h3>¥{{ stats.totalUnpaid }}</h3>
              <p>未付金额</p>
            </div>
          </div>
        </div>

        <!-- 表格 -->
        <table class="data-table">
          <thead>
            <tr>
              <th>账单编号</th>
              <th>工厂客户</th>
              <th>账单月份</th>
              <th>账单金额</th>
              <th>已付金额</th>
              <th>未付金额</th>
              <th>账单状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="9" style="text-align:center;padding:40px;color:#909399;">
                <div class="loading-spinner" style="margin:0 auto 8px;"></div>加载中...
              </td>
            </tr>
            <tr v-for="b in bills" :key="b.id">
              <td><strong>{{ b.billNo || b.id }}</strong></td>
              <td>{{ b.factoryName || getFactoryName(b) }}</td>
              <td>{{ b.month }}</td>
              <td style="color:#e6a23c;font-weight:600;">¥{{ fmtMoney(b.totalAmount) }}</td>
              <td style="color:#67c23a;">¥{{ fmtMoney(b.paidAmount) }}</td>
              <td :style="{ color: getUnpaid(b) > 0 ? '#f56c6c' : '#909399', fontWeight: 600 }">
                ¥{{ fmtMoney(getUnpaid(b)) }}
              </td>
              <td>
                <span class="status-tag" :class="statusClass(b.status)">{{ statusLabel(b.status) }}</span>
              </td>
              <td style="color:#909399;font-size:12px;">{{ b.createTime || b.date || '-' }}</td>
              <td class="action-btns">
                <button class="action-btn view" @click="viewDetail(b)">查看</button>
                <button class="action-btn edit" @click="reconcile(b)">对账</button>
                <button class="action-btn view" @click="handleExport">导出</button>
              </td>
            </tr>
            <tr v-if="!loading && bills.length === 0">
              <td colspan="9" style="text-align:center;padding:40px;color:#c0c4cc;">暂无账单数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 生成账单弹窗 -->
    <div class="modal-overlay" :class="{ show: showCreate }" @click.self="showCreate = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">🏭 生成工厂账单</span>
          <button class="modal-close" @click="showCreate = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">工厂客户 *</label>
              <select v-model="createForm.factoryId" class="form-input">
                <option value="">请选择客户</option>
                <option v-for="f in factories" :key="f.id" :value="f.id">{{ f.factoryName || f.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">账单月份 *</label>
              <input v-model="createForm.month" type="month" class="form-input">
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">账单说明</label>
            <textarea v-model="createForm.remark" class="form-input" placeholder="可选，填写账单备注..."></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showCreate = false">取消</button>
          <button class="btn btn-primary" @click="createBill">💾 生成账单</button>
        </div>
      </div>
    </div>

    <!-- 对账弹窗 -->
    <div class="modal-overlay" :class="{ show: showReconcile }" @click.self="showReconcile = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">📊 账单对账</span>
          <button class="modal-close" @click="showReconcile = false">✕</button>
        </div>
        <div class="modal-body" v-if="reconcileData">
          <div style="text-align:center;padding:15px;background:#ecf5ff;border-radius:8px;margin-bottom:20px;">
            <div style="font-size:13px;color:#606266;">账单编号</div>
            <div style="font-size:18px;font-weight:600;color:#409eff;">{{ reconcileData.billNo || reconcileData.id }}</div>
            <div style="font-size:24px;font-weight:600;color:#e6a23c;margin-top:5px;">¥{{ fmtMoney(reconcileData.totalAmount) }}</div>
            <div style="font-size:12px;color:#909399;">应付总额</div>
          </div>
          <table style="width:100%;font-size:13px;">
            <tr>
              <td style="padding:10px;color:#909399;border-bottom:1px solid #f5f7fa;">订单总额</td>
              <td style="padding:10px;text-align:right;border-bottom:1px solid #f5f7fa;">¥{{ fmtMoney(reconcileData.totalAmount) }}</td>
            </tr>
            <tr>
              <td style="padding:10px;color:#909399;border-bottom:1px solid #f5f7fa;">已付款项</td>
              <td style="padding:10px;text-align:right;color:#67c23a;border-bottom:1px solid #f5f7fa;">¥{{ fmtMoney(reconcileData.paidAmount) }}</td>
            </tr>
            <tr>
              <td style="padding:10px;color:#909399;border-bottom:1px solid #f5f7fa;">本次应付</td>
              <td style="padding:10px;text-align:right;font-weight:600;border-bottom:1px solid #f5f7fa;">¥{{ fmtMoney(getUnpaid(reconcileData)) }}</td>
            </tr>
          </table>
          <div class="form-row" style="margin-top:20px;">
            <div class="form-group">
              <label class="form-label">付款金额</label>
              <input v-model="reconcileAmount" type="number" class="form-input" :placeholder="String(getUnpaid(reconcileData))" step="0.01">
            </div>
            <div class="form-group">
              <label class="form-label">付款方式</label>
              <select v-model="reconcileMethod" class="form-input">
                <option>银行转账</option>
                <option>现金</option>
                <option>支票</option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showReconcile = false">取消</button>
          <button class="btn btn-success" @click="doReconcile">✅ 确认对账</button>
        </div>
      </div>
    </div>

    <!-- 对账报表弹窗 -->
    <div class="modal-overlay" :class="{ show: showExportReport }" @click.self="showExportReport = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">📊 对账报表</span>
          <button class="modal-close" @click="showExportReport = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">工厂客户</label>
              <select class="form-input">
                <option value="">全部客户</option>
                <option v-for="f in factories" :key="f.id">{{ f.factoryName || f.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">报表周期</label>
              <select class="form-input">
                <option>本月</option>
                <option>本季度</option>
                <option>本年</option>
                <option>自定义</option>
              </select>
            </div>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:15px;">
            <input type="date" class="form-input" value="2026-01-01">
            <input type="date" class="form-input">
          </div>
          <div style="margin-top:20px;">
            <label style="font-size:13px;color:#606266;">导出内容</label>
            <div style="display:flex;flex-wrap:wrap;gap:10px;margin-top:10px;">
              <label style="display:flex;align-items:center;gap:5px;cursor:pointer;"><input type="checkbox" checked> 账单汇总</label>
              <label style="display:flex;align-items:center;gap:5px;cursor:pointer;"><input type="checkbox" checked> 订单明细</label>
              <label style="display:flex;align-items:center;gap:5px;cursor:pointer;"><input type="checkbox"> 付款记录</label>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showExportReport = false">取消</button>
          <button class="btn btn-primary" @click="showExportReport = false; ElMessage.info('报表导出中...')">⬇️ 导出报表</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { factoryApi } from '@/api'

const factories = ref<any[]>([])
const bills = ref<any[]>([])
const selectedFactory = ref('all')
const monthFilter = ref('')
const statusFilter = ref('')
const loading = ref(false)
const showCreate = ref(false)
const showReconcile = ref(false)
const showExportReport = ref(false)
const reconcileData = ref<any>(null)
const reconcileAmount = ref(0)
const reconcileMethod = ref('银行转账')

const createForm = ref({ factoryId: null as number | null, month: '', remark: '' })

const now = new Date()
const months = Array.from({ length: 6 }, (_, i) => {
  const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
  return `${d.getFullYear()}年${String(d.getMonth() + 1).padStart(2, '0')}月`
})

const cardTitle = computed(() => {
  if (selectedFactory.value === 'all') return '🏢 全部工厂账单'
  const f = factories.value.find(f => String(f.id) === selectedFactory.value)
  return `🏢 ${f?.factoryName || f?.name || '未知'} - 账单明细`
})

const stats = computed(() => {
  const t = bills.value.reduce((s: number, b: any) => s + Number(b.totalAmount || 0), 0)
  const p = bills.value.reduce((s: number, b: any) => s + Number(b.paidAmount || 0), 0)
  return {
    totalAmount: t.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
    totalPaid: p.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
    totalUnpaid: (t - p).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }
})

function getUnpaid(row: any): number {
  return Number((row.totalAmount || 0) - (row.paidAmount || 0))
}

function getFactoryName(b: any): string {
  if (selectedFactory.value !== 'all') {
    const f = factories.value.find(f => String(f.id) === selectedFactory.value)
    return f?.factoryName || f?.name || '-'
  }
  return b.factoryName || '-'
}

function statusClass(status: any): string {
  const map: Record<string, string> = { 1: 'status-pending', 2: 'status-active', 3: 'status-active', 4: 'status-completed', '未对账': 'status-pending', '已对账': 'status-active', '已结清': 'status-completed' }
  return map[status] || 'status-pending'
}

function statusLabel(status: any): string {
  const map: Record<string, string> = { 1: '未对账', 2: '已对账', 3: '部分付款', 4: '已结清' }
  return map[status] || status || '未知'
}

function fmtMoney(v: any): string {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function selectFactory(id: string) {
  selectedFactory.value = id
  await loadBills()
}

async function loadBills() {
  loading.value = true
  try {
    const params: any = { current: 1, size: 100 }
    if (selectedFactory.value !== 'all') params.factoryId = selectedFactory.value
    if (monthFilter.value) params.month = monthFilter.value
    if (statusFilter.value) {
      const sm: Record<string, number> = { '未对账': 1, '已对账': 2, '已结清': 4 }
      params.status = sm[statusFilter.value] || statusFilter.value
    }
    const res = await factoryApi.getBills(params)
    bills.value = res.data?.records || res.data?.list || []
  } catch {
    bills.value = []
  } finally {
    loading.value = false
  }
}

function viewDetail(row: any) {
  ElMessage.info(`账单 ${row.billNo || row.id} 详情（待开发）`)
}

function reconcile(row: any) {
  reconcileData.value = row
  reconcileAmount.value = getUnpaid(row)
  showReconcile.value = true
}

function doReconcile() {
  ElMessage.success('对账成功！')
  showReconcile.value = false
  loadBills()
}

async function createBill() {
  if (!createForm.value.factoryId || !createForm.value.month) {
    ElMessage.warning('请选择工厂并填写账单月份')
    return
  }
  try {
    await factoryApi.createBill(createForm.value)
    ElMessage.success('账单生成成功')
    showCreate.value = false
    createForm.value = { factoryId: null, month: '', remark: '' }
    loadBills()
  } catch (e: any) {
    ElMessage.error(e?.message || '生成失败')
  }
}

function handleExport() {
  ElMessage.info('导出功能开发中')
}

onMounted(async () => {
  try {
    const res = await factoryApi.getFactories()
    factories.value = res.data || []
  } catch {
    factories.value = []
  }
  await loadBills()
})
</script>

<style scoped>
.v2-badge {
  background: var(--success);
  color: #fff;
  font-size: 9px;
  padding: 2px 5px;
  border-radius: 3px;
  font-weight: normal;
}
.config-sidebar-item {
  padding: 12px 14px;
  border-radius: 6px;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--text2);
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  background: transparent;
  width: 100%;
  text-align: left;
}
.config-sidebar-item:hover {
  background: var(--bg);
  color: var(--primary);
}
.config-sidebar-item.active {
  background: linear-gradient(135deg, var(--primary), #66b1ff);
  color: #fff;
}
</style>
