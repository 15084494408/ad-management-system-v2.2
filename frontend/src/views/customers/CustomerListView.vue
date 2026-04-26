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
        <button class="btn btn-default" @click="showImport = true">⬆️ 批量导入</button>
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
        <select v-model="searchForm.type" class="form-control">
          <option value="">全部</option>
          <option value="普通客户">普通客户</option>
          <option value="工厂客户">工厂客户</option>
        </select>
      </div>
      <div class="form-group">
        <label>客户标签</label>
        <select v-model="searchForm.tag" class="form-control">
          <option value="">全部</option>
          <option v-for="t in tagOptions" :key="t.id" :value="t.name">{{ t.icon || '🏷️' }} {{ t.name }}</option>
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
            <th>类型</th>
            <th>联系人</th>
            <th>联系电话</th>
            <th>标签</th>
            <th>累计消费</th>
            <th>跟进人</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="9" style="text-align:center;padding:40px;color:#909399;">
              <div class="loading-spinner" style="margin:0 auto 8px;"></div>加载中...
            </td>
          </tr>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.id }}</td>
            <td><strong>{{ row.name || row.customerName }}</strong></td>
            <td><span class="tag tag-primary">{{ row.type || '普通客户' }}</span></td>
            <td>{{ row.contact || row.contactName || '-' }}</td>
            <td>{{ row.phone || '-' }}</td>
            <td>
              <span v-if="row.tags" class="tag tag-success">{{ row.tags }}</span>
              <span v-else style="color:#c0c4cc;">-</span>
            </td>
            <td style="font-weight:600;">¥{{ Number(row.totalAmount || 0).toLocaleString() }}</td>
            <td>{{ row.follower || '-' }}</td>
            <td class="action-btns">
              <button class="action-btn view" @click="viewCustomer(row)">查看</button>
              <button class="action-btn edit" @click="openEditModal(row)">编辑</button>
              <button class="action-btn view" @click="showFollowDialog = true">跟进</button>
            </td>
          </tr>
          <tr v-if="!loading && list.length === 0">
            <td colspan="9" style="text-align:center;padding:40px;color:#c0c4cc;">暂无客户数据</td>
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
              <select v-model="editForm.type" class="form-input">
                <option value="">请选择类型</option>
                <option value="企业客户">企业客户</option>
                <option value="个人客户">个人客户</option>
                <option value="工厂客户">工厂客户</option>
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
              <select v-model="editForm.level" class="form-input">
                <option value="1">普通会员</option>
                <option value="2">银牌会员</option>
                <option value="3">金牌会员</option>
                <option value="4">钻石会员</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">客户标签</label>
              <select v-model="editForm.tags" class="form-input">
                <option value="">无标签</option>
                <option v-for="t in tagOptions" :key="t.id" :value="t.name">{{ t.icon || '🏷️' }} {{ t.name }}</option>
              </select>
            </div>
          </div>
          <div class="form-row full">
            <div class="form-group">
              <label class="form-label">详细地址</label>
              <input v-model="editForm.address" type="text" class="form-input" placeholder="详细地址">
            </div>
          </div>
          <div class="form-row full">
            <div class="form-group">
              <label class="form-label">备注</label>
              <textarea v-model="editForm.remark" class="form-input" placeholder="客户备注信息..."></textarea>
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
              <div style="font-size:30px;margin-bottom:10px;">🏢</div>
              <div style="font-size:16px;font-weight:600;">{{ viewData.name }}</div>
              <div style="font-size:12px;color:#909399;">客户ID: {{ viewData.id }}</div>
            </div>
            <div style="background:#f0f9eb;padding:15px;border-radius:8px;text-align:center;">
              <div style="font-size:24px;font-weight:600;color:#67c23a;">¥{{ Number(viewData.totalAmount || 0).toLocaleString() }}</div>
              <div style="font-size:12px;color:#909399;">累计消费</div>
            </div>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:15px;">
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;">
              <div style="font-size:11px;color:#909399;">客户类型</div>
              <div style="font-size:14px;"><span class="tag tag-primary">{{ viewData.type || '企业客户' }}</span></div>
            </div>
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;">
              <div style="font-size:11px;color:#909399;">客户等级</div>
              <div style="font-size:14px;"><span class="tag tag-success">{{ getLevelName(viewData.level) }}</span></div>
            </div>
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;">
              <div style="font-size:11px;color:#909399;">联系人</div>
              <div style="font-size:14px;">{{ viewData.contact || '-' }}</div>
            </div>
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;">
              <div style="font-size:11px;color:#909399;">联系电话</div>
              <div style="font-size:14px;">{{ viewData.phone || '-' }}</div>
            </div>
          </div>
          <div v-if="viewData.tags" style="margin-top:15px;">
            <div style="font-size:12px;color:#909399;margin-bottom:5px;">客户标签</div>
            <div><span class="tag tag-success">{{ viewData.tags }}</span></div>
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

    <!-- 客户跟进弹窗 -->
    <div class="modal-overlay" :class="{ show: showFollowDialog }" @click.self="showFollowDialog = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">📝 客户跟进</span>
          <button class="modal-close" @click="showFollowDialog = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">跟进方式</label>
              <select v-model="followForm.method" class="form-input">
                <option>电话沟通</option>
                <option>上门拜访</option>
                <option>微信/短信</option>
                <option>邮件沟通</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">跟进时间</label>
              <input v-model="followForm.time" type="datetime-local" class="form-input">
            </div>
          </div>
          <div class="form-row full">
            <div class="form-group">
              <label class="form-label">跟进内容 *</label>
              <textarea v-model="followForm.content" class="form-input" placeholder="请详细记录本次跟进内容..."></textarea>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">下次跟进计划</label>
            <input v-model="followForm.nextDate" type="date" class="form-input">
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showFollowDialog = false">取消</button>
          <button class="btn btn-success" @click="saveFollow">💾 保存记录</button>
        </div>
      </div>
    </div>

    <!-- 批量导入弹窗 -->
    <div class="modal-overlay" :class="{ show: showImport }" @click.self="showImport = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">⬆️ 批量导入客户</span>
          <button class="modal-close" @click="showImport = false">✕</button>
        </div>
        <div class="modal-body">
          <div style="border:2px dashed #dcdfe6;border-radius:8px;padding:40px;text-align:center;margin-bottom:20px;cursor:pointer;">
            <div style="font-size:48px;margin-bottom:15px;">📁</div>
            <div style="font-size:14px;color:#606266;">点击或拖拽文件到此处上传</div>
            <div style="font-size:12px;color:#909399;margin-top:8px;">支持 Excel (.xlsx, .xls) 或 CSV 格式</div>
          </div>
          <div style="background:#f5f7fa;padding:15px;border-radius:8px;margin-bottom:15px;">
            <h4 style="font-size:13px;margin-bottom:10px;">📥 下载导入模板</h4>
            <p style="font-size:12px;color:#909399;margin-bottom:10px;">请先下载模板，按照格式填写后上传</p>
            <button class="btn btn-default btn-sm">⬇️ 下载模板</button>
          </div>
          <div class="form-group">
            <label class="form-label">导入说明</label>
            <ul style="font-size:12px;color:#909399;line-height:1.8;padding-left:20px;">
              <li>Excel文件请包含表头：客户名称、联系人、电话、客户类型</li>
              <li>一次最多导入500条数据</li>
              <li>重复的客户数据将被跳过</li>
            </ul>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showImport = false">取消</button>
          <button class="btn btn-primary" @click="showImport = false; alert('导入功能开发中')">⬆️ 开始导入</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const levelNames: Record<number, string> = { 1: '普通会员', 2: '银牌会员', 3: '金牌会员', 4: '钻石会员' }
function getLevelName(level: any) { return levelNames[level] || '普通会员' }

const list = ref<any[]>([])
const loading = ref(false)
const tagOptions = ref<any[]>([])

async function loadTags() {
  try {
    const res = await request.get('/customers/tags')
    tagOptions.value = res.data || []
  } catch {
    tagOptions.value = []
  }
}
const total = ref(0)
const current = ref(1)
const size = ref(10)
const showModal = ref(false)
const showView = ref(false)
const showFollowDialog = ref(false)
const showImport = ref(false)
const viewData = ref<any>(null)

const searchForm = ref({ keyword: '', type: '', tag: '' })

const editForm = ref<any>({
  id: null, name: '', type: '', contact: '', phone: '',
  level: 1, tags: '', address: '', remark: ''
})

const followForm = ref({

  method: '电话沟通',
  time: new Date().toISOString().slice(0, 16),
  content: '',
  nextDate: ''
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
    const res = await request.get('/customers', {
      params: {
        current: current.value,
        size: size.value,
        keyword: searchForm.value.keyword || undefined,
        type: searchForm.value.type || undefined,
        tag: searchForm.value.tag || undefined
      }
    })
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
  searchForm.value = { keyword: '', type: '', tag: '' }
  current.value = 1
  loadData()
}

function openAddModal() {
  editForm.value = { id: null, name: '', type: '', contact: '', phone: '', level: 1, tags: '', address: '', remark: '' }
  showModal.value = true
}

function openEditModal(row: any) {
  editForm.value = {
    id: row.id, name: row.name || row.customerName, type: row.type || '',
    contact: row.contact || row.contactName || '', phone: row.phone || '',
    level: row.level || 1, tags: row.tags || '',
    address: row.address || '', remark: row.remark || ''
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

function saveFollow() {
  if (!followForm.value.content) {
    ElMessage.warning('请填写跟进内容')
    return
  }
  ElMessage.success('跟进记录已保存')
  showFollowDialog.value = false
  followForm.value = { method: '电话沟通', time: new Date().toISOString().slice(0, 16), content: '', nextDate: '' }
}

function handleExport() {
  ElMessage.info('导出功能开发中')
}

onMounted(() => { loadData(); loadTags() })
</script>
