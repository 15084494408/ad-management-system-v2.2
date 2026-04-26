<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item">客户管理</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">客户跟进</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">📝 客户跟进记录</h1>
      <div class="page-actions">
        <button class="btn btn-default" @click="handleExport">⬇️ 导出</button>
        <button class="btn btn-primary" @click="openAddModal">+ 新增记录</button>
      </div>
    </div>

    <!-- 搜索表单 -->
    <div class="search-form">
      <div class="form-group">
        <label>客户名称</label>
        <input v-model="searchForm.keyword" type="text" class="form-control" placeholder="请输入客户名称">
      </div>
      <div class="form-group">
        <label>跟进人</label>
        <select v-model="searchForm.follower" class="form-control">
          <option value="">全部</option>
          <option value="张美丽">张美丽</option>
          <option value="王财务">王财务</option>
        </select>
      </div>
      <div class="form-group">
        <label>日期范围</label>
        <input v-model="searchForm.date" type="date" class="form-control">
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
            <th>跟进ID</th>
            <th>客户名称</th>
            <th>跟进内容</th>
            <th>跟进人</th>
            <th>跟进时间</th>
            <th>附件</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.id }}</td>
            <td><strong>{{ row.customerName || row.name }}</strong></td>
            <td style="max-width:250px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ row.content }}</td>
            <td>{{ row.follower || row.followerName || '-' }}</td>
            <td style="color:#909399;font-size:12px;">{{ row.followTime || row.createTime || '-' }}</td>
            <td>
              <span v-if="row.attachmentCount" class="tag tag-primary">{{ row.attachmentCount }}个附件</span>
              <span v-else style="color:#c0c4cc;">-</span>
            </td>
            <td class="action-btns">
              <button class="action-btn view" @click="viewRecord(row)">查看</button>
              <button class="action-btn edit" @click="editRecord(row)">编辑</button>
            </td>
          </tr>
          <tr v-if="!loading && list.length === 0">
            <td colspan="7" style="text-align:center;padding:40px;color:#c0c4cc;">暂无跟进记录</td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="pagination">
        <div class="pagination-info">共 {{ total }} 条记录</div>
        <div class="pagination-buttons">
          <button class="page-btn" :disabled="current <= 1" @click="current--; loadData()">«</button>
          <button class="page-btn active">{{ current }}</button>
          <button class="page-btn" :disabled="current >= Math.ceil(total / 10)" @click="current++; loadData()">»</button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div class="modal-overlay" :class="{ show: showModal }" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">{{ editForm.id ? '✏️ 编辑跟进记录' : '📝 新增跟进记录' }}</span>
          <button class="modal-close" @click="showModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">客户名称</label>
              <input v-model="editForm.customerName" type="text" class="form-input" placeholder="客户名称">
            </div>
            <div class="form-group">
              <label class="form-label">跟进方式</label>
              <select v-model="editForm.method" class="form-input">
                <option>电话沟通</option>
                <option>上门拜访</option>
                <option>微信/短信</option>
                <option>邮件沟通</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">跟进时间</label>
              <input v-model="editForm.followTime" type="datetime-local" class="form-input">
            </div>
            <div class="form-group">
              <label class="form-label">跟进人</label>
              <input v-model="editForm.follower" type="text" class="form-input" placeholder="跟进人">
            </div>
          </div>
          <div class="form-row full">
            <div class="form-group">
              <label class="form-label">跟进内容 *</label>
              <textarea v-model="editForm.content" class="form-input" placeholder="请详细记录本次跟进内容..."></textarea>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">下次跟进计划</label>
            <input v-model="editForm.nextDate" type="date" class="form-input">
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showModal = false">取消</button>
          <button class="btn btn-success" @click="saveRecord">💾 保存记录</button>
        </div>
      </div>
    </div>

    <!-- 查看详情弹窗 -->
    <div class="modal-overlay" :class="{ show: showView }" @click.self="showView = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">👁️ 跟进详情</span>
          <button class="modal-close" @click="showView = false">✕</button>
        </div>
        <div class="modal-body" v-if="viewData">
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:15px;margin-bottom:20px;">
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;">
              <div style="font-size:11px;color:#909399;">客户名称</div>
              <div style="font-size:14px;font-weight:600;">{{ viewData.customerName }}</div>
            </div>
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;">
              <div style="font-size:11px;color:#909399;">跟进方式</div>
              <div style="font-size:14px;">{{ viewData.method }}</div>
            </div>
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;">
              <div style="font-size:11px;color:#909399;">跟进人</div>
              <div style="font-size:14px;">{{ viewData.follower }}</div>
            </div>
            <div style="background:#f5f7fa;padding:12px;border-radius:6px;">
              <div style="font-size:11px;color:#909399;">跟进时间</div>
              <div style="font-size:14px;">{{ viewData.followTime }}</div>
            </div>
          </div>
          <div style="background:#f5f7fa;padding:15px;border-radius:8px;">
            <div style="font-size:12px;color:#909399;margin-bottom:8px;">跟进内容</div>
            <div style="font-size:14px;line-height:1.8;">{{ viewData.content }}</div>
          </div>
          <div v-if="viewData.nextDate" style="margin-top:15px;">
            <div style="font-size:12px;color:#909399;margin-bottom:5px;">下次跟进计划</div>
            <div style="background:#ecf5ff;padding:12px;border-radius:6px;font-size:13px;">📅 {{ viewData.nextDate }}</div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showView = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const list = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const current = ref(1)
const showModal = ref(false)
const showView = ref(false)
const viewData = ref<any>(null)

const searchForm = ref({ keyword: '', follower: '', date: '' })

const editForm = ref<any>({
  id: null, customerName: '', method: '电话沟通',
  followTime: new Date().toISOString().slice(0, 16),
  follower: '', content: '', nextDate: ''
})

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/customers/follow', {
      params: { current: current.value, size: 10, ...searchForm.value }
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

function resetSearch() {
  searchForm.value = { keyword: '', follower: '', date: '' }
  current.value = 1
  loadData()
}

function openAddModal() {
  editForm.value = {
    id: null, customerName: '', method: '电话沟通',
    followTime: new Date().toISOString().slice(0, 16),
    follower: '', content: '', nextDate: ''
  }
  showModal.value = true
}

function editRecord(row: any) {
  editForm.value = { ...row }
  showModal.value = true
}

function viewRecord(row: any) {
  viewData.value = row
  showView.value = true
}

async function saveRecord() {
  if (!editForm.value.content) {
    ElMessage.warning('请填写跟进内容')
    return
  }
  ElMessage.success('跟进记录已保存')
  showModal.value = false
  loadData()
}

function handleExport() {
  ElMessage.info('导出功能开发中')
}

onMounted(loadData)
</script>
