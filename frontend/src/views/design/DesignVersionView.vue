<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">🔄 版本控制</span>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="文件名称">
          <el-input v-model="searchForm.fileName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="关联订单">
          <el-input v-model="searchForm.orderId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="versionList" stripe v-loading="loading" row-key="id" default-expand-all :tree-props="{ children: 'versions' }">
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column prop="fileSize" label="大小" width="100" />
        <el-table-column prop="designerName" label="修改人" width="100" />
        <el-table-column prop="changeLog" label="变更说明" min-width="180" />
        <el-table-column prop="createTime" label="修改时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="downloadVersion(row)">下载</el-button>
            <el-button size="small" type="warning" link @click="compareVersion(row)">对比</el-button>
            <el-button size="small" type="success" link @click="rollbackVersion(row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const searchForm = ref({ fileName: '', orderId: '' })
const versionList = ref<any[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/design/file', { params: searchForm.value })
    versionList.value = res.data?.records || res.data || []
  } catch {
    ElMessage.error('加载版本数据失败')
  }
  loading.value = false
}

function resetSearch() { searchForm.value = { fileName: '', orderId: '' }; loadData() }
function downloadVersion(row: any) { ElMessage.info('下载版本: ' + row.version) }
function compareVersion(row: any) { ElMessage.info('对比版本: ' + row.version) }
function rollbackVersion(row: any) { ElMessage.warning('回滚到版本: ' + row.version) }

onMounted(loadData)
</script>
