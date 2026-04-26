<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">📦 物料总览</span>
      <div class="page-actions">
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
        <el-button @click="goToPurchase">
          <el-icon><ShoppingCart /></el-icon> 快速采购
        </el-button>
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon> 添加物料
        </el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">📦</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalCount }}</div>
          <div class="stat-label">物料种类</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">✅</div>
        <div class="stat-info">
          <div class="stat-value" style="color: #67c23a;">{{ normalCount }}</div>
          <div class="stat-label">库存正常</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">⚠️</div>
        <div class="stat-info">
          <div class="stat-value" style="color: #f56c6c;">{{ warningCount }}</div>
          <div class="stat-label">预警物料</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">💰</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalValue?.toLocaleString() }}</div>
          <div class="stat-label">库存总值</div>
        </div>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="物料名称">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="物料分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部" clearable style="width: 150px;">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存状态">
          <el-select v-model="searchForm.warning" placeholder="全部" clearable style="width: 120px;">
            <el-option label="正常" value="normal" />
            <el-option label="预警" value="warning" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="material-grid" v-loading="loading">
      <div v-for="item in tableData" :key="item.id" class="material-card">
        <div class="material-card-header" :class="item.stockQuantity <= item.warningQuantity ? 'warning-bg' : 'normal-bg'">
          <span class="material-icon">{{ item.stockQuantity <= item.warningQuantity ? '⚠️' : '📦' }}</span>
        </div>
        <div class="material-card-body">
          <div class="material-name">{{ item.name }}</div>
          <div class="material-tags">
            <el-tag size="small" type="primary">{{ item.categoryName }}</el-tag>
            <el-tag size="small" :type="item.stockQuantity <= item.warningQuantity ? 'danger' : 'success'">
              {{ item.stockQuantity <= item.warningQuantity ? '⚠️ 预警' : '✅ 正常' }}
            </el-tag>
          </div>
          <div class="material-info-row">
            <span class="info-label">当前库存</span>
            <span class="info-value" :class="item.stockQuantity <= item.warningQuantity ? 'warning-text' : ''">
              {{ item.stockQuantity }} {{ item.unit }}
            </span>
          </div>
          <div class="material-info-row">
            <span class="info-label">预警值</span>
            <span class="info-value">{{ item.warningQuantity }} {{ item.unit }}</span>
          </div>
          <div class="material-price-row">
            <div class="price-item">
              <span class="price-label">零售价</span>
              <span class="price-value cost">¥{{ item.price?.toFixed(2) }}</span>
            </div>
            <div class="price-item">
              <span class="price-label">成本价</span>
              <span class="price-value factory">¥{{ item.costPrice?.toFixed(2) || '-' }}</span>
            </div>
          </div>
          <div class="material-value">
            <span class="info-label">库存价值</span>
            <span class="info-value">¥{{ ((item.price || 0) * item.stockQuantity)?.toFixed(2) }}</span>
          </div>
          <div class="material-actions">
            <el-button size="small" @click="stockIn(item)">入库</el-button>
            <el-button type="warning" size="small" @click="stockOut(item)">出库</el-button>
            <el-button type="primary" size="small" @click="editMaterial(item)">编辑</el-button>
          </div>
        </div>
      </div>

      <div v-if="tableData.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无物料数据" />
      </div>
    </div>

    <div class="pagination-wrap" v-if="pagination.total > 0">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editForm.id ? '编辑物料' : '添加物料'" width="500px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="物料名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="物料分类">
          <el-select v-model="editForm.categoryId" style="width: 100%;">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格"><el-input v-model="editForm.spec" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="editForm.unit" /></el-form-item>
        <el-form-item label="零售价"><el-input-number v-model="editForm.price" :min="0" :precision="2" style="width: 100%;" /></el-form-item>
        <el-form-item label="成本价"><el-input-number v-model="editForm.costPrice" :min="0" :precision="2" style="width: 100%;" /></el-form-item>
        <el-form-item label="库存数量"><el-input-number v-model="editForm.stockQuantity" :min="0" style="width: 100%;" /></el-form-item>
        <el-form-item label="预警值"><el-input-number v-model="editForm.warningQuantity" :min="0" style="width: 100%;" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMaterial">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stockDialogVisible" :title="stockType === 'in' ? '物料入库' : '物料出库'" width="400px">
      <el-form :model="stockForm" label-width="90px">
        <el-form-item label="物料">{{ stockForm.materialName }}</el-form-item>
        <el-form-item label="当前库存">{{ stockForm.currentStock }} {{ stockForm.unit }}</el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="stockForm.quantity" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stockForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStock">{{ stockType === 'in' ? '确认入库' : '确认出库' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, ShoppingCart, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const dialogVisible = ref(false)
const stockDialogVisible = ref(false)
const tableData = ref<any[]>([])
const categories = ref<any[]>([])

const searchForm = reactive({
  name: '',
  categoryId: null,
  warning: ''
})

const pagination = reactive({ page: 1, size: 12, total: 0 })

const stockType = ref('in')
const stockForm = reactive<any>({
  materialId: null,
  materialName: '',
  currentStock: 0,
  unit: '',
  quantity: 1,
  remark: ''
})

const defaultForm = {
  id: null as number | null,
  name: '',
  categoryId: null,
  spec: '',
  unit: '',
  price: 0,
  costPrice: 0,
  stockQuantity: 0,
  warningQuantity: 10
}

const editForm = reactive<any>({ ...defaultForm })

const totalCount = computed(() => tableData.value.length)
const normalCount = computed(() => tableData.value.filter(t => t.stockQuantity > t.warningQuantity).length)
const warningCount = computed(() => tableData.value.filter(t => t.stockQuantity <= t.warningQuantity).length)
const totalValue = computed(() => tableData.value.reduce((sum, t) => sum + (t.price || 0) * t.stockQuantity, 0))

const loadCategories = async () => {
  try {
    const res = await request.get('/material/category')
    categories.value = res.data || []
  } catch (e) {
    categories.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/material', {
      params: {
        current: pagination.page,
        size: pagination.size,
        name: searchForm.name || undefined,
        categoryId: searchForm.categoryId || undefined,
        warning: searchForm.warning || undefined
      }
    })
    // res 是 Result 对象，res.data 是 PageResult { total, current, size, records }
    const pageResult = res.data
    if (pageResult && Array.isArray(pageResult.records)) {
      tableData.value = pageResult.records
      pagination.total = pageResult.total || 0
    } else if (Array.isArray(pageResult)) {
      tableData.value = pageResult
      pagination.total = pageResult.length
    } else {
      tableData.value = []
      pagination.total = 0
    }
  } catch (e) {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.name = ''
  searchForm.categoryId = null
  searchForm.warning = ''
  loadData()
}

const refreshData = () => {
  loadData()
  ElMessage.success('刷新成功')
}

const goToPurchase = () => {
  router.push('/materials/purchase')
}

const showAddDialog = () => {
  Object.assign(editForm, defaultForm)
  dialogVisible.value = true
}

const editMaterial = (item: any) => {
  Object.assign(editForm, item)
  dialogVisible.value = true
}

const saveMaterial = async () => {
  if (!editForm.name) {
    ElMessage.warning('请输入物料名称')
    return
  }
  try {
    if (editForm.id) {
      await request.put(`/material/${editForm.id}`, editForm)
    } else {
      await request.post('/material', editForm)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {
    // 响应拦截器已统一处理错误提示
  }
}

const stockIn = (item: any) => {
  stockType.value = 'in'
  stockForm.materialId = item.id
  stockForm.materialName = item.name
  stockForm.currentStock = item.stockQuantity
  stockForm.unit = item.unit
  stockForm.quantity = 1
  stockForm.remark = ''
  stockDialogVisible.value = true
}

const stockOut = (item: any) => {
  stockType.value = 'out'
  stockForm.materialId = item.id
  stockForm.materialName = item.name
  stockForm.currentStock = item.stockQuantity
  stockForm.unit = item.unit
  stockForm.quantity = 1
  stockForm.remark = ''
  stockDialogVisible.value = true
}

const submitStock = async () => {
  try {
    await request.post(`/material/${stockType.value === 'in' ? 'stock-in' : 'stock-out'}`, {
      materialId: stockForm.materialId,
      quantity: stockForm.quantity,
      remark: stockForm.remark
    })
    ElMessage.success(stockType.value === 'in' ? '入库成功' : '出库成功')
    stockDialogVisible.value = false
    loadData()
  } catch (e) {
    // 响应拦截器已统一处理错误提示
  }
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>

<style scoped lang="scss">
.material-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.material-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  }
}

.material-card-header {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
}

.warning-bg {
  background: linear-gradient(135deg, #fef0f0, #fee);
}

.normal-bg {
  background: linear-gradient(135deg, #f0f9eb, #e8f5e9);
}

.material-card-body {
  padding: 15px;
}

.material-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #303133;
}

.material-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.material-info-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 8px;
}

.info-label {
  color: #606266;
}

.info-value {
  font-weight: 600;
}

.warning-text {
  color: #f56c6c;
}

.material-price-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-top: 1px solid #f5f7fa;
  border-bottom: 1px solid #f5f7fa;
  margin: 10px 0;
}

.price-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.price-label {
  font-size: 12px;
  color: #909399;
}

.price-value {
  font-size: 14px;
  font-weight: 600;
}

.price-value.cost {
  color: #e6a23c;
}

.price-value.factory {
  color: #67c23a;
}

.material-value {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 12px;
  color: #409eff;
  font-weight: 600;
}

.material-actions {
  display: flex;
  gap: 8px;

  .el-button {
    flex: 1;
  }
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px;
  text-align: center;
}
</style>
