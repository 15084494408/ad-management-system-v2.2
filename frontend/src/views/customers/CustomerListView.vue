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
        <button class="btn btn-default" @click="handleExport">⬇️ 导出</button>
        <button class="btn btn-primary" @click="openAddModal">+ 新增客户</button>
      </div>
    </div>

    <!-- 搜索表单 -->
    <div class="search-form">
      <div class="form-group">
        <label>客户名称</label>
        <input v-model="searchForm.keyword" type="text" class="form-control" placeholder="请输入客户名称">
      </div>
      <div class="form-group">
        <label>客户类型</label>
        <select v-model="searchForm.customerType" class="form-control">
          <option value="">全部</option>
          <option :value="1">👤 普通客户（有订单）</option>
          <option :value="2">🏭 工厂客户（有账单）</option>
        </select>
      </div>
      <div class="form-group" style="align-self:flex-end;">
        <button class="btn btn-primary" @click="loadData">🔍 搜索</button>
        <button class="btn btn-default" @click="resetSearch">重置</button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="card">
      <table class="data-table">
        <thead>
          <tr>
            <th>客户ID</th>
            <th>客户名称</th>
            <th>客户类型</th>
            <th>工厂类型</th>
            <th>联系人</th>
            <th>联系电话</th>
            <th>累计消费</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" style="text-align:center;padding:40px;color:#909399;">
              <div class="loading-spinner" style="margin:0 auto 8px;"></div>加载中...
            </td>
          </tr>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.id }}</td>
            <td><strong>{{ row.name || row.customerName }}</strong></td>
            <td>
              <span v-if="row.customerType === 2" class="tag tag-warning">🏭 工厂客户</span>
              <span v-else class="tag tag-primary">👤 普通客户</span>
            </td>
            <td>{{ row.factoryType || '-' }}</td>
            <td>{{ row.contact || row.contactPerson || '-' }}</td>
            <td>{{ row.phone || '-' }}</td>
            <td style="font-weight:600;">¥{{ Number(row.totalAmount || 0).toLocaleString() }}</td>
            <td class="action-btns">
              <button class="action-btn view" @click="viewCustomer(row)">查看</button>
              <button class="action-btn edit" @click="openEditModal(row)">编辑</button>
              <button v-if="isSuperAdmin" class="action-btn delete" @click="confirmDeleteCustomer(row)" title="仅超级管理员可删除">删除</button>
            </td>
          </tr>
          <tr v-if="!loading && list.length === 0">
            <td colspan="8" style="text-align:center;padding:40px;color:#c0c4cc;">暂无客户数据</td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="pagination">
        <div class="pagination-info">共 {{ total }} 条记录</div>
        <div class="pagination-buttons">
          <button class="page-btn" :disabled="current <= 1" @click="changePage(current - 1)">«</button>
          <button
            v-for="p in pageButtons" :key="p"
            class="page-btn" :class="{ active: p === current }"
            :disabled="p === '...'"
            @click="p !== '...' && changePage(p as number)"
          >{{ p }}</button>
          <button class="page-btn" :disabled="current >= totalPages" @click="changePage(current + 1)">»</button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑客户弹窗 -->
    <div class="modal-overlay" :class="{ show: showModal }" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">{{ editForm.id ? '✏️ 编辑客户' : '👤 新增客户' }}</span>
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
          <div class="form-row" v-if="editForm.customerType === 2">
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
          <button class="btn btn-default" @click="showModal = false">取消</button>
          <button class="btn btn-success" @click="saveCustomer">💾 {{ editForm.id ? '保存修改' : '保存客户' }}</button>
        </div>
      </div>
    </div>

    <!-- 查看客户详情弹窗 -->
    <div class="modal-overlay" :class="{ show: showView }" @click.self="showView = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">👁️ 客户详情</span>
          <button class="modal-close" @click="showView = false">✕</button>
        </div>
        <div class="modal-body" v-if="viewData">
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:15px;margin-bottom:20px;">
            <div style="background:#f5f7fa;padding:15px;border-radius:8px;text-align:center;">
              <div style="font-size:30px;margin-bottom:10px;">{{ viewData.customerType === 2 ? '🏭' : '🏢' }}</div>
              <div style="font-size:16px;font-weight:600;">{{ viewData.name || viewData.customerName }}</div>
              <div style="font-size:12px;color:#909399;">客户ID: {{ viewData.id }}</div>
            </div>
            <div style="background:#f0f9eb;padding:15px;border-radius:8px;text-align:center;">
              <div style="font-size:24px;font-weight:600;color:#67c23a;">¥{{ Number(viewData.totalAmount || 0).toLocaleString() }}</div>
              <div style="font-size:12px;color:#909399;">累计消费</div>
            </div>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:15px;">
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;">
              <div style="font-size:11px;color:#909399;">客户类型</div>
              <div style="font-size:14px;">
                <span v-if="viewData.customerType === 2" class="tag tag-warning">🏭 工厂客户</span>
                <span v-else class="tag tag-primary">👤 普通客户</span>
              </div>
            </div>
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;" v-if="viewData.customerType === 2">
              <div style="font-size:11px;color:#909399;">工厂类型</div>
              <div style="font-size:14px;"><span class="tag tag-info">{{ viewData.factoryType || '-' }}</span></div>
            </div>
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;">
              <div style="font-size:11px;color:#909399;">客户等级</div>
              <div style="font-size:14px;"><span class="tag tag-success">{{ getLevelName(viewData.level) }}</span></div>
            </div>
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;">
              <div style="font-size:11px;color:#909399;">联系人</div>
              <div style="font-size:14px;">{{ viewData.contact || viewData.contactPerson || '-' }}</div>
            </div>
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;">
              <div style="font-size:11px;color:#909399;">联系电话</div>
              <div style="font-size:14px;">{{ viewData.phone || '-' }}</div>
            </div>
          </div>
          <div v-if="viewData.address" style="margin-top:15px;">
            <div style="font-size:12px;color:#909399;margin-bottom:5px;">地址</div>
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;font-size:13px;">{{ viewData.address }}</div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-primary" @click="showView = false; openEditModal(viewData!)">编辑</button>
          <button class="btn btn-default" @click="showView = false">关闭</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

// 超级管理员判断
const isSuperAdmin = computed(() => {
  const roles = authStore.userInfo?.roles || []
  return roles.includes('SUPER_ADMIN') || roles.includes('ADMIN')
})

const levelNames: Record<number, string> = { 1: '普通会员', 2: '银牌会员', 3: '金牌会员', 4: '钻石会员' }
function getLevelName(level: any) { return levelNames[level] || '普通会员' }

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
  ElMessage.info('导出功能开发中')
}

async function confirmDeleteCustomer(row: any) {
  if (!confirm(`确定要删除客户「${row.name || row.customerName}」吗？\n\n⚠️ 关联数据可能受影响，此操作不可恢复！`)) return
  try {
    await request.delete(`/customers/${row.id}`)
    ElMessage.success(`已删除客户「${row.name || row.customerName}」`)
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(() => { loadData() })
</script>
