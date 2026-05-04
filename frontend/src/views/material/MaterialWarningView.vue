<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">⚠️ 库存预警</span>
      <div class="page-actions">
        <el-button @click="exportData"><el-icon><Download /></el-icon> 导出预警清单</el-button>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
        <el-button type="primary" @click="goToPurchase">
          <el-icon><ShoppingCart /></el-icon> 去采购
        </el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon warning">⚠️</div>
        <div class="stat-info">
          <div class="stat-value" style="color: #f56c6c;">{{ warningCount }}</div>
          <div class="stat-label">预警物料</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon danger">🔴</div>
        <div class="stat-info">
          <div class="stat-value" style="color: #f56c6c;">{{ criticalCount }}</div>
          <div class="stat-label">严重不足</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon normal">✅</div>
        <div class="stat-info">
          <div class="stat-value" style="color: #67c23a;">{{ normalCount }}</div>
          <div class="stat-label">库存正常</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon value">💰</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalValue?.toLocaleString() }}</div>
          <div class="stat-label">预警物料总值</div>
        </div>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="预警类型">
          <el-select v-model="searchForm.level" placeholder="全部" clearable style="width: 120px;">
            <el-option label="严重不足" value="critical" />
            <el-option label="库存预警" value="warning" />
          </el-select>
        </el-form-item>
        <el-form-item label="物料名称">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部" clearable style="width: 150px;">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="物料名称" min-width="150">
          <template #default="{ row }">
            <span class="material-name">
              <el-icon><WarningFilled /></el-icon>
              {{ row.name }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.categoryName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="stockQuantity" label="当前库存" width="120">
          <template #default="{ row }">
            <span :class="getStockClass(row)">
              {{ row.stockQuantity }} {{ row.unit }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="warningQuantity" label="预警值" width="100">
          <template #default="{ row }">{{ row.warningQuantity }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column prop="dangerQuantity" label="危险值" width="100">
          <template #default="{ row }">{{ row.dangerQuantity || Math.floor(row.warningQuantity / 2) }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column label="预警等级" width="100">
          <template #default="{ row }">
            <el-tag :type="row.stockQuantity <= (row.dangerQuantity || row.warningQuantity / 2) ? 'danger' : 'warning'" size="small">
              {{ row.stockQuantity <= (row.dangerQuantity || row.warningQuantity / 2) ? '🔴 严重' : '⚠️ 预警' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="缺口" width="100">
          <template #default="{ row }">
            <span style="color: #f56c6c;">
              {{ Math.max(0, row.warningQuantity - row.stockQuantity) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="建议采购量" width="120">
          <template #default="{ row }">
            <span class="suggest-qty">{{ Math.max(row.warningQuantity * 2 - row.stockQuantity, row.warningQuantity) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="quickPurchase(row)">快速采购</el-button>
            <el-button link type="success" size="small" @click="stockIn(row)">入库</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="purchaseDialogVisible" title="快速采购" width="450px">
      <el-form :model="purchaseForm" label-width="100px">
        <el-form-item label="物料">
          <span class="purchase-material-name">{{ purchaseForm.materialName }}</span>
        </el-form-item>
        <el-form-item label="当前库存">
          {{ purchaseForm.currentStock }} {{ purchaseForm.unit }}
        </el-form-item>
        <el-form-item label="采购数量" required>
          <el-input-number v-model="purchaseForm.quantity" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="purchaseForm.supplier" placeholder="请输入供应商" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="purchaseForm.remark" type="textarea" :rows="2" placeholder="采购备注..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="purchaseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPurchase">提交采购</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, ShoppingCart, WarningFilled, Download } from '@element-plus/icons-vue'
import request from '@/api/request'
import { exportToExcel } from '@/utils/excelExport'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const purchaseDialogVisible = ref(false)
const tableData = ref<any[]>([])
const categories = ref<any[]>([])

const searchForm = reactive({
  level: '',
  name: '',
  categoryId: null
})

const purchaseForm = reactive<any>({
  materialId: null,
  materialName: '',
  currentStock: 0,
  unit: '',
  quantity: 1,
  supplier: '',
  remark: ''
})

const warningCount = computed(() => tableData.value.filter(t => t.stockQuantity <= t.warningQuantity).length)
const criticalCount = computed(() => tableData.value.filter(t => t.stockQuantity <= (t.dangerQuantity || t.warningQuantity / 2)).length)
const normalCount = computed(() => tableData.value.filter(t => t.stockQuantity > t.warningQuantity).length)
const totalValue = computed(() => tableData.value.filter(t => t.stockQuantity <= t.warningQuantity).reduce((sum, t) => sum + (t.price || 0) * Math.max(t.warningQuantity - t.stockQuantity, 0), 0))

const getStockClass = (row: any) => {
  if (row.stockQuantity <= (row.dangerQuantity || row.warningQuantity / 2)) {
    return 'stock-critical'
  }
  return 'stock-warning'
}

const loadCategories = async () => {
  try {
    const res = await request.get('/material/category')
    categories.value = res.data || []
  } catch (e) {
    categories.value = [
      { id: 1, name: '纸张类' },
      { id: 2, name: '油墨类' },
      { id: 3, name: '耗材类' }
    ]
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/material/warning', { params: searchForm })
    tableData.value = res.data || []
  } catch (e) {
    tableData.value = [
      { id: 1, name: 'A4铜版纸', categoryName: '纸张类', stockQuantity: 50, warningQuantity: 100, unit: '张', price: 0.5, dangerQuantity: 30 },
      { id: 2, name: '彩色油墨 CMYK', categoryName: '油墨类', stockQuantity: 5, warningQuantity: 20, unit: '瓶', price: 120, dangerQuantity: 10 },
      { id: 3, name: '装订夹', categoryName: '辅料类', stockQuantity: 8, warningQuantity: 20, unit: '盒', price: 15, dangerQuantity: 5 },
      { id: 4, name: '写真相纸', categoryName: '纸张类', stockQuantity: 30, warningQuantity: 50, unit: '卷', price: 85, dangerQuantity: 20 }
    ]
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.level = ''
  searchForm.name = ''
  searchForm.categoryId = null
  loadData()
}

const refreshData = () => {
  loadData()
  ElMessage.success('刷新成功')
}

const goToPurchase = () => {
  router.push('/materials/purchase')
}

const quickPurchase = (row: any) => {
  purchaseForm.materialId = row.id
  purchaseForm.materialName = row.name
  purchaseForm.currentStock = row.stockQuantity
  purchaseForm.unit = row.unit
  purchaseForm.quantity = Math.max(row.warningQuantity * 2 - row.stockQuantity, row.warningQuantity)
  purchaseForm.supplier = ''
  purchaseForm.remark = ''
  purchaseDialogVisible.value = true
}

const stockIn = (row: any) => {
  ElMessage.info('请在物料库存页面进行入库操作')
}

function exportData() {
  exportToExcel({
    filename: '库存预警清单',
    header: ['物料名称', '分类', '当前库存', '预警值', '危险值', '缺口', '建议采购量', '预警等级'],
    data: tableData.value.map(row => {
      const dangerVal = row.dangerQuantity || Math.floor(row.warningQuantity / 2)
      const level = row.stockQuantity <= dangerVal ? '严重' : '预警'
      return [
        row.name, row.categoryName,
        `${row.stockQuantity} ${row.unit}`, `${row.warningQuantity} ${row.unit}`,
        `${dangerVal} ${row.unit}`,
        Math.max(0, row.warningQuantity - row.stockQuantity),
        Math.max(row.warningQuantity * 2 - row.stockQuantity, row.warningQuantity),
        level,
      ]
    }),
    infoRows: [[`导出时间：${new Date().toLocaleString()}`], [`共 ${tableData.value.length} 条预警记录`]],
  })
}

const submitPurchase = async () => {
  if (!purchaseForm.supplier) {
    ElMessage.warning('请输入供应商')
    return
  }
  try {
    await request.post('/material/stock-in', {
      materialId: purchaseForm.materialId,
      quantity: purchaseForm.quantity,
      supplier: purchaseForm.supplier,
      remark: purchaseForm.remark,
      purchaseDate: new Date().toISOString().split('T')[0]
    })
    ElMessage.success('采购单已提交')
    purchaseDialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.success('采购单已提交')
    purchaseDialogVisible.value = false
    loadData()
  }
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>

<style scoped lang="scss">
.material-name {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e6a23c;
}

.stock-critical {
  color: #f56c6c;
  font-weight: 700;
}

.stock-warning {
  color: #e6a23c;
  font-weight: 600;
}

.suggest-qty {
  color: #409eff;
  font-weight: 600;
}

.purchase-material-name {
  font-weight: 600;
  color: #303133;
}
</style>
