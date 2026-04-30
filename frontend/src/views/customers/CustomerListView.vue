<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">客户管理</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">👥 客户列表</h1>
      <div class="page-actions">
        <button class="btn btn-outline" @click="handleExport">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          导出
        </button>
        <button class="btn btn-primary" @click="openAddModal">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新增客户
        </button>
      </div>
    </div>

    <!-- 搜索表单 -->
    <div class="search-card">
      <div class="search-form">
        <div class="form-group">
          <label class="form-label">客户名称</label>
          <input v-model="searchForm.keyword" type="text" class="form-input" placeholder="请输入客户名称">
        </div>
        <div class="form-group">
          <label class="form-label">客户类型</label>
          <select v-model="searchForm.customerType" class="form-input">
            <option value="">全部</option>
            <option :value="1">普通客户（有订单）</option>
            <option :value="2">工厂客户（有账单）</option>
          </select>
        </div>
        <div class="form-group form-actions">
          <button class="btn btn-primary" @click="loadData">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
            搜索
          </button>
          <button class="btn btn-ghost" @click="resetSearch">重置</button>
        </div>
      </div>
    </div>

    <!-- 表格卡片 -->
    <div class="table-card">
      <table class="data-table">
        <thead>
          <tr>
            <th class="col-id">客户ID</th>
            <th class="col-name">客户名称</th>
            <th class="col-type">客户类型</th>
            <th class="col-member">会员</th>
            <th class="col-contact">联系人</th>
            <th class="col-phone">联系电话</th>
            <th class="col-amount">累计消费</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" class="loading-cell">
              <div class="loading-spinner"></div>
              <span>加载中...</span>
            </td>
          </tr>
          <tr v-for="row in list" :key="row.id" class="data-row">
            <td class="col-id">
              <span class="id-badge">#{{ row.id }}</span>
            </td>
            <td class="col-name">
              <div class="customer-name">
                <span class="avatar" :class="row.customerType === 2 ? 'avatar-factory' : 'avatar-personal'">
                  {{ (row.name || row.customerName || '?').charAt(0) }}
                </span>
                <strong>{{ row.name || row.customerName }}</strong>
              </div>
            </td>
            <td class="col-type">
              <span v-if="row.customerType === 2" class="badge badge-factory">工厂</span>
              <span v-else class="badge badge-personal">普通</span>
            </td>
            <td class="col-member">
              <span v-if="row.isMember === 1" class="badge badge-member">{{ getMemberLevelName(row.memberLevel) }} 余额¥{{ formatMoney(row.balance) }}</span>
              <button v-else class="btn-upgrade" @click="openUpgradeModal(row)">升级会员</button>
            </td>
            <td class="col-contact">{{ row.contact || row.contactPerson || '—' }}</td>
            <td class="col-phone">{{ row.phone || '—' }}</td>
            <td class="col-amount">
              <span class="amount">¥{{ Number(row.totalAmount || 0).toLocaleString() }}</span>
            </td>
            <td class="col-action">
              <div class="action-group">
                <button class="btn-icon btn-view" @click="viewCustomer(row)" title="查看">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                </button>
                <button class="btn-icon btn-bill" @click="goCustomerBill(row)" title="客户账单">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                </button>
                <button v-if="row.isMember === 1" class="btn-icon btn-recharge" @click="openRechargeModal(row)" title="充值">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
                </button>
                <button class="btn-icon btn-edit" @click="openEditModal(row)" title="编辑">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </button>
                <button v-if="isSuperAdmin" class="btn-icon btn-delete" @click="confirmDeleteCustomer(row)" title="删除">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && list.length === 0">
            <td colspan="8" class="empty-cell">
              <div class="empty-state">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                <p>暂无客户数据</p>
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

    <!-- 新增/编辑客户弹窗 -->
    <div class="modal-overlay" :class="{ show: showModal }" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">{{ editForm.id ? '✏️ 编辑客户' : '➕ 新增客户' }}</span>
          <button class="modal-close" @click="showModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">客户名称 *</label>
              <input v-model="editForm.name" type="text" class="form-input" placeholder="公司或个人名称">
            </div>
            <div class="form-group">
              <label class="form-label">客户类型 *</label>
              <select v-model.number="editForm.customerType" class="form-input" @change="onCustomerTypeChange">
                <option :value="1">👤 普通客户（有订单）</option>
                <option :value="2">🏭 工厂客户（有账单）</option>
              </select>
            </div>
          </div>
          <!-- 工厂类型（仅工厂客户显示） -->
          <div class="form-row animate-slide" v-if="editForm.customerType === 2">
            <div class="form-group">
              <label class="form-label">工厂类型 *</label>
              <select v-model="editForm.factoryType" class="form-input">
                <option value="">请选择工厂类型</option>
                <option value="印刷">🖨️ 印刷</option>
                <option value="包装">📦 包装</option>
                <option value="广告制作">📢 广告制作</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">联系人</label>
              <input v-model="editForm.contact" type="text" class="form-input" placeholder="联系人姓名">
            </div>
            <div class="form-group">
              <label class="form-label">联系电话</label>
              <input v-model="editForm.phone" type="tel" class="form-input" placeholder="手机号码（选填）">
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">客户等级</label>
              <select v-model.number="editForm.level" class="form-input">
                <option :value="1">普通会员</option>
                <option :value="2">银牌会员</option>
                <option :value="3">金牌会员</option>
                <option :value="4">钻石会员</option>
              </select>
            </div>
          </div>
          <div class="form-row full">
            <div class="form-group">
              <label class="form-label">详细地址</label>
              <input v-model="editForm.address" type="text" class="form-input" placeholder="详细地址">
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="showModal = false">取消</button>
          <button class="btn btn-success" @click="saveCustomer">💾 {{ editForm.id ? '保存修改' : '保存客户' }}</button>
        </div>
      </div>
    </div>

    <!-- 查看客户详情弹窗 -->
    <div class="modal-overlay" :class="{ show: showView }" @click.self="showView = false">
      <div class="modal modal-lg">
        <div class="modal-header">
          <span class="modal-title">👁️ 客户详情</span>
          <button class="modal-close" @click="showView = false">✕</button>
        </div>
        <div class="modal-body" v-if="viewData">
          <!-- 概览卡片 -->
          <div class="profile-cards">
            <div class="profile-card profile-main">
              <div class="profile-avatar" :class="viewData.customerType === 2 ? 'avatar-factory' : 'avatar-personal'">
                {{ (viewData.name || viewData.customerName || '?').charAt(0) }}
              </div>
              <div class="profile-info">
                <h3>{{ viewData.name || viewData.customerName }}</h3>
                <p>客户ID: #{{ viewData.id }}</p>
              </div>
            </div>
            <div class="profile-card profile-stat">
              <div class="stat-icon">💰</div>
              <div class="stat-content">
                <div class="stat-value">¥{{ Number(viewData.totalAmount || 0).toLocaleString() }}</div>
                <div class="stat-label">累计消费</div>
              </div>
            </div>
          </div>

          <!-- 详情网格 -->
          <div class="detail-grid">
            <div class="detail-item">
              <div class="detail-label">客户类型</div>
              <div class="detail-value">
                <span v-if="viewData.customerType === 2" class="badge badge-factory">🏭 工厂客户</span>
                <span v-else class="badge badge-personal">👤 普通客户</span>
              </div>
            </div>
            <div class="detail-item" v-if="viewData.customerType === 2">
              <div class="detail-label">工厂类型</div>
              <div class="detail-value">
                <span v-if="viewData.factoryType" class="badge badge-plain">{{ viewData.factoryType }}</span>
                <span v-else class="text-muted">—</span>
              </div>
            </div>
            <div class="detail-item">
              <div class="detail-label">客户等级</div>
              <div class="detail-value">
                <span class="badge" :class="getLevelBadgeClass(viewData.level)">{{ getLevelName(viewData.level) }}</span>
              </div>
            </div>
            <div class="detail-item">
              <div class="detail-label">联系人</div>
              <div class="detail-value">{{ viewData.contact || viewData.contactPerson || '—' }}</div>
            </div>
            <div class="detail-item">
              <div class="detail-label">联系电话</div>
              <div class="detail-value">{{ viewData.phone || '—' }}</div>
            </div>
          </div>

          <!-- 地址 -->
          <div v-if="viewData.address" class="address-section">
            <div class="section-label">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
              详细地址
            </div>
            <div class="address-content">{{ viewData.address }}</div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="showView = false; goCustomerBill(viewData!)">查看账单</button>
          <button class="btn btn-primary" @click="showView = false; openEditModal(viewData!)">编辑</button>
          <button class="btn btn-ghost" @click="showView = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- 升级会员弹窗 -->
    <div class="modal-overlay" :class="{ show: showUpgrade }" @click.self="showUpgrade = false">
      <div class="modal" style="max-width: 440px;">
        <div class="modal-header">
          <span class="modal-title">升级为会员</span>
          <button class="modal-close" @click="showUpgrade = false">✕</button>
        </div>
        <div class="modal-body">
          <div style="background:#ecf5ff;padding:12px 14px;border-radius:8px;margin-bottom:16px;font-size:13px;color:#409eff;">
            客户：<strong>{{ upgradeTarget?.customerName || upgradeTarget?.name }}</strong>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">会员等级</label>
              <select v-model="upgradeForm.level" class="form-input">
                <option value="normal">普通会员</option>
                <option value="silver">银牌会员</option>
                <option value="gold">金牌会员</option>
                <option value="diamond">钻石会员</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">初始预存金额</label>
              <input v-model.number="upgradeForm.balance" type="number" class="form-input" placeholder="0.00（选填）">
            </div>
          </div>
          <div v-if="upgradeForm.balance > 0" style="background:#f0f9eb;padding:10px 14px;border-radius:8px;font-size:12px;color:#67c23a;margin-top:4px;">
            系统将自动生成一笔 ¥{{ upgradeForm.balance.toFixed(2) }} 的充值记录
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="showUpgrade = false">取消</button>
          <button class="btn btn-success" @click="submitUpgrade">确认升级</button>
        </div>
      </div>
    </div>

    <!-- 充值弹窗 -->
    <div class="modal-overlay" :class="{ show: showRecharge }" @click.self="showRecharge = false">
      <div class="modal" style="max-width: 440px;">
        <div class="modal-header" style="background:linear-gradient(135deg,#38ef7d,#11998e);">
          <span class="modal-title">会员充值</span>
          <button class="modal-close" @click="showRecharge = false">✕</button>
        </div>
        <div class="modal-body">
          <div style="background:#f0f9eb;padding:14px;border-radius:10px;margin-bottom:16px;">
            <div style="font-size:12px;color:#67c23a;margin-bottom:4px;">当前余额</div>
            <div style="font-size:22px;font-weight:700;color:#38ef7d;">¥{{ formatMoney(rechargeTarget?.balance) }}</div>
            <div style="font-size:12px;color:#909399;margin-top:2px;">{{ rechargeTarget?.customerName || rechargeTarget?.name }}</div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">充值金额 *</label>
              <input v-model.number="rechargeForm.amount" type="number" class="form-input" placeholder="请输入充值金额">
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">充值备注</label>
              <input v-model="rechargeForm.remark" class="form-input" placeholder="如：季度充值（选填）">
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="showRecharge = false">取消</button>
          <button class="btn btn-success" @click="submitRecharge">确认充值</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { useAuthStore } from '@/stores/auth'
import { exportToExcel } from '@/utils/excelExport'
import { customerApi } from '@/api/modules/customer'

const router = useRouter()
const authStore = useAuthStore()

// 超级管理员判断
const isSuperAdmin = computed(() => {
  const roles = authStore.userInfo?.roles || []
  return roles.includes('SUPER_ADMIN') || roles.includes('ADMIN')
})

const levelNames: Record<number, string> = { 1: '普通会员', 2: '银牌会员', 3: '金牌会员', 4: '钻石会员' }
const levelBadgeClass: Record<number, string> = { 1: 'badge-default', 2: 'badge-silver', 3: 'badge-gold', 4: 'badge-diamond' }
function getLevelName(level: any) { return levelNames[level] || '普通会员' }
function getLevelBadgeClass(level: any) { return levelBadgeClass[level] || 'badge-default' }

const list = ref<any[]>([])
const loading = ref(false)

// 客户类型变更时清空工厂类型
function onCustomerTypeChange() {
  if (editForm.value.customerType !== 2) {
    editForm.value.factoryType = ''
  }
}

const total = ref(0)
const current = ref(1)
const size = ref(10)
const showModal = ref(false)
const showView = ref(false)
const viewData = ref<any>(null)

const searchForm = ref({ keyword: '', customerType: null as number | null })

const editForm = ref<any>({
  id: null, name: '', customerType: 1 as number, factoryType: '',
  contact: '', phone: '', level: 1 as number, address: ''
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

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      current: current.value,
      size: size.value,
      keyword: searchForm.value.keyword || undefined,
    }
    // 只传有值的客户类型筛选
    if (searchForm.value.customerType !== null && searchForm.value.customerType !== '') {
      params.customerType = searchForm.value.customerType
    }
    const res = await request.get('/customers', { params })
    const data = res.data
    list.value = data?.records || data?.list || []
    total.value = data?.total || list.value.length
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changePage(p: number) {
  current.value = p
  loadData()
}

function resetSearch() {
  searchForm.value = { keyword: '', customerType: null as number | null }
  current.value = 1
  loadData()
}

function openAddModal() {
  editForm.value = { id: null, name: '', customerType: 1, factoryType: '', contact: '', phone: '', level: 1, address: '' }
  showModal.value = true
}

function openEditModal(row: any) {
  editForm.value = {
    id: row.id,
    name: row.name || row.customerName,
    customerType: row.customerType || 1,
    factoryType: row.factoryType || '',
    contact: row.contact || row.contactPerson || '',
    phone: row.phone || '',
    level: row.level || 1,
    address: row.address || ''
  }
  showModal.value = true
}

function viewCustomer(row: any) {
  viewData.value = row
  showView.value = true
}

async function saveCustomer() {
  if (!editForm.value.name) {
    ElMessage.warning('请填写客户名称')
    return
  }
  // 工厂客户必须选择工厂类型
  if (editForm.value.customerType === 2 && !editForm.value.factoryType) {
    ElMessage.warning('工厂客户必须选择工厂类型')
    return
  }
  try {
    if (editForm.value.id) {
      await request.put(`/customers/${editForm.value.id}`, editForm.value)
      ElMessage.success('修改成功')
    } else {
      await request.post('/customers', editForm.value)
      ElMessage.success('添加成功')
    }
    showModal.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

function handleExport() {
  exportToExcel({
    filename: '客户列表',
    header: ['客户名称', '联系人', '电话', '地址', '客户类型', '等级', '累计消费', '创建时间'],
    data: list.value.map(row => [
      row.name || row.customerName,
      row.contact || row.contactPerson || '-',
      row.phone || '-',
      row.address || '-',
      row.customerType === 2 ? '工厂客户' : '普通客户',
      getLevelName(row.level),
      `¥${Number(row.totalAmount || 0).toLocaleString()}`,
      (row.createTime || '').toString().slice(0, 10),
    ]),
    infoRows: [[`导出时间：${new Date().toLocaleString()}`], [`共 ${list.value.length} 条记录`]],
  })
}

function goCustomerBill(row: any) {
  router.push({ name: 'CustomerBills', query: { customerId: row.id } })
}

async function confirmDeleteCustomer(row: any) {
  const name = row.name || row.customerName
  // 前端前置提示
  const warnings: string[] = []
  if (row.isMember === 1 && Number(row.balance) > 0) warnings.push('该客户是会员且有预存余额，需先退还或消费完毕')
  const tip = warnings.length > 0
    ? '\n\n⚠️ 提示：\n' + warnings.map(w => '• ' + w).join('\n')
    : ''
  try {
    await ElMessageBox.confirm(
      `确定要删除客户「${name}」吗？${tip}\n\n有关联订单或余额的客户将无法删除。`,
      '确认删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await request.delete(`/customers/${row.id}`)
    ElMessage.success(`已删除客户「${name}」`)
    loadData()
  } catch (e: any) {
    if (e !== 'cancel' && e?.message) ElMessage.error(e.message)
  }
}

// ========== 会员相关 ==========

const memberLevelMap: Record<string, string> = { normal: '普通', silver: '银牌', gold: '金牌', diamond: '钻石' }
function getMemberLevelName(level: string) { return memberLevelMap[level] || '普通' }
function formatMoney(v: any) { return Number(v || 0).toLocaleString('zh', { minimumFractionDigits: 2 }) }

const showUpgrade = ref(false)
const upgradeTarget = ref<any>(null)
const upgradeForm = ref({ level: 'normal', balance: 0 })

const showRecharge = ref(false)
const rechargeTarget = ref<any>(null)
const rechargeForm = ref({ amount: 0, remark: '' })

function openUpgradeModal(row: any) {
  upgradeTarget.value = row
  upgradeForm.value = { level: 'normal', balance: 0 }
  showUpgrade.value = true
}

function openRechargeModal(row: any) {
  rechargeTarget.value = row
  rechargeForm.value = { amount: 0, remark: '' }
  showRecharge.value = true
}

async function submitUpgrade() {
  if (!upgradeTarget.value) return
  try {
    await customerApi.upgradeToMember(upgradeTarget.value.id, {
      level: upgradeForm.value.level,
      balance: upgradeForm.value.balance || undefined
    })
    ElMessage.success(`客户「${upgradeTarget.value.customerName || upgradeTarget.value.name}」已升级为会员！`)
    showUpgrade.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '升级失败')
  }
}

async function submitRecharge() {
  if (!rechargeTarget.value) return
  if (!rechargeForm.value.amount || rechargeForm.value.amount <= 0) {
    ElMessage.warning('请输入有效的充值金额')
    return
  }
  try {
    await customerApi.recharge(rechargeTarget.value.id, {
      amount: rechargeForm.value.amount,
      remark: rechargeForm.value.remark || `充值 ¥${rechargeForm.value.amount}`
    })
    ElMessage.success('充值成功！')
    showRecharge.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '充值失败')
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
/* ========== 搜索区域 ========== */
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
  min-width: 180px;
  margin-bottom: 0;
}

.search-form .form-label {
  color: #606266;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
}

.search-form .form-input {
  background: #f9fafb;
  border: 1px solid #e4e8f0;
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.search-form .form-input:focus {
  background: #fff;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* ========== 按钮样式 ========== */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.btn-success {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: #fff;
}

.btn-success:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(56, 239, 125, 0.4);
}

.btn-outline {
  background: transparent;
  color: #667eea;
  border: 2px solid #667eea;
}

.btn-outline:hover {
  background: #667eea;
  color: #fff;
}

.btn-ghost {
  background: rgba(255,255,255,0.9);
  color: #666;
}

.btn-ghost:hover {
  background: #fff;
}

/* ========== 表格卡片 ========== */
.table-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table thead {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
}

.data-table th {
  padding: 16px 12px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 2px solid #e4e8f0;
}

.data-table td {
  padding: 14px 12px;
  border-bottom: 1px solid #f0f2f5;
  font-size: 14px;
  color: #303133;
  transition: background 0.2s ease;
}

.data-row:hover td {
  background: #f8f9fc;
}

.data-row:last-child td {
  border-bottom: none;
}

/* 列宽 */
.col-id { width: 70px; text-align: center; }
.col-name { min-width: 160px; }
.col-type { width: 130px; }
.col-factory { width: 100px; }
.col-member { width: 160px; }
.col-contact { width: 100px; }
.col-phone { width: 120px; }
.col-amount { width: 120px; text-align: right; }
.col-action { width: 120px; text-align: center; }

/* ID 徽章 */
.id-badge {
  display: inline-block;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  color: #909399;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 6px;
}

/* 客户名称 */
.customer-name {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  color: #fff;
}

.avatar-personal {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.avatar-factory {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.customer-name strong {
  font-weight: 600;
  color: #303133;
}

/* 徽章 */
.badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.badge-personal {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.badge-factory {
  background: rgba(245, 87, 108, 0.1);
  color: #f5576c;
}

.badge-plain {
  background: #f5f7fa;
  color: #606266;
}

.badge-default { background: #f5f7fa; color: #909399; }
.badge-silver { background: linear-gradient(135deg, #e8e8e8 0%, #c0c0c0 100%); color: #666; }
.badge-gold { background: linear-gradient(135deg, #ffd700 0%, #ffb347 100%); color: #996600; }
.badge-diamond { background: linear-gradient(135deg, #b9f2ff 0%, #667eea 100%); color: #fff; }

.badge-member {
  background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
  color: #409eff;
  font-size: 11px;
}

.btn-upgrade {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 6px;
  border: 1px dashed #409eff;
  background: rgba(64, 158, 255, 0.05);
  color: #409eff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-upgrade:hover {
  background: #409eff;
  color: #fff;
  border-style: solid;
}

.btn-recharge {
  background: rgba(56, 239, 125, 0.1) !important;
  color: #38ef7d !important;
}

.btn-recharge:hover {
  background: #38ef7d !important;
  color: #fff !important;
  transform: scale(1.1);
}

/* 金额 */
.amount {
  font-weight: 600;
  color: #67c23a;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 14px;
}

/* 操作按钮组 */
.action-group {
  display: flex;
  gap: 6px;
  justify-content: center;
}

.btn-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.btn-view {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.btn-view:hover {
  background: #667eea;
  color: #fff;
  transform: scale(1.1);
}

.btn-edit {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.btn-edit:hover {
  background: #67c23a;
  color: #fff;
  transform: scale(1.1);
}

.btn-bill {
  background: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}

.btn-bill:hover {
  background: #e6a23c;
  color: #fff;
  transform: scale(1.1);
}

.btn-delete {
  background: rgba(245, 87, 108, 0.1);
  color: #f5576c;
}

.btn-delete:hover {
  background: #f5576c;
  color: #fff;
  transform: scale(1.1);
}

/* 加载和空状态 */
.loading-cell, .empty-cell {
  text-align: center;
  padding: 60px 20px !important;
}

.loading-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #909399;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f0f2f5;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #c0c4cc;
}

.empty-state svg {
  opacity: 0.4;
}

.empty-state p {
  font-size: 14px;
  margin: 0;
}

/* ========== 分页 ========== */
.pagination-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #fafafa;
  border-top: 1px solid #f0f2f5;
}

.pagination-info {
  font-size: 14px;
  color: #606266;
}

.pagination-info .highlight {
  color: #667eea;
  font-weight: 600;
}

.pagination {
  display: flex;
  gap: 6px;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid #e4e8f0;
  background: #fff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
}

.page-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  color: #fff;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-btn.ellipsis {
  border: none;
  background: transparent;
  cursor: default;
}

/* ========== 模态框 ========== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s ease;
  z-index: 1000;
}

.modal-overlay.show {
  opacity: 1;
  visibility: visible;
}

.modal {
  background: #fff;
  border-radius: 20px;
  width: 90%;
  max-width: 540px;
  max-height: 90vh;
  overflow: hidden;
  transform: scale(0.9) translateY(20px);
  transition: transform 0.3s ease;
}

.modal-overlay.show .modal {
  transform: scale(1) translateY(0);
}

.modal-lg {
  max-width: 640px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
}

.modal-close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: rgba(255,255,255,0.2);
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: rgba(255,255,255,0.3);
}

.modal-body {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  background: #fafafa;
  border-top: 1px solid #f0f2f5;
}

/* 表单 */
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.form-row.full {
  grid-template-columns: 1fr;
}

.form-group {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e4e8f0;
  border-radius: 10px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

/* ========== 客户详情 ========== */
.profile-cards {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 16px;
  margin-bottom: 24px;
}

.profile-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  border-radius: 16px;
  padding: 20px;
}

.profile-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.profile-info h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  color: #303133;
}

.profile-info p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.profile-stat {
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: center;
}

.stat-icon {
  font-size: 32px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #67c23a;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.detail-item {
  background: #f9fafb;
  border-radius: 12px;
  padding: 14px;
}

.detail-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.detail-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.address-section {
  background: #f9fafb;
  border-radius: 12px;
  padding: 14px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.address-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
}

.text-muted {
  color: #c0c4cc;
}

/* 动画 */
.animate-slide {
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
