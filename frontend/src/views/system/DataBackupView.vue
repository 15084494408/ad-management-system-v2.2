<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item">系统管理</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item active">数据备份</span>
    </div>

    <div class="page-header">
      <span class="page-title">💾 数据备份</span>
      <div class="page-actions">
        <el-button type="primary" @click="showBackupConfirm=true" :loading="backing">
          <el-icon><FolderAdd /></el-icon> 备份当前数据库
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">📦</div>
        <div class="stat-info"><div class="stat-value">{{ stats.totalCount }}</div><div class="stat-label">备份文件数</div></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">💾</div>
        <div class="stat-info"><div class="stat-value">{{ stats.totalSize }}</div><div class="stat-label">总备份大小</div></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">🔄</div>
        <div class="stat-info"><div class="stat-value">{{ autoTime }}</div><div class="stat-label">自动备份时间</div></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">⏱️</div>
        <div class="stat-info"><div class="stat-value">{{ retainDays }}</div><div class="stat-label">备份保留期</div></div>
      </div>
    </div>

    <div class="card">
      <div class="card-header-row"><span class="card-title">📋 备份记录</span></div>
      <el-table :data="backupList" stripe v-loading="loading">
        <el-table-column prop="id" label="备份编号" width="160" />
        <el-table-column prop="type" label="备份类型" width="110">
          <template #default="{ row }"><el-tag :type="row.type==='auto'?'success':'primary'" size="small">{{ row.type==='auto'?'自动备份':'手动备份' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="备份时间" width="170" />
        <el-table-column prop="fileSize" label="文件大小" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status==='success'?'success':'danger'" size="small">{{ row.status==='success'?'成功':'失败' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="downloadBackup(row)">下载</el-button>
            <el-button size="small" type="warning" link @click="restoreBackup(row)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { FolderAdd } from '@element-plus/icons-vue'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const backing = ref(false)
const showBackupConfirm = ref(false)
const autoTime = ref('每日凌晨2点')
const retainDays = ref('30天')
const stats = reactive({ totalCount: 30, totalSize: '2.5GB' })
const backupList = ref<any[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/system/backup/list')
    backupList.value = res.data?.records || res.data || []
    stats.totalCount = backupList.value.length
  } catch {}
  loading.value = false
}

async function startBackup() {
  backing.value = true
  try {
    await request.post('/system/backup/manual')
    ElMessage.success('数据库备份完成！')
    showBackupConfirm.value = false
    setTimeout(loadData, 3000)
  } catch { ElMessage.error('备份失败') }
  backing.value = false
}

function downloadBackup(row: any) {
  ElMessageBox.confirm(
    `确认下载备份文件 ${row.id}？\n文件大小约 ${row.fileSize}，请确认下载。`,
    '⬇️ 下载备份文件',
    { type: 'info' }
  ).then(() => {
    ElMessage.info('下载开始...')
  }).catch(() => {})
}

async function restoreBackup(row: any) {
  try {
    await ElMessageBox.confirm(
      `⚠️ 警告：此操作不可逆！\n恢复数据库将覆盖当前所有数据，建议恢复前先备份当前数据库。\n\n备份编号：${row.id}\n备份时间：${row.createTime}`,
      '🔄 恢复数据备份',
      { type: 'warning', confirmButtonText: '🔄 确认恢复', cancelButtonText: '取消' }
    )
    ElMessage.warning('数据恢复中...请勿关闭页面')
    await request.post('/system/backup/restore/' + row.id)
    ElMessage.success('数据已成功恢复！')
  } catch {}
}

onMounted(loadData)
</script>
