<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">📤 流水导出</span>
    </div>

    <div class="card" style="max-width: 700px;">
      <el-form :model="exportForm" label-width="120px" style="padding: 20px;">
        <el-form-item label="导出范围">
          <el-radio-group v-model="exportForm.range">
            <el-radio value="all">全部数据</el-radio>
            <el-radio value="date">按日期范围</el-radio>
            <el-radio value="type">按流水类型</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="exportForm.range === 'date'" label="日期范围">
          <el-date-picker v-model="exportForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:100%;" />
        </el-form-item>
        <el-form-item v-if="exportForm.range === 'type'" label="流水类型">
          <el-select v-model="exportForm.flowType" placeholder="请选择" style="width:100%;">
            <el-option v-for="t in flowTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="导出格式">
          <el-radio-group v-model="exportForm.format">
            <el-radio value="xlsx">Excel (.xlsx)</el-radio>
            <el-radio value="csv">CSV (.csv)</el-radio>
            <el-radio value="pdf">PDF (.pdf)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="包含字段">
          <el-checkbox-group v-model="exportForm.fields">
            <el-checkbox value="date" label="日期" />
            <el-checkbox value="type" label="类型" />
            <el-checkbox value="amount" label="金额" />
            <el-checkbox value="remark" label="备注" />
            <el-checkbox value="operator" label="操作人" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="exporting" @click="doExport">
            <el-icon><Download /></el-icon> 开始导出
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider />
      <div class="export-history">
        <h4 style="margin-bottom:12px;">📋 导出记录</h4>
        <el-table :data="historyData" size="small" stripe>
          <el-table-column prop="fileName" label="文件名" min-width="200" />
          <el-table-column prop="format" label="格式" width="80" />
          <el-table-column prop="size" label="大小" width="100" />
          <el-table-column prop="exportTime" label="导出时间" width="160" />
          <el-table-column prop="operator" label="操作人" width="100" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="downloadFile(row)">下载</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { exportToExcel } from '@/utils/excelExport'

const exporting = ref(false)
const flowTypes = [
  { label: '现金收入', value: 'cash_in' }, { label: '微信收入', value: 'wechat_in' },
  { label: '物料采购', value: 'material' }, { label: '人工费用', value: 'labor' },
]
const exportForm = reactive({
  range: 'all', dateRange: [], flowType: '', format: 'xlsx',
  fields: ['date', 'type', 'amount', 'remark', 'operator'],
})
const historyData = ref<any[]>([])

async function doExport() {
  exporting.value = true
  try {
    // 构建筛选参数用于文件名
    const rangeLabel = exportForm.range === 'all' ? '全部' : `${exportForm.dateRange?.[0] || ''} 至 ${exportForm.dateRange?.[1] || ''}`
    const typeLabel = exportForm.flowType || '全部类型'

    // 调用接口获取数据
    const params: any = { current: 1, size: 10000 }
    if (exportForm.dateRange?.length === 2) {
      params.startDate = exportForm.dateRange[0]
      params.endDate = exportForm.dateRange[1]
    }
    if (exportForm.direction) params.direction = exportForm.direction

    const res: any = await request.get('/finance/all-flow', { params })
    const list = res.data?.records || []

    if (!list.length) { ElMessage.warning('没有可导出的数据'); exporting.value = false; return }

    // 根据选择的字段动态构建表头和数据
    const allFields = [
      { key: 'record_no', label: '编号' },
      { key: 'amount', label: '金额(¥)' },
      { key: 'direction', label: '方向', fmt: (v: string) => v === 'income' ? '收入' : '支出' },
      { key: 'source', label: '来源' },
      { key: 'category', label: '分类' },
      { key: 'related_name', label: '关联对象' },
      { key: 'payment_method', label: '支付方式' },
      { key: 'remark', label: '备注' },
      { key: 'create_time', label: '时间' },
    ]

    // 如果用户选择了特定字段，则过滤
    const fieldsToExport = exportForm.fields.length > 0
      ? allFields.filter(f => exportForm.fields.includes(f.key) || exportForm.fields.includes(f.label))
      : allFields

    const header = fieldsToExport.map(f => f.label)
    const data = list.map(row => fieldsToExport.map(f => f.fmt ? f.fmt(row[f.key]) : (row[f.key] ?? '-')))

    exportToExcel({
      filename: `流水导出_${typeLabel}`,
      title: `流水导出报表（${rangeLabel}）`,
      header,
      data,
      summaryRow: ['', '', '', `共 ${list.length} 笔记录`],
    })

    ElMessage.success(`成功导出 ${list.length} 条记录`)
    loadHistory()
  } catch {
    ElMessage.error('导出失败，请重试')
  }
  exporting.value = false
}

function downloadFile(row: any) { ElMessage.info('下载: ' + row.fileName) }

async function loadHistory() {
  historyData.value = [
    { fileName: '每日流水_2026年4月.xlsx', format: 'xlsx', size: '256 KB', exportTime: '2026-04-18 14:30', operator: '管理员' },
    { fileName: '每日流水_2026年3月.xlsx', format: 'xlsx', size: '312 KB', exportTime: '2026-04-01 09:15', operator: '管理员' },
    { fileName: '每日流水_2026Q1汇总.pdf', format: 'pdf', size: '1.2 MB', exportTime: '2026-04-02 16:00', operator: '财务专员' },
  ]
}

onMounted(loadHistory)
</script>
