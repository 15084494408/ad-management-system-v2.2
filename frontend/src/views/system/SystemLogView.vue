<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item">系统管理</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item active">操作日志</span>
    </div>

    <div class="page-header">
      <span class="page-title">📝 操作日志</span>
      <div class="page-actions">
        <el-button @click="showExportDialog=true"><el-icon><Download /></el-icon> 导出日志</el-button>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="操作人"><el-input v-model="searchForm.username" placeholder="请输入操作人" clearable /></el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.module" placeholder="全部" clearable style="width:150px;">
            <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="handleSearch">🔍 搜索</el-button></el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="日志ID" width="80" />
        <el-table-column prop="username" label="操作人" width="100" />
        <el-table-column prop="module" label="操作类型" width="110">
          <template #default="{ row }"><el-tag size="small" type="primary">{{ row.module }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="description" label="操作内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="130" />
        <el-table-column prop="createTime" label="操作时间" width="170" />
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{ row }"><el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button></template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" :total="pagination.total"
          layout="total, prev, pager, next" @size-change="loadData" @current-change="loadData" />
      </div>
    </div>

    <!-- 日志详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="📝 日志详情" width="550px">
      <el-descriptions :column="2" border v-if="currentLog">
        <el-descriptions-item label="日志ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog.username }}</el-descriptions-item>
        <el-descriptions-item label="操作类型"><el-tag size="small" type="primary">{{ currentLog.module }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ip }}</el-descriptions-item>
        <el-descriptions-item label="操作内容" :span="2">{{ currentLog.description }}</el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ currentLog.createTime }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2" v-if="currentLog.param">
          <pre style="max-height:150px;overflow:auto;margin:0;font-size:12px;">{{ currentLog.param }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="showDetailDialog=false">关闭</el-button></template>
    </el-dialog>

    <!-- 导出日志弹窗 -->
    <el-dialog v-model="showExportDialog" title="⬇️ 导出操作日志" width="500px" destroy-on-close>
      <el-form label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始日期"><el-date-picker v-model="exportForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width:100%;" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期"><el-date-picker v-model="exportForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width:100%;" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="操作类型">
              <el-select v-model="exportForm.module" placeholder="全部" clearable style="width:100%;">
                <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="导出格式">
              <el-select v-model="exportForm.format" style="width:100%;">
                <el-option label="Excel (.xlsx)" value="xlsx" />
                <el-option label="CSV (.csv)" value="csv" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showExportDialog=false">取消</el-button>
        <el-button type="primary" @click="doExport" :loading="exporting">⬇️ 导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import request from '@/api/request'
import { exportToExcel } from '@/utils/excelExport'

const loading = ref(false)
const showDetailDialog = ref(false)
const showExportDialog = ref(false)
const exporting = ref(false)
const currentLog = ref<any>(null)
const modules = ref(['用户管理','订单管理','财务管理','客户管理','物料管理','系统配置','设计文件'])
const dateRange = ref<any[]>([])
const tableData = ref<any[]>([])
const searchForm = reactive({ username: '', module: '' })
const pagination = reactive({ page: 1, size: 20, total: 0 })
const exportForm = reactive({ startDate: '', endDate: '', module: '', format: 'xlsx' })

async function loadData() {
  loading.value = true
  try {
    const params: any = { current: pagination.page, size: pagination.size }
    if (searchForm.username) params.username = searchForm.username
    if (searchForm.module) params.module = searchForm.module
    if (dateRange.value?.length) { params.startDate = dateRange.value[0]; params.endDate = dateRange.value[1] }
    const res = await request.get('/system/logs', { params })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch { tableData.value = [] }
  loading.value = false
}

function handleSearch() { pagination.page = 1; loadData() }
function showDetail(row: any) { currentLog.value = row; showDetailDialog.value = true }

async function doExport() {
  exporting.value = true
  try {
    // 按弹窗筛选条件过滤数据（如果后端支持全量查询，这里可以直接用全部数据）
    let filteredData = [...tableData.value]
    if (exportForm.module) {
      filteredData = filteredData.filter((r: any) => r.module === exportForm.module)
    }
    if (exportForm.startDate || exportForm.endDate) {
      filteredData = filteredData.filter((r: any) => {
        if (!r.createTime) return false
        const d = String(r.createTime).slice(0, 10)
        if (exportForm.startDate && d < exportForm.startDate) return false
        if (exportForm.endDate && d > exportForm.endDate) return false
        return true
      })
    }
    // 如果当前页数据不足，尝试加载全部数据用于导出
    if (filteredData.length === 0 || filteredData.length < pagination.total) {
      try {
        const params: any = { current: 1, size: pagination.total || 1000 }
        if (searchForm.username) params.username = searchForm.username
        if (searchForm.module) params.module = searchForm.module
        if (dateRange.value?.length) { params.startDate = dateRange.value[0]; params.endDate = dateRange.value[1] }
        if (exportForm.startDate) params.startDate = exportForm.startDate
        if (exportForm.endDate) params.endDate = exportForm.endDate
        if (exportForm.module) params.module = exportForm.module
        const res = await request.get('/system/logs', { params })
        filteredData = res.data?.records || tableData.value
      } catch { /* use existing data */ }
    }

    exportToExcel({
      filename: '操作日志',
      header: ['日志ID', '操作人', '模块', '操作类型', 'IP地址', '详情', '操作时间'],
      data: filteredData.map((row: any) => [
        row.id, row.username || '-', row.module || '-', row.description?.slice(0, 50) || '-',
        row.ip || '-', row.param ? JSON.stringify(row.param).slice(0, 100) : '-',
        (row.createTime || '').toString(),
      ]),
      infoRows: [
        [`导出时间：${new Date().toLocaleString()}`],
        [`筛选条件：${exportForm.module ? `模块=${exportForm模块}` : '全部'} ${exportForm.startDate ? `${exportForm.startDate} ~ ${exportForm.endDate || ''}` : ''}`.trim()],
        [`共 ${filteredData.length} 条记录`],
      ],
    })
    ElMessage.success('导出成功！')
    showExportDialog.value = false
  } catch (e) { ElMessage.error('导出失败') }
  exporting.value = false
}

onMounted(loadData)
</script>
