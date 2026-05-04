<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">🔔 系统通知</span>
      <div class="page-actions">
        <el-button @click="markAllRead"><el-icon><Check /></el-icon> 全部已读</el-button>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="通知类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable style="width:140px;">
            <el-option label="系统公告" value="system" />
            <el-option label="更新通知" value="update" />
            <el-option label="维护通知" value="maintenance" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.read" placeholder="全部" clearable style="width:100px;">
            <el-option label="未读" value="0" />
            <el-option label="已读" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="notice-list" v-loading="loading">
      <div v-for="item in noticeList" :key="item.id" class="notice-item" :class="{ unread: !item.read }">
        <div class="notice-icon">{{ { system:'📢', update:'🔄', maintenance:'🔧' }[item.type] || '🔔' }}</div>
        <div class="notice-content">
          <div class="notice-title">{{ item.title }}</div>
          <div class="notice-body">{{ item.content }}</div>
          <div class="notice-meta">
            <span>{{ item.createTime }}</span>
            <el-tag v-if="!item.read" size="small" type="danger">未读</el-tag>
          </div>
        </div>
        <div class="notice-actions">
          <el-button v-if="!item.read" size="small" type="primary" link @click="markRead(item)">标记已读</el-button>
          <el-button size="small" type="danger" link @click="deleteNotice(item)">删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Check } from '@element-plus/icons-vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const noticeList = ref<any[]>([])
const searchForm = ref({ type: '', read: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/notice', { params: { ...searchForm.value, type: 'system' } })
    noticeList.value = res.data?.records || res.data || []
  } catch {}
  loading.value = false
}

function resetSearch() { searchForm.value = { type: '', read: '' }; loadData() }
function markRead(item: any) { request.put(`/notice/${item.id}/read`).then(() => { item.isRead = 1; ElMessage.success('已标记已读') }).catch(() => {}) }
function markAllRead() { request.put('/notice/read-all').then(() => { noticeList.value.forEach((n: any) => n.isRead = 1); ElMessage.success('全部已读') }).catch(() => {}) }
function deleteNotice(item: any) { request.delete(`/notice/${item.id}`).then(() => { noticeList.value = noticeList.value.filter((n: any) => n.id !== item.id); ElMessage.success('已删除') }).catch(() => {}) }
onMounted(loadData)
</script>

<style scoped lang="scss">
.notice-list { display: flex; flex-direction: column; gap: 12px; }
.notice-item { display: flex; gap: 15px; padding: 16px 20px; background: #fff; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); transition: all 0.2s; align-items: flex-start; }
.notice-item.unread { border-left: 4px solid #409eff; background: #f5f9ff; }
.notice-item:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.notice-icon { font-size: 24px; flex-shrink: 0; margin-top: 2px; }
.notice-content { flex: 1; }
.notice-title { font-size: 15px; font-weight: 600; margin-bottom: 6px; }
.notice-body { font-size: 13px; color: #606266; line-height: 1.6; margin-bottom: 8px; }
.notice-meta { display: flex; align-items: center; gap: 10px; font-size: 12px; color: #909399; }
.notice-actions { flex-shrink: 0; display: flex; gap: 8px; align-items: center; }
</style>
