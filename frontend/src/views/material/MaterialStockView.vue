<template>
  <div class="page-container">
    <div class="page-header"><span class="page-title">📦 物料库存</span><div class="page-actions"><el-button @click="exportData"><el-icon><Download /></el-icon> 导出</el-button><el-button type="primary" @click="showAdd=true"><el-icon><Plus /></el-icon> 添加物料</el-button></div></div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="物料名称"><el-input v-model="searchForm.name" placeholder="请输入" clearable /></el-form-item>
        <el-form-item label="物料类型">
          <el-select v-model="searchForm.categoryId" placeholder="全部" clearable style="width:150px;">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存状态">
          <el-select v-model="searchForm.warning" placeholder="全部" clearable style="width:120px;">
            <el-option label="正常" value="normal" />
            <el-option label="预警" value="warning" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">搜索</el-button><el-button @click="resetSearch">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="物料名称" min-width="120" />
        <el-table-column prop="categoryName" label="分类" width="100">
          <template #default="{ row }"><el-tag size="small" type="primary">{{ row.categoryName }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="unit" label="单位" width="60" />
        <el-table-column prop="stockQuantity" label="库存数量" width="100">
          <template #default="{ row }"><span :style="{color: row.stockQuantity <= row.warningQuantity?'#f56c6c':'inherit',fontWeight: row.stockQuantity <= row.warningQuantity?700:400}">{{ row.stockQuantity }}</span></template>
        </el-table-column>
        <el-table-column prop="warningQuantity" label="预警值" width="80" />
        <el-table-column prop="price" label="单价" width="100">
          <template #default="{ row }">¥{{ row.price?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.stockQuantity <= row.warningQuantity?'danger':'success'" size="small">{{ row.stockQuantity <= row.warningQuantity?'⚠️ 预警':'正常' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" size="small" @click="stockIn(row)">入库</el-button>
            <el-button link type="warning" size="small" @click="stockOut(row)">出库</el-button>
            <el-button link type="primary" size="small" @click="editRow(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" :total="pagination.total"
          layout="total, prev, pager, next" @size-change="loadData" @current-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="showAdd" :title="editForm.id?'编辑物料':'添加物料'" width="500px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="物料名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="物料分类">
          <el-select v-model="editForm.categoryId" style="width:100%;">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格"><el-input v-model="editForm.spec" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="editForm.unit" /></el-form-item>
        <el-form-item label="零售价"><el-input-number v-model="editForm.price" :min="0" :precision="2" style="width:100%;" /></el-form-item>
        <el-form-item label="成本价"><el-input-number v-model="editForm.costPrice" :min="0" :precision="2" style="width:100%;" /></el-form-item>
        <el-form-item label="库存数量"><el-input-number v-model="editForm.stockQuantity" :min="0" style="width:100%;" /></el-form-item>
        <el-form-item label="预警值"><el-input-number v-model="editForm.warningQuantity" :min="0" style="width:100%;" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showAdd=false">取消</el-button><el-button type="primary" @click="saveMaterial">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="showStock" :title="stockType==='in'?'入库':'出库'" width="400px">
      <el-form :model="stockForm" label-width="90px">
        <el-form-item label="物料">{{ stockForm.materialName }}</el-form-item>
        <el-form-item label="当前库存">{{ stockForm.currentStock }}</el-form-item>
        <el-form-item label="数量"><el-input-number v-model="stockForm.quantity" :min="1" style="width:100%;" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="stockForm.remark" type="textarea" rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showStock=false">取消</el-button><el-button type="primary" @click="submitStock">{{ stockType==='in'?'确认入库':'确认出库' }}</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const showAdd = ref(false)
const showStock = ref(false)
const stockType = ref('in')
const tableData = ref<any[]>([])
const categories = ref<any[]>([])
const searchForm = reactive({ name: '', categoryId: null, warning: '' })
const pagination = reactive({ page: 1, size: 20, total: 0 })
const editForm = reactive<any>({ id: null, name: '', categoryId: null, spec: '', unit: '', price: 0, costPrice: 0, stockQuantity: 0, warningQuantity: 10 })
const stockForm = reactive<any>({ materialId: null, materialName: '', currentStock: 0, quantity: 1, remark: '' })

async function loadData() {
  loading.value = true
  try {
    const params: any = { current: pagination.page, size: pagination.size }
    if (searchForm.name) params.name = searchForm.name
    if (searchForm.categoryId) params.categoryId = searchForm.categoryId
    const res = await request.get('/material', { params })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch { tableData.value = [] }
  loading.value = false
}

function resetSearch() { searchForm.name = ''; searchForm.categoryId = null; searchForm.warning = ''; loadData() }
function editRow(row: any) { Object.assign(editForm, row); showAdd.value = true }
function stockIn(row: any) { stockType.value = 'in'; Object.assign(stockForm, { materialId: row.id, materialName: row.name, currentStock: row.stockQuantity, quantity: 1, remark: '' }); showStock.value = true }
function stockOut(row: any) { stockType.value = 'out'; Object.assign(stockForm, { materialId: row.id, materialName: row.name, currentStock: row.stockQuantity, quantity: 1, remark: '' }); showStock.value = true }
async function submitStock() {
  try {
    if (stockType.value === 'out' && stockForm.quantity > stockForm.currentStock) { ElMessage.warning('出库数量不能超过库存'); return }
    const api = stockType.value === 'in' ? '/material/stock-in' : '/material/stock-out'
    await request.post(api, stockForm)
    ElMessage.success(stockType.value === 'in' ? '入库成功' : '出库成功')
    showStock.value = false
    loadData()
  } catch { ElMessage.error('操作失败') }
}
async function saveMaterial() {
  try { await request.post('/material', editForm); ElMessage.success('保存成功'); showAdd.value = false; loadData() } catch { ElMessage.error('保存失败') }
}
async function loadCategories() {
  try { const res = await request.get('/material/category'); categories.value = res.data || [] } catch {}
}
onMounted(() => { loadData(); loadCategories() })
</script>
