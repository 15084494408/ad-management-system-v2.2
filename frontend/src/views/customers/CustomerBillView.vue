<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item">客户管理</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">客户账单</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">📋 客户账单</h1>
      <div class="page-actions">
        <button class="btn btn-outline" @click="handleExport">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          导出
        </button>
        <button class="btn btn-generate" @click="openGenerateModal" :disabled="generating">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="18" x2="12" y2="12"/><line x1="9" y1="15" x2="15" y2="15"/></svg>
          {{ generating ? '生成中...' : '从订单生成' }}
        </button>
        <button class="btn btn-primary" @click="openCreateModal">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新建账单
        </button>
      </div>
    </div>

    <!-- 搜索卡片 -->
    <div class="search-card">
      <div class="search-form">
        <div class="form-group">
          <label class="form-label">客户名称</label>
          <select v-model="searchForm.customerId" class="form-input" @change="loadBills">
            <option value="">全部客户</option>
            <option v-for="c in customers" :key="c.id" :value="c.id">
              {{ c.customerName || c.name }}
            </option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">月份</label>
          <input v-model="searchForm.month" type="month" class="form-input" @change="onMonthChange">
        </div>
        <div class="form-group">
          <label class="form-label">状态</label>
          <select v-model="searchForm.status" class="form-input" @change="loadBills">
            <option value="">全部状态</option>
            <option :value="1">未对账</option>
            <option :value="2">已对账</option>
            <option :value="3">部分付款</option>
            <option :value="4">已结清</option>
          </select>
        </div>
        <div class="form-group form-actions">
          <button class="btn btn-ghost" @click="resetSearch">重置</button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon blue">📋</div>
        <div class="stat-info">
          <h3>{{ bills.length }}</h3>
          <p>账单总数</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">💰</div>
        <div class="stat-info">
          <h3>¥{{ totalAmount.toLocaleString() }}</h3>
          <p>应收总额</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">📊</div>
        <div class="stat-info">
          <h3>¥{{ paidAmount.toLocaleString() }}</h3>
          <p>已收总额</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">⚠️</div>
        <div class="stat-info">
          <h3>¥{{ unpaidAmount.toLocaleString() }}</h3>
          <p>未收总额</p>
        </div>
      </div>
    </div>

    <!-- 表格卡片 -->
    <div class="table-card">
      <table class="data-table">
        <thead>
          <tr>
            <th class="col-id">编号</th>
            <th class="col-name">客户名称</th>
            <th class="col-month">月份</th>
            <th class="col-amount">应收金额</th>
            <th class="col-paid">已付金额</th>
            <th class="col-status">状态</th>
            <th class="col-remark">备注</th>
            <th class="col-time">创建时间</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="9" class="loading-cell">
              <div class="loading-spinner"></div>
              <span>加载中...</span>
            </td>
          </tr>
          <tr v-for="row in bills" :key="row.id" class="data-row">
            <td class="col-id">
              <span class="id-badge">{{ row.billNo }}</span>
            </td>
            <td class="col-name">
              <div class="customer-name">
                <span class="avatar avatar-customer">{{ (row.factoryName || '?').charAt(0) }}</span>
                <strong>{{ row.factoryName }}</strong>
              </div>
            </td>
            <td class="col-month">{{ row.month || '—' }}</td>
            <td class="col-amount">
              <span class="amount">¥{{ Number(row.totalAmount || 0).toLocaleString() }}</span>
            </td>
            <td class="col-paid">
              <span class="paid">{{ row.paidAmount ? '¥' + Number(row.paidAmount).toLocaleString() : '—' }}</span>
            </td>
            <td class="col-status">
              <span class="badge" :class="getStatusClass(row.status)">{{ getStatusText(row.status) }}</span>
            </td>
            <td class="col-remark">
              <span class="text-muted">{{ row.remark || '—' }}</span>
            </td>
            <td class="col-time">
              <span class="text-muted">{{ formatTime(row.createTime) }}</span>
            </td>
            <td class="col-action">
              <div class="action-group">
                <button class="btn-icon btn-detail" @click="openDetailModal(row)" title="详情">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                </button>
                <button class="btn-icon btn-view" @click="openEditModal(row)" title="编辑">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </button>
                <button class="btn-icon btn-pay" @click="openPayModal(row)" title="收款">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
                </button>
                <button class="btn-icon btn-delete" @click="confirmDelete(row)" title="删除">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && bills.length === 0">
            <td colspan="9" class="empty-cell">
              <div class="empty-state">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                <p>暂无账单数据</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <div class="pagination-info">
          共 <span class="highlight">{{ total }}</span> 条记录
        </div>
        <div class="pagination">
          <button class="page-btn" :disabled="current <= 1" @click="changePage(current - 1)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
          </button>
          <button
            v-for="p in pageButtons" :key="p"
            class="page-btn" :class="{ active: p === current, ellipsis: p === '...' }"
            :disabled="p === '...'"
            @click="p !== '...' && changePage(p as number)"
          >{{ p }}</button>
          <button class="page-btn" :disabled="current >= totalPages" @click="changePage(current + 1)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 新建/编辑账单弹窗 ========== -->
    <div class="modal-overlay" :class="{ show: showCreate }" @click.self="showCreate = false">
      <div class="modal" style="max-width:820px;">
        <div class="modal-header">
          <span class="modal-title">{{ editForm.id ? '编辑账单' : '新建客户账单' }}</span>
          <button class="modal-close" @click="showCreate = false">✕</button>
        </div>
        <div class="modal-body">
          <!-- 基本信息 -->
          <div class="section-title">基本信息</div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">客户 *</label>
              <select v-model="editForm.customerId" class="form-input" :disabled="!!editForm.id">
                <option value="">请选择客户</option>
                <option v-for="c in customers" :key="c.id" :value="c.id">
                  {{ c.customerName || c.name }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">月份</label>
              <input v-model="editForm.month" type="month" class="form-input">
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">状态</label>
              <select v-model="editForm.status" class="form-input">
                <option :value="1">未对账</option>
                <option :value="2">已对账</option>
                <option :value="3">部分付款</option>
                <option :value="4">已结清</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">备注</label>
              <input v-model="editForm.remark" type="text" class="form-input" placeholder="可选备注信息">
            </div>
          </div>

          <!-- 物料明细 -->
          <div class="section-title" style="margin-top:20px; display:flex; align-items:center; justify-content:space-between;">
            <span>📦 物料明细</span>
            <button class="btn btn-sm btn-add-item" @click="addMaterialRow">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              添加物料
            </button>
          </div>

          <!-- 物料明细表 -->
          <div class="material-table-wrapper" v-if="materialRows.length > 0">
            <table class="material-table">
              <thead>
                <tr>
                  <th style="width:28%;">物料名称</th>
                  <th style="width:18%;">规格</th>
                  <th style="width:12%;">数量</th>
                  <th style="width:10%;">单位</th>
                  <th style="width:14%;">单价</th>
                  <th style="width:14%;">小计</th>
                  <th style="width:40px;"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, idx) in materialRows" :key="idx">
                  <td>
                    <select v-model="row.materialId" class="table-input" @change="onMaterialSelect(row)">
                      <option value="">选择物料</option>
                      <option v-for="m in materials" :key="m.id" :value="m.id">
                        {{ m.name }} ({{ m.spec || m.code || '—' }})
                      </option>
                    </select>
                  </td>
                  <td><input v-model="row.spec" class="table-input" placeholder="规格"></td>
                  <td><input v-model.number="row.quantity" type="number" min="0" class="table-input" placeholder="0"></td>
                  <td><input v-model="row.unit" class="table-input" placeholder="个"></td>
                  <td><input v-model.number="row.unitPrice" type="number" step="0.01" min="0" class="table-input" placeholder="0.00"></td>
                  <td>
                    <span class="subtotal">¥{{ calcSubtotal(row).toFixed(2) }}</span>
                  </td>
                  <td>
                    <button class="btn-remove" @click="removeMaterialRow(idx)" title="移除">✕</button>
                  </td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="5" class="total-label">物料合计</td>
                  <td class="total-value">¥{{ materialTotal.toFixed(2) }}</td>
                  <td></td>
                </tr>
              </tfoot>
            </table>
          </div>
          <div v-else class="empty-material">
            <p>暂未添加物料，点击「添加物料」选择要记账的物料</p>
          </div>

          <!-- 应收金额（自动汇总或手动修改） -->
          <div class="form-row full" style="margin-top:16px;">
            <div class="form-group">
              <label class="form-label">应收金额（物料合计自动填入，也可手动修改）</label>
              <input v-model="editForm.totalAmount" type="number" step="0.01" min="0" class="form-input amount-input" placeholder="0.00">
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="showCreate = false">取消</button>
          <button class="btn btn-success" @click="saveBill" :disabled="savingBill">
            {{ savingBill ? '保存中...' : (editForm.id ? '保存修改' : '创建账单') }}
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 详情弹窗 ========== -->
    <div class="modal-overlay" :class="{ show: showDetail }" @click.self="showDetail = false">
      <div class="modal" style="max-width:750px;">
        <div class="modal-header">
          <span class="modal-title">📋 账单详情</span>
          <button class="modal-close" @click="showDetail = false">✕</button>
        </div>
        <div class="modal-body" v-if="detailData">
          <!-- 基本信息 -->
          <div class="detail-info-grid">
            <div class="detail-info-item">
              <span class="detail-label">账单编号</span>
              <span class="detail-value id-text">{{ detailData.billNo }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">客户名称</span>
              <span class="detail-value">{{ detailData.factoryName }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">月份</span>
              <span class="detail-value">{{ detailData.month || '—' }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">状态</span>
              <span class="badge" :class="getStatusClass(detailData.status)">{{ getStatusText(detailData.status) }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">应收金额</span>
              <span class="detail-value amount-text">¥{{ Number(detailData.totalAmount || 0).toLocaleString() }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">已付金额</span>
              <span class="detail-value paid-text">¥{{ Number(detailData.paidAmount || 0).toLocaleString() }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">待收金额</span>
              <span class="detail-value unpaid-text">¥{{ Math.max(Number(detailData.totalAmount || 0) - Number(detailData.paidAmount || 0), 0).toLocaleString() }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">创建时间</span>
              <span class="detail-value">{{ formatTime(detailData.createTime) }}</span>
            </div>
          </div>

          <!-- 备注 -->
          <div class="detail-remark" v-if="detailData.remark">
            <span class="detail-label">备注：</span>{{ detailData.remark }}
          </div>

          <!-- 物料明细 -->
          <div class="detail-section-title">📦 物料明细</div>
          <div v-if="detailLoading" class="detail-loading">加载明细中...</div>
          <div v-else-if="detailItems.length > 0">
            <table class="detail-material-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>物料名称</th>
                  <th>规格</th>
                  <th>数量</th>
                  <th>单位</th>
                  <th>单价</th>
                  <th>小计</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in detailItems" :key="item.id || idx">
                  <td class="text-muted">{{ idx + 1 }}</td>
                  <td><strong>{{ item.itemName || '—' }}</strong></td>
                  <td>{{ item.spec || '—' }}</td>
                  <td>{{ item.quantity || 0 }}</td>
                  <td>{{ item.unit || '—' }}</td>
                  <td class="price-cell">¥{{ Number(item.unitPrice || 0).toFixed(2) }}</td>
                  <td class="subtotal-cell">¥{{ Number(item.amount || 0).toFixed(2) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="6" class="total-label">合计</td>
                  <td class="total-cell">¥{{ detailTotal.toFixed(2) }}</td>
                </tr>
              </tfoot>
            </table>
          </div>
          <div v-else class="empty-material">
            <p>该账单暂无物料明细</p>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="showDetail = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- ========== 收款弹窗 ========== -->
    <div class="modal-overlay" :class="{ show: showPay }" @click.self="showPay = false">
      <div class="modal" style="max-width:420px;">
        <div class="modal-header" style="background:linear-gradient(135deg, #11998e 0%, #38ef7d 100%);">
          <span class="modal-title">收款登记</span>
          <button class="modal-close" @click="showPay = false">✕</button>
        </div>
        <div class="modal-body" v-if="payTarget">
          <div class="pay-info">
            <div class="pay-info-item">
              <span class="pay-label">客户</span>
              <span class="pay-value">{{ payTarget.factoryName }}</span>
            </div>
            <div class="pay-info-item">
              <span class="pay-label">应收金额</span>
              <span class="pay-value amount">¥{{ Number(payTarget.totalAmount || 0).toLocaleString() }}</span>
            </div>
            <div class="pay-info-item">
              <span class="pay-label">已付金额</span>
              <span class="pay-value paid">¥{{ Number(payTarget.paidAmount || 0).toLocaleString() }}</span>
            </div>
            <div class="pay-info-item">
              <span class="pay-label">待收金额</span>
              <span class="pay-value" style="color:#f56c6c;font-weight:700;">
                ¥{{ Math.max(Number(payTarget.totalAmount || 0) - Number(payTarget.paidAmount || 0), 0).toLocaleString() }}
              </span>
            </div>
          </div>
          <div class="form-group" style="margin-top:20px;">
            <label class="form-label">本次收款金额 *</label>
            <input v-model="payAmount" type="number" step="0.01" min="0.01" class="form-input" placeholder="请输入收款金额">
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="showPay = false">取消</button>
          <button class="btn btn-success" @click="submitPay">确认收款</button>
        </div>
      </div>
    </div>

    <!-- ========== 从订单生成账单弹窗 ========== -->
    <div class="modal-overlay" :class="{ show: showGenerate }" @click.self="showGenerate = false">
      <div class="modal" style="max-width:460px;">
        <div class="modal-header" style="background:linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
          <span class="modal-title">从订单自动生成账单</span>
          <button class="modal-close" @click="showGenerate = false">✕</button>
        </div>
        <div class="modal-body">
          <p class="generate-desc">选择客户和月份，系统将自动汇总该客户该月所有未取消的订单，生成客户账单和明细。</p>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">客户 *</label>
              <select v-model="generateForm.customerId" class="form-input">
                <option value="">请选择客户</option>
                <option v-for="c in customers" :key="c.id" :value="c.id">
                  {{ c.customerName || c.name }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">月份 *</label>
              <input v-model="generateForm.month" type="month" class="form-input">
            </div>
          </div>
          <div class="generate-tip" v-if="generatePreview">
            <div class="tip-icon">💡</div>
            <div class="tip-text">{{ generatePreview }}</div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="showGenerate = false">取消</button>
          <button class="btn btn-generate" @click="submitGenerate" :disabled="generating || !generateForm.customerId || !generateForm.month">
            {{ generating ? '生成中...' : '确认生成' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { exportToExcel } from '@/utils/excelExport'

const route = useRoute()

// ========== 数据 ==========
const bills = ref<any[]>([])
const customers = ref<any[]>([])
const materials = ref<any[]>([])
const loading = ref(false)
const savingBill = ref(false)
const total = ref(0)
const current = ref(1)
const size = ref(10)
const showCreate = ref(false)
const showPay = ref(false)
const showDetail = ref(false)
const payTarget = ref<any>(null)
const payAmount = ref<number | null>(null)

// 从订单生成
const showGenerate = ref(false)
const generating = ref(false)
const generateForm = reactive({
  customerId: '' as any,
  month: '',
})
const generatePreview = ref('')

// 详情
const detailData = ref<any>(null)
const detailItems = ref<any[]>([])
const detailLoading = ref(false)

// 从路由参数获取预选客户ID
const preselectedCustomerId = computed(() => route.query.customerId as string || '')

const searchForm = reactive({
  customerId: '',
  month: '',
  status: '' as any,
})

const editForm = reactive({
  id: null as number | null,
  customerId: '' as any,
  month: '',
  totalAmount: '' as any,
  status: 1 as number,
  remark: '',
})

// ========== 物料明细行 ==========
interface MaterialRow {
  materialId: number | string
  itemName: string
  spec: string
  quantity: number
  unit: string
  unitPrice: number
}

const materialRows = ref<MaterialRow[]>([])

function addMaterialRow() {
  materialRows.value.push({
    materialId: '',
    itemName: '',
    spec: '',
    quantity: 1,
    unit: '个',
    unitPrice: 0,
  })
}

function removeMaterialRow(idx: number) {
  materialRows.value.splice(idx, 1)
  recalcMaterialTotal()
}

function onMaterialSelect(row: MaterialRow) {
  const m = materials.value.find(mat => mat.id === Number(row.materialId))
  if (m) {
    row.itemName = m.name || ''
    row.spec = m.spec || m.code || ''
    row.unit = m.unit || '个'
    row.unitPrice = Number(m.price) || 0
  }
}

function calcSubtotal(row: MaterialRow): number {
  return (Number(row.quantity) || 0) * (Number(row.unitPrice) || 0)
}

const materialTotal = computed(() => {
  return materialRows.value.reduce((sum, r) => sum + calcSubtotal(r), 0)
})

function recalcMaterialTotal() {
  // 自动填入应收金额
  const total = materialRows.value.reduce((sum, r) => sum + calcSubtotal(r), 0)
  if (total > 0) {
    editForm.totalAmount = total.toFixed(2)
  }
}

// 监听物料行变化自动计算
watch(() => materialRows.value.map(r => `${r.quantity}-${r.unitPrice}`).join(','), () => {
  recalcMaterialTotal()
})

// 详情合计
const detailTotal = computed(() => {
  return detailItems.value.reduce((sum: number, item: any) => {
    return sum + Number(item.amount || 0)
  }, 0)
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

const pageButtons = computed(() => {
  const pages: (number | string)[] = []
  const tp = totalPages.value
  const c = current.value
  if (tp <= 5) {
    for (let i = 1; i <= tp; i++) pages.push(i)
  } else {
    pages.push(1)
    if (c > 3) pages.push('...')
    for (let i = Math.max(2, c - 1); i <= Math.min(tp - 1, c + 1); i++) pages.push(i)
    if (c < tp - 2) pages.push('...')
    pages.push(tp)
  }
  return pages
})

// ========== 统计 ==========
const totalAmount = computed(() => bills.value.reduce((s, b) => s + Number(b.totalAmount || 0), 0))
const paidAmount = computed(() => bills.value.reduce((s, b) => s + Number(b.paidAmount || 0), 0))
const unpaidAmount = computed(() => Math.max(totalAmount.value - paidAmount.value, 0))

// ========== 状态工具 ==========
const statusMap: Record<number, { text: string; cls: string }> = {
  1: { text: '未对账', cls: 'badge-warning' },
  2: { text: '已对账', cls: 'badge-info' },
  3: { text: '部分付款', cls: 'badge-orange' },
  4: { text: '已结清', cls: 'badge-success' },
}
function getStatusText(status: number) { return statusMap[status]?.text || '未知' }
function getStatusClass(status: number) { return statusMap[status]?.cls || 'badge-default' }

function formatTime(t: string) {
  if (!t) return '—'
  return t.replace('T', ' ').slice(0, 16)
}

function onMonthChange() {
  if (searchForm.month) {
    const [y, m] = searchForm.month.split('-')
    searchForm.month = `${y}年${m}月`
  }
  loadBills()
}

// ========== 加载数据 ==========
async function loadBills() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      current: current.value,
      size: size.value,
      billType: 2,
    }
    if (searchForm.customerId) params.factoryId = searchForm.customerId
    if (searchForm.month) params.month = searchForm.month
    if (searchForm.status !== '') params.status = searchForm.status

    const res = await request.get('/factory/bills', { params })
    const data = res.data
    bills.value = data?.records || data?.list || []
    total.value = data?.total || bills.value.length
  } catch {
    bills.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadCustomers() {
  try {
    const res = await request.get('/customers', { params: { current: 1, size: 200 } })
    customers.value = res.data?.records || []
  } catch {
    customers.value = []
  }
}

async function loadMaterials() {
  try {
    const res = await request.get('/material/all')
    materials.value = res.data || []
  } catch {
    materials.value = []
  }
}

function changePage(p: number) {
  current.value = p
  loadBills()
}

function resetSearch() {
  searchForm.customerId = ''
  searchForm.month = ''
  searchForm.status = ''
  current.value = 1
  loadBills()
}

// ========== 新建/编辑 ==========
function openCreateModal() {
  editForm.id = null
  editForm.customerId = ''
  editForm.month = ''
  editForm.totalAmount = ''
  editForm.status = 1
  editForm.remark = ''
  materialRows.value = []
  // 预选客户
  if (searchForm.customerId) {
    editForm.customerId = searchForm.customerId
  }
  if (!editForm.customerId && preselectedCustomerId.value) {
    editForm.customerId = Number(preselectedCustomerId.value)
    searchForm.customerId = preselectedCustomerId.value
  }
  showCreate.value = true
}

async function openEditModal(row: any) {
  editForm.id = row.id
  editForm.customerId = row.customerId
  editForm.month = row.month ? row.month.replace('年', '-').replace('月', '') : ''
  editForm.totalAmount = row.totalAmount
  editForm.status = row.status
  editForm.remark = row.remark || ''
  // 加载已有明细
  materialRows.value = []
  showCreate.value = true
  try {
    const res = await request.get(`/factory/bills/${row.id}/details`)
    const items = res.data || []
    materialRows.value = items.map((d: any) => ({
      materialId: '',
      itemName: d.itemName || '',
      spec: d.spec || '',
      quantity: Number(d.quantity) || 0,
      unit: d.unit || '',
      unitPrice: Number(d.unitPrice) || 0,
    }))
  } catch {
    // 没有明细也不报错
  }
}

async function saveBill() {
  if (!editForm.customerId) {
    ElMessage.warning('请选择客户')
    return
  }

  // 转换月份格式
  let month = ''
  if (editForm.month) {
    if (editForm.month.includes('年')) {
      month = editForm.month
    } else {
      const [y, m] = editForm.month.split('-')
      month = `${y}年${m}月`
    }
  }

  savingBill.value = true
  try {
    if (editForm.id) {
      // 编辑模式：更新账单基本信息
      const payload: any = {
        customerId: editForm.customerId,
        month,
        status: editForm.status,
        remark: editForm.remark,
        billType: 2,
      }
      await request.put(`/factory/bills/${editForm.id}`, payload)

      // 如果有物料明细，先删除旧明细再批量添加
      if (materialRows.value.length > 0) {
        await request.delete(`/factory/bills/${editForm.id}/details/batch-cleanup`)
        const details = materialRows.value.filter(r => r.itemName || r.quantity > 0).map(r => ({
          itemName: r.itemName,
          spec: r.spec,
          quantity: Number(r.quantity) || 0,
          unit: r.unit,
          unitPrice: Number(r.unitPrice) || 0,
          calcMode: 1, // 按数量计价
        }))
        if (details.length > 0) {
          await request.post(`/factory/bills/${editForm.id}/details/batch`, details)
        }
      }

      ElMessage.success('修改成功')
    } else {
      // 新建模式：先创建账单
      const payload: any = {
        customerId: editForm.customerId,
        month,
        totalAmount: materialTotal.value > 0 ? materialTotal.value : (Number(editForm.totalAmount) || 0),
        status: editForm.status,
        remark: editForm.remark,
        billType: 2,
      }
      const billRes = await request.post('/factory/bills', payload)
      const billId = billRes.data

      // 批量添加物料明细
      if (billId && materialRows.value.length > 0) {
        const details = materialRows.value.filter(r => r.itemName || r.quantity > 0).map(r => ({
          itemName: r.itemName,
          spec: r.spec,
          quantity: Number(r.quantity) || 0,
          unit: r.unit,
          unitPrice: Number(r.unitPrice) || 0,
          calcMode: 1,
        }))
        if (details.length > 0) {
          await request.post(`/factory/bills/${billId}/details/batch`, details)
        }
      }

      ElMessage.success('创建成功')
    }
    showCreate.value = false
    loadBills()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    savingBill.value = false
  }
}

// ========== 详情 ==========
async function openDetailModal(row: any) {
  detailData.value = row
  detailItems.value = []
  detailLoading.value = true
  showDetail.value = true
  try {
    const res = await request.get(`/factory/bills/${row.id}/details`)
    detailItems.value = res.data || []
  } catch {
    detailItems.value = []
  } finally {
    detailLoading.value = false
  }
}

// ========== 收款 ==========
function openPayModal(row: any) {
  payTarget.value = row
  payAmount.value = null
  showPay.value = true
}

async function submitPay() {
  if (!payAmount.value || payAmount.value <= 0) {
    ElMessage.warning('请输入有效的收款金额')
    return
  }
  const newPaid = Number(payTarget.value.paidAmount || 0) + payAmount.value
  try {
    await request.put(`/factory/bills/${payTarget.value.id}/paid`, null, {
      params: { paidAmount: newPaid },
    })
    ElMessage.success(`收款 ¥${payAmount.value.toLocaleString()} 成功`)
    showPay.value = false
    loadBills()
  } catch (e: any) {
    ElMessage.error(e?.message || '收款失败')
  }
}

// ========== 从订单生成 ==========
function openGenerateModal() {
  generateForm.customerId = searchForm.customerId || ''
  // 默认当前月份
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  generateForm.month = `${y}-${m}`
  generatePreview.value = ''
  showGenerate.value = true
}

async function submitGenerate() {
  if (!generateForm.customerId || !generateForm.month) {
    ElMessage.warning('请选择客户和月份')
    return
  }
  generating.value = true
  generatePreview.value = ''
  try {
    const res = await request.post('/factory/customer-bills/generate', null, {
      params: {
        customerId: generateForm.customerId,
        month: generateForm.month,
      },
    })
    ElMessage.success(res.data || '生成成功')
    showGenerate.value = false
    // 自动筛选到刚生成的客户和月份
    searchForm.customerId = generateForm.customerId
    const [y, m] = generateForm.month.split('-')
    searchForm.month = `${y}年${m}月`
    current.value = 1
    loadBills()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '生成失败')
  } finally {
    generating.value = false
  }
}

// ========== 删除 ==========
async function confirmDelete(row: any) {
  if (!confirm(`确定要删除账单「${row.billNo}」吗？\n\n此操作不可恢复！`)) return
  try {
    await request.delete(`/factory/bills/${row.id}`)
    ElMessage.success('已删除')
    loadBills()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

// ========== 导出 ==========
function handleExport() {
  exportToExcel({
    filename: '客户账单',
    header: ['编号', '客户名称', '月份', '应收金额', '已付金额', '状态', '备注', '创建时间'],
    data: bills.value.map(row => [
      row.billNo,
      row.factoryName,
      row.month || '-',
      Number(row.totalAmount || 0),
      Number(row.paidAmount || 0),
      getStatusText(row.status),
      row.remark || '-',
      formatTime(row.createTime),
    ]),
    infoRows: [
      [`应收总额: ¥${totalAmount.value.toLocaleString()}`],
      [`已收总额: ¥${paidAmount.value.toLocaleString()}`],
      [`未收总额: ¥${unpaidAmount.value.toLocaleString()}`],
      [`导出时间：${new Date().toLocaleString()}`],
    ],
  })
}

// ========== 初始化 ==========
onMounted(() => {
  loadMaterials()
  loadCustomers().then(() => {
    if (preselectedCustomerId.value) {
      searchForm.customerId = preselectedCustomerId.value
    }
    loadBills()
  })
})
</script>

<style scoped>
/* ========== 搜索卡片 ========== */
.search-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  border: 1px solid #f0f2f5;
}

.search-form {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.search-form .form-group {
  flex: 1;
  min-width: 160px;
  margin-bottom: 0;
}

.form-actions {
  flex: 0 !important;
  min-width: auto !important;
}

/* ========== 统计网格 ========== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  border: 1px solid #f0f2f5;
  transition: transform 0.2s ease;
}

.stat-card:hover { transform: translateY(-2px); }

.stat-icon {
  width: 48px; height: 48px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center; font-size: 22px;
}
.stat-icon.blue { background: rgba(102, 126, 234, 0.1); }
.stat-icon.green { background: rgba(103, 194, 58, 0.1); }
.stat-icon.orange { background: rgba(230, 162, 60, 0.1); }
.stat-icon.red { background: rgba(245, 108, 108, 0.1); }

.stat-info h3 { margin: 0 0 4px 0; font-size: 20px; font-weight: 700; color: #303133; }
.stat-info p { margin: 0; font-size: 13px; color: #909399; }

/* ========== 按钮样式 ========== */
.btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 10px 18px; border-radius: 10px; font-size: 14px;
  font-weight: 500; border: none; cursor: pointer; transition: all 0.2s ease;
}
.btn-primary { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; }
.btn-primary:hover { transform: translateY(-2px); box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4); }
.btn-generate { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: #fff; }
.btn-generate:hover { transform: translateY(-2px); box-shadow: 0 4px 15px rgba(245, 87, 108, 0.4); }
.btn-success { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); color: #fff; }
.btn-success:hover { transform: translateY(-2px); box-shadow: 0 4px 15px rgba(56, 239, 125, 0.4); }
.btn-outline { background: transparent; color: #667eea; border: 2px solid #667eea; }
.btn-outline:hover { background: #667eea; color: #fff; }
.btn-ghost { background: #f5f7fa; color: #606266; }
.btn-ghost:hover { background: #e4e8f0; }
.btn-sm { padding: 6px 12px; font-size: 13px; border-radius: 8px; }
.btn-add-item { background: #ecf5ff; color: #409eff; }
.btn-add-item:hover { background: #409eff; color: #fff; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ========== 表格 ========== */
.table-card {
  background: #fff; border-radius: 16px; overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.data-table { width: 100%; border-collapse: collapse; }
.data-table thead { background: #f9fafb; }
.data-table th {
  padding: 14px 12px; text-align: left; font-size: 13px;
  font-weight: 600; color: #606266; letter-spacing: 0.5px;
  border-bottom: 2px solid #f0f2f5;
}
.data-table td {
  padding: 12px; border-bottom: 1px solid #f0f2f5;
  font-size: 14px; color: #303133; transition: background 0.15s ease;
}
.data-row:hover td { background: #f8f9fc; }

.col-id { width: 150px; }
.col-name { min-width: 160px; }
.col-month { width: 100px; }
.col-amount { width: 110px; text-align: right; }
.col-paid { width: 110px; text-align: right; }
.col-status { width: 90px; }
.col-remark { min-width: 120px; }
.col-time { width: 140px; }
.col-action { width: 150px; text-align: center; }

.id-badge {
  display: inline-block; background: #f5f7fa; color: #909399;
  font-size: 12px; font-weight: 600; padding: 3px 8px;
  border-radius: 6px; font-family: 'SF Mono', 'Consolas', monospace;
}
.amount { font-weight: 700; color: #303133; font-family: 'SF Mono', 'Consolas', monospace; }
.paid { font-weight: 600; color: #67c23a; font-family: 'SF Mono', 'Consolas', monospace; }

.customer-name { display: flex; align-items: center; gap: 10px; }
.avatar {
  width: 34px; height: 34px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-weight: 600; font-size: 14px; color: #fff;
}
.avatar-customer { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.customer-name strong { font-weight: 600; color: #303133; }

/* 徽章 */
.badge { display: inline-block; padding: 3px 10px; border-radius: 6px; font-size: 12px; font-weight: 500; }
.badge-default { background: #f5f7fa; color: #909399; }
.badge-warning { background: #fdf6ec; color: #e6a23c; }
.badge-info { background: #ecf5ff; color: #409eff; }
.badge-orange { background: #fef0e6; color: #e6a23c; }
.badge-success { background: #f0f9eb; color: #67c23a; }

.text-muted { color: #c0c4cc; }

/* 操作按钮 */
.action-group { display: flex; gap: 6px; justify-content: center; }
.btn-icon {
  width: 32px; height: 32px; border-radius: 8px; border: none; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: all 0.2s ease;
}
.btn-detail { background: rgba(64, 158, 255, 0.1); color: #409eff; }
.btn-detail:hover { background: #409eff; color: #fff; transform: scale(1.1); }
.btn-view { background: rgba(102, 126, 234, 0.1); color: #667eea; }
.btn-view:hover { background: #667eea; color: #fff; transform: scale(1.1); }
.btn-pay { background: rgba(103, 194, 58, 0.1); color: #67c23a; }
.btn-pay:hover { background: #67c23a; color: #fff; transform: scale(1.1); }
.btn-delete { background: rgba(245, 108, 108, 0.1); color: #f56c6c; }
.btn-delete:hover { background: #f56c6c; color: #fff; transform: scale(1.1); }

/* 加载和空状态 */
.loading-cell, .empty-cell { text-align: center; padding: 60px 20px !important; }
.loading-cell { display: flex; flex-direction: column; align-items: center; gap: 12px; color: #909399; }
.loading-spinner {
  width: 32px; height: 32px; border: 3px solid #f0f2f5;
  border-top-color: #667eea; border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 12px; color: #c0c4cc; }
.empty-state svg { opacity: 0.4; }
.empty-state p { font-size: 14px; margin: 0; }

/* ========== 分页 ========== */
.pagination-wrapper {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; background: #fafafa; border-top: 1px solid #f0f2f5;
}
.pagination-info { font-size: 14px; color: #606266; }
.pagination-info .highlight { color: #667eea; font-weight: 600; }
.pagination { display: flex; gap: 6px; }
.page-btn {
  min-width: 36px; height: 36px; border-radius: 8px; border: 1px solid #e4e8f0;
  background: #fff; color: #606266; font-size: 14px; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: all 0.2s ease;
}
.page-btn:hover:not(:disabled) { border-color: #667eea; color: #667eea; }
.page-btn.active { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-color: #667eea; color: #fff; }
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.page-btn.ellipsis { border: none; background: transparent; cursor: default; }

/* ========== 模态框 ========== */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center;
  opacity: 0; visibility: hidden; transition: all 0.3s ease; z-index: 1000;
}
.modal-overlay.show { opacity: 1; visibility: visible; }
.modal {
  background: #fff; border-radius: 20px; width: 90%; max-width: 540px;
  max-height: 90vh; overflow: hidden; transform: scale(0.9) translateY(20px);
  transition: transform 0.3s ease;
}
.modal-overlay.show .modal { transform: scale(1) translateY(0); }
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff;
}
.modal-title { font-size: 18px; font-weight: 600; }
.modal-close {
  width: 32px; height: 32px; border-radius: 8px; border: none;
  background: rgba(255,255,255,0.2); color: #fff; font-size: 18px;
  cursor: pointer; transition: all 0.2s ease;
}
.modal-close:hover { background: rgba(255,255,255,0.3); }
.modal-body { padding: 24px; max-height: 70vh; overflow-y: auto; }
.modal-footer {
  display: flex; justify-content: flex-end; gap: 12px;
  padding: 16px 24px; background: #fafafa; border-top: 1px solid #f0f2f5;
}

/* ========== 表单 ========== */
.form-row {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px;
}
.form-row.full { grid-template-columns: 1fr; }
.form-label {
  display: block; font-size: 13px; font-weight: 500; color: #606266; margin-bottom: 8px;
}
.form-input {
  width: 100%; padding: 10px 14px; border: 1px solid #e4e8f0; border-radius: 10px;
  font-size: 14px; transition: all 0.2s ease; box-sizing: border-box;
}
.form-input:focus {
  outline: none; border-color: #667eea; box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}
.amount-input {
  font-size: 18px; font-weight: 700; color: #303133;
  background: #f9fafb; border: 2px solid #e4e8f0;
}
.amount-input:focus { border-color: #67c23a; background: #f0f9eb; }

/* ========== 物料明细表 ========== */
.section-title {
  font-size: 14px; font-weight: 600; color: #303133;
  margin-bottom: 12px; padding-bottom: 8px;
  border-bottom: 1px solid #f0f2f5;
}

.material-table-wrapper {
  border: 1px solid #e4e8f0; border-radius: 10px; overflow: hidden;
  margin-bottom: 8px;
}

.material-table {
  width: 100%; border-collapse: collapse; font-size: 13px;
}
.material-table th {
  background: #f9fafb; padding: 8px 10px; text-align: left;
  font-weight: 600; color: #606266; border-bottom: 1px solid #e4e8f0;
}
.material-table td {
  padding: 6px 8px; border-bottom: 1px solid #f5f5f5;
}
.material-table tfoot td {
  background: #fafafa; border-top: 2px solid #e4e8f0;
  border-bottom: none; padding: 10px;
}

.table-input {
  width: 100%; padding: 6px 8px; border: 1px solid #e4e8f0;
  border-radius: 6px; font-size: 13px; box-sizing: border-box;
  transition: border-color 0.2s;
}
.table-input:focus { outline: none; border-color: #667eea; }

.subtotal {
  font-weight: 700; color: #303133; font-size: 13px;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.total-label {
  text-align: right; font-weight: 600; color: #606266; font-size: 14px;
}
.total-value {
  font-weight: 700; color: #67c23a; font-size: 16px;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.btn-remove {
  width: 24px; height: 24px; border-radius: 50%; border: none;
  background: #fef0f0; color: #f56c6c; font-size: 12px;
  cursor: pointer; transition: all 0.2s; display: flex;
  align-items: center; justify-content: center;
}
.btn-remove:hover { background: #f56c6c; color: #fff; }

.empty-material {
  text-align: center; padding: 20px;
  background: #f9fafb; border-radius: 10px; color: #c0c4cc; font-size: 13px;
}

/* ========== 详情弹窗 ========== */
.detail-info-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
  margin-bottom: 16px;
}
.detail-info-item {
  background: #f9fafb; border-radius: 10px; padding: 12px 14px;
}
.detail-label { display: block; font-size: 11px; color: #909399; margin-bottom: 4px; font-weight: 600; }
.detail-value { font-size: 14px; color: #303133; font-weight: 500; }
.id-text { font-family: 'SF Mono', 'Consolas', monospace; font-size: 13px; color: #909399; }
.amount-text { color: #303133; font-weight: 700; font-family: 'SF Mono', 'Consolas', monospace; }
.paid-text { color: #67c23a; font-weight: 600; font-family: 'SF Mono', 'Consolas', monospace; }
.unpaid-text { color: #f56c6c; font-weight: 700; font-family: 'SF Mono', 'Consolas', monospace; }

.detail-remark {
  background: #fdf6ec; border-radius: 10px; padding: 10px 14px;
  font-size: 13px; color: #606266; margin-bottom: 16px;
}
.detail-remark .detail-label { color: #e6a23c; }

.detail-section-title {
  font-size: 15px; font-weight: 600; color: #303133;
  margin-bottom: 12px; padding-bottom: 8px;
  border-bottom: 2px solid #f0f2f5;
}

.detail-loading {
  text-align: center; padding: 30px; color: #909399; font-size: 13px;
}

.detail-material-table {
  width: 100%; border-collapse: collapse; font-size: 13px;
  border: 1px solid #e4e8f0; border-radius: 10px; overflow: hidden;
}
.detail-material-table th {
  background: #f9fafb; padding: 10px 12px; text-align: left;
  font-weight: 600; color: #606266; border-bottom: 1px solid #e4e8f0;
}
.detail-material-table td {
  padding: 10px 12px; border-bottom: 1px solid #f5f5f5;
}
.detail-material-table tfoot td {
  background: #fafafa; border-top: 2px solid #e4e8f0;
  border-bottom: none; padding: 12px;
}

.price-cell {
  font-family: 'SF Mono', 'Consolas', monospace; color: #606266;
}
.subtotal-cell {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 700; color: #303133;
}

/* ========== 收款信息 ========== */
.pay-info { background: #f9fafb; border-radius: 12px; padding: 16px; }
.pay-info-item {
  display: flex; justify-content: space-between;
  padding: 8px 0; border-bottom: 1px solid #f0f2f5;
}
.pay-info-item:last-child { border-bottom: none; }
.pay-label { font-size: 14px; color: #909399; }
.pay-value { font-size: 14px; color: #303133; font-weight: 500; }

/* ========== 从订单生成 ========== */
.generate-desc {
  font-size: 13px; color: #606266; margin: 0 0 16px 0;
  padding: 12px 14px; background: #fff7ed; border-radius: 10px;
  border-left: 3px solid #f5576c;
}
.generate-tip {
  display: flex; align-items: flex-start; gap: 10px;
  margin-top: 12px; padding: 12px 14px; background: #ecf5ff;
  border-radius: 10px; border-left: 3px solid #409eff;
}
.tip-icon { font-size: 18px; flex-shrink: 0; margin-top: 1px; }
.tip-text { font-size: 13px; color: #409eff; line-height: 1.6; }
</style>
