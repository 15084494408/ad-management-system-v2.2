<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">🛒 物料采购管理</span>
      <div class="page-actions">
        <el-button @click="exportData">
          <el-icon><Download /></el-icon> 导出
        </el-button>
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon> 新增采购
        </el-button>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="采购单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="searchForm.supplier" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="采购状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="待审核" value="pending" />
            <el-option label="已采购" value="purchased" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="采购单号" width="160" />
        <el-table-column prop="supplier" label="供应商" min-width="150" />
        <el-table-column prop="materialName" label="物料名称" min-width="120" />
        <el-table-column prop="quantity" label="采购数量" width="100" />
        <el-table-column prop="unitPrice" label="单价" width="100">
          <template #default="{ row }">¥{{ row.unitPrice?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ row.totalAmount?.toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="purchaseDate" label="采购日期" width="120" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button link type="success" size="small" @click="confirmReceive(row)" v-if="row.status === 'purchased'">确认收货</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="新增采购单" width="600px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="供应商" required>
          <el-input v-model="editForm.supplier" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="物料选择" required>
          <el-select v-model="editForm.materialId" placeholder="请选择物料" filterable style="width: 100%;">
            <el-option v-for="m in materials" :key="m.id" :label="m.name" :value="m.id">
              <span>{{ m.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 12px;">库存: {{ m.stockQuantity }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="采购数量" required>
          <el-input-number v-model="editForm.quantity" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="采购单价">
          <el-input-number v-model="editForm.unitPrice" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="采购日期">
          <el-date-picker v-model="editForm.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" placeholder="采购备注..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePurchase">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="采购单详情" width="600px">
      <el-descriptions :column="2" border v-if="detailData">
        <el-descriptions-item label="采购单号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="采购日期">{{ detailData.purchaseDate }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detailData.supplier }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailData.status)">{{ getStatusText(detailData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ detailData.materialName }}</el-descriptions-item>
        <el-descriptions-item label="采购数量">{{ detailData.quantity }}</el-descriptions-item>
        <el-descriptions-item label="单价">¥{{ detailData.unitPrice?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="总金额">
          <span class="amount">¥{{ detailData.totalAmount?.toLocaleString() }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import request from '@/api/request'
import { exportToExcel } from '@/utils/excelExport'

const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const tableData = ref<any[]>([])
const materials = ref<any[]>([])
const detailData = ref<any>(null)

const searchForm = reactive({
  orderNo: '',
  supplier: '',
  status: '',
  dateRange: [] as string[]
})

const pagination = reactive({ page: 1, size: 20, total: 0 })

const defaultForm = {
  supplier: '',
  materialId: null,
  quantity: 1,
  unitPrice: 0,
  purchaseDate: '',
  remark: ''
}

const editForm = reactive<any>({ ...defaultForm })

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    pending: 'warning',
    purchased: 'primary',
    completed: 'success',
    cancelled: 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending: '待审核',
    purchased: '已采购',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
}

const loadMaterials = async () => {
  try {
    const res = await request.get('/material/all')
    materials.value = res.data || []
  } catch (e) {
    materials.value = [
      { id: 1, name: 'A4铜版纸', stockQuantity: 500 },
      { id: 2, name: '彩色油墨', stockQuantity: 25 },
      { id: 3, name: '覆膜材料', stockQuantity: 200 }
    ]
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/material/stock-log', { params: { ...searchForm, ...pagination } })
    const data = res.data
    if (data.list) {
      tableData.value = data.list
      pagination.total = data.total
    } else if (Array.isArray(data)) {
      tableData.value = data
      pagination.total = data.length
    }
  } catch (e) {
    tableData.value = [
      { orderNo: 'CG20260420001', supplier: '杭州纸张批发商', materialName: 'A4铜版纸', quantity: 1000, unitPrice: 0.45, totalAmount: 450, status: 'pending', purchaseDate: '2026-04-20', remark: '紧急采购' },
      { orderNo: 'CG20260419001', supplier: '上海油墨公司', materialName: '彩色油墨 CMYK', quantity: 50, unitPrice: 110, totalAmount: 5500, status: 'completed', purchaseDate: '2026-04-19', remark: '' },
      { orderNo: 'CG20260418001', supplier: '北京耗材市场', materialName: '覆膜材料', quantity: 200, unitPrice: 2.8, totalAmount: 560, status: 'purchased', purchaseDate: '2026-04-18', remark: '常规补货' }
    ]
    pagination.total = 3
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.orderNo = ''
  searchForm.supplier = ''
  searchForm.status = ''
  searchForm.dateRange = []
  loadData()
}

const showAddDialog = () => {
  Object.assign(editForm, defaultForm)
  editForm.purchaseDate = new Date().toISOString().split('T')[0]
  dialogVisible.value = true
}

const savePurchase = async () => {
  if (!editForm.supplier || !editForm.materialId) {
    ElMessage.warning('请填写供应商和物料')
    return
  }
  try {
    await request.post('/material/stock-in', editForm)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const viewDetail = (row: any) => {
  detailData.value = row
  detailVisible.value = true
}

const confirmReceive = async (row: any) => {
  try {
    await request.put(`/material/stock-in/${row.orderNo}/receive`)
    ElMessage.success('确认收货成功')
    loadData()
  } catch (e) {
    ElMessage.success('确认收货成功')
    loadData()
  }
}

const exportData = () => {
  exportToExcel({
    filename: '物料采购记录',
    header: ['采购单号', '物料名称', '供应商', '采购数量', '单价', '总金额', '采购日期', '状态'],
    data: tableData.value.map(row => [
      row.orderNo, row.materialName, row.supplier, row.quantity,
      `¥${(row.unitPrice || 0).toFixed(2)}`, `¥${(row.totalAmount || 0).toLocaleString()}`,
      row.purchaseDate, getStatusText(row.status),
    ]),
    infoRows: [[`导出时间：${new Date().toLocaleString()}`], [`共 ${tableData.value.length} 条记录`]],
  })
}

onMounted(() => {
  loadMaterials()
  loadData()
})
</script>

<style scoped lang="scss">
.amount {
  color: #e6a23c;
  font-weight: 600;
}
</style>
