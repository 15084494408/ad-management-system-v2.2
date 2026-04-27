<template>
  <div class="page-container">
    <div class="page-header"><span class="page-title">📦 物料库存</span><div class="page-actions"><el-button @click="exportData"><el-icon><Download /></el-icon> 导出</el-button><el-button type="primary" @click="addNew"><el-icon><Plus /></el-icon> 添加物料</el-button></div></div>

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
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.categoryName }}</el-tag>
            <el-tag v-if="row.paperGroup" size="small" type="info" style="margin-left:4px;" title="同纸张类型+材质共享库存">共享</el-tag>
          </template>
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
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" size="small" @click="stockIn(row)">入库</el-button>
            <el-button link type="warning" size="small" @click="stockOut(row)">出库</el-button>
            <el-button link type="primary" size="small" @click="editRow(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="deleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" :total="pagination.total"
          layout="total, prev, pager, next" @size-change="loadData" @current-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="showAdd" :title="editForm.id?'编辑物料':'添加物料'" width="550px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="物料分类">
          <el-select v-model="editForm.categoryId" style="width:100%;" @change="onCategoryChange">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-divider content-position="left">纸张属性（印刷类物料）</el-divider>
        <el-form-item label="纸张类型">
          <el-select v-model="editForm.paperType" placeholder="非印刷物料可不选" clearable style="width:100%;" @change="autoGenerateName">
            <el-option v-for="t in paperTypes" :key="t" :label="t" :value="t" />
            <template #footer>
              <div style="padding:0 12px 8px;">
                <el-input v-model="newPaperType" size="small" placeholder="输入新纸张类型后回车" @keyup.enter="addPaperType" />
              </div>
            </template>
          </el-select>
        </el-form-item>
        <el-form-item label="纸张材质">
          <el-select v-model="editForm.paperSpec" placeholder="非印刷物料可不选" clearable style="width:100%;" @change="autoGenerateName">
            <el-option v-for="s in paperSpecs" :key="s" :label="s" :value="s" />
            <template #footer>
              <div style="padding:0 12px 8px;">
                <el-input v-model="newPaperSpec" size="small" placeholder="输入新材质后回车" @keyup.enter="addPaperSpec" />
              </div>
            </template>
          </el-select>
        </el-form-item>
        <el-form-item label="色彩类型">
          <el-radio-group v-model="editForm.colourType" @change="autoGenerateName">
            <el-radio :label="0">黑白</el-radio>
            <el-radio :label="1">彩色</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="物料名称"><el-input v-model="editForm.name" placeholder="可手动修改自动生成的名称" /></el-form-item>
        <el-form-item label="物料编码"><el-input v-model="editForm.code" placeholder="可手动修改自动生成的编码" /></el-form-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'
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
const editForm = reactive<any>({ id: null, name: '', categoryId: null, categoryName: '', paperType: '', paperSpec: '', paperGroup: '', colourType: 0, spec: '', unit: '', price: 0, costPrice: 0, stockQuantity: 0, warningQuantity: 10 })
const stockForm = reactive<any>({ materialId: null, materialName: '', currentStock: 0, quantity: 1, remark: '' })

// 纸张类型和材质选项
const paperTypes = ref<string[]>(['A4', 'A3', 'SRA3'])
const paperSpecs = ref<string[]>(['80g双胶纸', '128g铜版纸', '157g铜版纸', '200g铜版纸', '250g铜版纸', '300g铜版纸'])
const newPaperType = ref('')
const newPaperSpec = ref('')

function addPaperType() {
  const v = newPaperType.value.trim()
  if (v && !paperTypes.value.includes(v)) { paperTypes.value.push(v); editForm.paperType = v; newPaperType.value = ''; autoGenerateName() }
}
function addPaperSpec() {
  const v = newPaperSpec.value.trim()
  if (v && !paperSpecs.value.includes(v)) { paperSpecs.value.push(v); editForm.paperSpec = v; newPaperSpec.value = ''; autoGenerateName() }
}
const colourLabel = (t: number) => t === 1 ? '彩色' : '黑白'
function autoGenerateName() {
  if (editForm.paperType && editForm.paperSpec) {
    editForm.name = `${editForm.paperType} - ${editForm.paperSpec} - ${colourLabel(editForm.colourType)}`
    const typeCode = editForm.paperType.toUpperCase()
    const specMatch = editForm.paperSpec.match(/(\d+)g(.*)/)
    let specCode = ''
    if (specMatch) {
      specCode = specMatch[1] + 'G'
      const m = specMatch[2]
      if (m.includes('铜版')) specCode += '-TB'
      else if (m.includes('双胶')) specCode += '-SJ'
      else if (m.includes('特种')) specCode += '-TZ'
      else if (m.includes('白卡')) specCode += '-BK'
      else specCode += '-QT'
    }
    // 纸张分组标识（同尺寸+材质共享库存，不含色彩后缀）
    editForm.paperGroup = `${typeCode}-${specCode}`
    editForm.code = `${typeCode}-${specCode}${editForm.colourType === 1 ? '-C' : '-B'}`
  }
}
function onCategoryChange() {
  const cat = categories.value.find((c: any) => c.id === editForm.categoryId)
  if (cat) editForm.categoryName = cat.name
}

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
function addNew() { Object.assign(editForm, { id: null, name: '', categoryId: null, categoryName: '', paperType: '', paperSpec: '', paperGroup: '', colourType: 0, spec: '', unit: '', price: 0, costPrice: 0, stockQuantity: 0, warningQuantity: 10 }); showAdd.value = true }
function editRow(row: any) { Object.assign(editForm, { id: null, name: '', categoryId: null, categoryName: '', paperType: '', paperSpec: '', paperGroup: '', colourType: 0, spec: '', unit: '', price: 0, costPrice: 0, stockQuantity: 0, warningQuantity: 10 }); Object.assign(editForm, row); showAdd.value = true }
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
async function deleteRow(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除物料「${row.name}」吗？`, '删除确认', { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' })
    await request.delete(`/material/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel' && e?.message !== 'cancel') ElMessage.error('删除失败')
  }
}
async function saveMaterial() {
  // ★ 根据 categoryId 自动同步 categoryName
  const cat = categories.value.find((c: any) => c.id === editForm.categoryId)
  if (cat) editForm.categoryName = cat.name
  try {
    if (editForm.id) {
      // 编辑：使用 PUT
      await request.put(`/material/${editForm.id}`, editForm)
    } else {
      // 新增：使用 POST
      await request.post('/material', editForm)
    }
    ElMessage.success('保存成功')
    showAdd.value = false
    loadData()
  } catch { ElMessage.error('保存失败') }
}
async function loadCategories() {
  try { const res = await request.get('/material/category'); categories.value = res.data || [] } catch {}
}
onMounted(() => { loadData(); loadCategories() })
</script>
