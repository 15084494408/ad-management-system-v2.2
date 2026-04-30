<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">⚡ 快速记账</span>
      <el-button type="primary" @click="openAdd">+ 新增记账</el-button>
    </div>
    <div class="card" style="padding:0;overflow:hidden;">
      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{row}">
            <el-tag :type="row.type==='income'?'success':'danger'" size="small">
              {{row.type==='income'?'收入':'支出'}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="relatedName" label="公司/客户" min-width="140"/>
        <el-table-column prop="category" label="分类" width="100"/>
        <el-table-column prop="amount" label="金额" width="130">
          <template #default="{row}">
            <span :style="{color:row.type==='income'?'#67c23a':'#f56c6c',fontWeight:'700'}">
              {{row.type==='income'?'+':'-'}}¥{{Number(row.amount).toLocaleString()}}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="物料明细" width="80">
          <template #default="{row}">
            <el-tag v-if="row._itemCount > 0" size="small" type="warning">{{ row._itemCount }}项</el-tag>
            <span v-else style="color:#c0c4cc;">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="支付方式" width="100">
          <template #default="{row}">
            {{paymentMethodMap[row.paymentMethod] || row.paymentMethod}}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170"/>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{row}">
            <el-button type="primary" link size="small" @click="viewDetail(row)">明细</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增记账弹窗 -->
    <el-dialog v-model="showAdd" title="快速记账" width="720px" @closed="resetForm" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px" :rules="rules" ref="formRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型" prop="type">
              <el-radio-group v-model="form.type">
                <el-radio value="income">收入</el-radio>
                <el-radio value="expense">支出</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category" style="width:100%" placeholder="选择分类">
                <el-option-group label="收入">
                  <el-option label="订单收入" value="订单收入"/>
                  <el-option label="充值收入" value="充值收入"/>
                </el-option-group>
                <el-option-group label="支出">
                  <el-option label="采购支出" value="采购支出"/>
                  <el-option label="工资" value="工资"/>
                  <el-option label="房租" value="房租"/>
                  <el-option label="广告费" value="广告费"/>
                  <el-option label="印刷费" value="印刷费"/>
                  <el-option label="设计费" value="设计费"/>
                  <el-option label="材料费" value="材料费"/>
                  <el-option label="退款" value="退款"/>
                  <el-option label="其他" value="其他"/>
                </el-option-group>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="公司/客户" prop="relatedName">
              <el-select
                v-model="form.relatedName"
                filterable
                allow-create
                default-first-option
                placeholder="选择或输入"
                style="width:100%"
              >
                <el-option
                  v-for="c in companies"
                  :key="c.id || c"
                  :label="c.name || c"
                  :value="c.name || c"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支付方式" prop="paymentMethod">
              <el-select v-model="form.paymentMethod" style="width:100%" placeholder="选择支付方式">
                <el-option label="现金" value="cash"/>
                <el-option label="微信" value="wechat"/>
                <el-option label="支付宝" value="alipay"/>
                <el-option label="银行转账" value="transfer"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选"/>
        </el-form-item>

        <!-- 物料明细区域 -->
        <el-divider content-position="left">
          <span style="font-size:14px;">物料明细</span>
          <span style="color:#909399;font-size:12px;margin-left:8px;">（添加物料将自动扣减库存）</span>
        </el-divider>

        <!-- 物料分类筛选器 -->
        <div style="margin-bottom:12px;display:flex;gap:8px;flex-wrap:wrap;">
          <el-radio-group v-model="selectedMaterialCategory" size="small" @change="onCategoryChange">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button v-for="cat in materialCategories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </el-radio-button>
          </el-radio-group>
        </div>

        <div class="material-items">
          <div v-for="(item, index) in form.items" :key="index" class="material-item-row">
            <el-row :gutter="8" align="middle">
              <el-col :span="7">
                <el-select
                  v-model="item.materialId"
                  filterable
                  placeholder="选择物料"
                  style="width:100%"
                  @change="(val: any) => onMaterialSelect(item, val)"
                  value-key="id"
                >
                  <el-option-group
                    v-for="group in filteredMaterialGroups"
                    :key="group.label"
                    :label="group.label"
                  >
                    <el-option
                      v-for="m in group.items"
                      :key="m.id"
                      :label="m.name"
                      :value="m.id"
                    >
                      <div style="display:flex;justify-content:space-between;align-items:center;">
                        <span>{{ m.name }}</span>
                        <span style="color:#909399;font-size:12px;">
                          ¥{{ m.price?.toFixed(2) }}{{ m.pricingType === 1 ? '/㎡' : '' }}
                          <el-tag size="small" :type="getStockType(m)" style="margin-left:4px;">
                            库存:{{ m.stockQuantity }}
                            <span v-if="m.paperGroup" style="font-size:10px;">(共享)</span>
                          </el-tag>
                        </span>
                      </div>
                    </el-option>
                  </el-option-group>
                </el-select>
              </el-col>

              <!-- 按数量模式 -->
              <el-col :span="4" v-if="item.pricingType !== 1">
                <el-input-number v-model="item.quantity" :min="1" :precision="0" placeholder="数量" controls-position="right" style="width:100%;" @change="calcItemTotal(item)"/>
              </el-col>

              <!-- 按面积模式 -->
              <template v-else>
                <el-col :span="3">
                  <el-input-number v-model="item.width" :min="0.01" :precision="2" placeholder="宽" controls-position="right" style="width:100%;" @change="calcItemTotal(item)"/>
                </el-col>
                <el-col :span="1" style="text-align:center;line-height:32px;color:#909399;">×</el-col>
                <el-col :span="3">
                  <el-input-number v-model="item.height" :min="0.01" :precision="2" placeholder="高" controls-position="right" style="width:100%;" @change="calcItemTotal(item)"/>
                </el-col>
                <el-col :span="1" style="text-align:center;line-height:32px;color:#909399;">㎡</el-col>
              </template>

              <el-col :span="4">
                <el-input v-model="item.totalPriceDisplay" placeholder="小计" readonly style="width:100%;">
                  <template #prefix>¥</template>
                </el-input>
              </el-col>
              <el-col :span="2">
                <el-button type="danger" link @click="removeItem(index)" :disabled="form.items.length <= 1">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-col>
            </el-row>
            <!-- 库存不足警告 -->
            <div v-if="item.stockWarning" class="stock-warning">
              <el-icon><WarningFilled /></el-icon>
              {{ item.stockWarning }}
            </div>
          </div>
        </div>

        <div style="display:flex;justify-content:space-between;align-items:center;margin-top:8px;">
          <el-button size="small" @click="addItem">+ 添加物料行</el-button>
          <div class="total-price">
            物料合计：<strong>¥{{ materialTotal }}</strong>
          </div>
        </div>

        <el-form-item label="记账金额" prop="amount" style="margin-top:16px;">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="100" style="width:100%;"/>
          <el-button v-if="materialTotal > 0" type="primary" link size="small" @click="form.amount = Math.ceil(Number(materialTotal))" style="margin-left:8px;">
            填入物料合计（向上取整）
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确认记账</el-button>
      </template>
    </el-dialog>

    <!-- 物料明细查看弹窗 -->
    <el-dialog v-model="showDetail" title="物料明细" width="600px">
      <el-table :data="detailItems" v-loading="detailLoading" size="small">
        <el-table-column prop="materialName" label="物料名称" min-width="120"/>
        <el-table-column prop="pricingType" label="计价方式" width="90">
          <template #default="{row}">
            {{ row.pricingType === 1 ? '按面积' : '按数量' }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80"/>
        <el-table-column label="面积" width="80">
          <template #default="{row}">
            {{ row.pricingType === 1 && row.area ? Number(row.area).toFixed(2) + '㎡' : '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="unitPrice" label="单价" width="90">
          <template #default="{row}">¥{{ Number(row.unitPrice).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="小计" width="90">
          <template #default="{row}">¥{{ Number(row.totalPrice).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, WarningFilled } from '@element-plus/icons-vue'
import { financeApi, customerApi, materialApi } from '@/api'
import { useFinanceStore } from '@/stores/finance'
const financeStore = useFinanceStore()

interface MaterialItem {
  materialId: number | null
  materialName: string
  pricingType: number
  quantity: number
  width: number
  height: number
  area: number
  unitPrice: number
  totalPrice: number
  totalPriceDisplay: string
  stockWarning: string
  _stockQuantity: number
}

const list = ref<any[]>([])
const loading = ref(false)
const showAdd = ref(false)
const showDetail = ref(false)
const detailLoading = ref(false)
const detailItems = ref<any[]>([])
const saving = ref(false)
const companies = ref<any[]>([])
const materialList = ref<any[]>([])
const materialCategories = ref<any[]>([])
const selectedMaterialCategory = ref<number | null>(null)
const formRef = ref<any>(null)

const paymentMethodMap: Record<string, string> = {
  cash: '现金', wechat: '微信', alipay: '支付宝', transfer: '银行转账'
}

/**
 * 按分类和纸张分组过滤物料列表
 * 纸张类物料（paperGroup非空）按分组展示，每组只显示一次库存
 */
const filteredMaterialGroups = computed(() => {
  const filtered = selectedMaterialCategory.value
    ? materialList.value.filter((m: any) => m.categoryId === selectedMaterialCategory.value)
    : materialList.value

  // 按 categoryId 分组
  const categoryMap = new Map<number | string, any[]>()
  const noCategory: any[] = []

  filtered.forEach((m: any) => {
    const catId = m.categoryId || 'other'
    if (!categoryMap.has(catId)) {
      categoryMap.set(catId, [])
    }
    categoryMap.get(catId)!.push(m)
  })

  // 构建分组列表
  const groups: { label: string; items: any[] }[] = []

  // 添加有分类的组
  materialCategories.value.forEach(cat => {
    const items = categoryMap.get(cat.id)
    if (items && items.length > 0) {
      groups.push({ label: cat.name, items })
    }
  })

  // 添加无分类的物料
  if (noCategory.length > 0) {
    groups.push({ label: '其他', items: noCategory })
  }

  // 处理纸张类物料：同 paperGroup 只显示一个并标注 (共享)
  groups.forEach(group => {
    const seenGroups = new Map<string, any>()
    const paperItems: any[] = []
    const otherItems: any[] = []

    group.items.forEach((m: any) => {
      if (m.paperGroup && m.paperGroup.trim()) {
        // 同 paperGroup 只保留第一个（带库存）
        if (!seenGroups.has(m.paperGroup)) {
          seenGroups.set(m.paperGroup, m)
          paperItems.push({ ...m, _isFirstInGroup: true })
        } else {
          // 同组后续项标注为共享库存
          paperItems.push({ ...m, _isFirstInGroup: false, stockQuantity: '—' })
        }
      } else {
        otherItems.push(m)
      }
    })

    // 纸张放前面，标注库存共享
    group.items = [...paperItems, ...otherItems]
  })

  return groups.length > 0 ? groups : [{ label: '全部物料', items: filtered }]
})

/**
 * 获取库存标签类型（预警颜色）
 */
function getStockType(m: any): string {
  if (!m.stockQuantity || m.stockQuantity === '—') return 'info'
  const qty = Number(m.stockQuantity) || 0
  if (qty <= 0) return 'danger'
  if (qty <= (m.warningQuantity || 10)) return 'warning'
  return 'success'
}

/**
 * 切换物料分类
 */
function onCategoryChange() {
  // 重置已选的物料
  form.items.forEach(item => {
    item.materialId = null
    item.materialName = ''
    item._stockQuantity = 0
  })
}

function createEmptyItem(): MaterialItem {
  return {
    materialId: null,
    materialName: '',
    pricingType: 0,
    quantity: 1,
    width: 0,
    height: 0,
    area: 0,
    unitPrice: 0,
    totalPrice: 0,
    totalPriceDisplay: '',
    stockWarning: '',
    _stockQuantity: 0
  }
}

const form = reactive({
  type: 'income' as string,
  relatedName: '' as string,
  category: '订单收入' as string,
  amount: 0 as number,
  paymentMethod: 'transfer' as string,
  remark: '' as string,
  items: [createEmptyItem()] as MaterialItem[]
})

const rules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  relatedName: [{ required: true, message: '请选择或输入公司/客户名', trigger: 'change' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  paymentMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

const materialTotal = computed(() => {
  const sum = form.items.reduce((acc, item) => acc + (item.totalPrice || 0), 0)
  return sum > 0 ? Math.ceil(sum).toFixed(2) : '0.00'
})

function onMaterialSelect(item: MaterialItem, materialId: number) {
  const m = materialList.value.find((mat: any) => mat.id === materialId)
  if (!m) return

  item.materialName = m.name
  item.pricingType = m.pricingType || 0
  item.unitPrice = m.price || 0
  item._stockQuantity = m.stockQuantity || 0

  // 重置输入
  item.quantity = 1
  item.width = 0
  item.height = 0
  item.area = 0
  item.stockWarning = ''
  item.totalPrice = 0
  item.totalPriceDisplay = ''

  calcItemTotal(item)
}

function calcItemTotal(item: MaterialItem) {
  if (item.pricingType === 1) {
    // 按面积：宽 × 高 × 单价，向上取整
    if (item.width > 0 && item.height > 0) {
      item.area = Math.round(item.width * item.height * 100) / 100
      const raw = item.area * item.unitPrice
      item.totalPrice = Math.ceil(raw)
      // 库存不足检查（按面积：area 向上取整作为出库数量）
      const needed = Math.ceil(item.area)
      if (needed > item._stockQuantity) {
        item.stockWarning = `库存不足！需要 ${needed}，当前 ${item._stockQuantity}`
      } else {
        item.stockWarning = ''
      }
    } else {
      item.totalPrice = 0
      item.stockWarning = ''
    }
  } else {
    // 按数量：数量 × 单价，向上取整
    if (item.quantity > 0 && item.unitPrice > 0) {
      const raw = item.quantity * item.unitPrice
      item.totalPrice = Math.ceil(raw)
      if (item.quantity > item._stockQuantity) {
        item.stockWarning = `库存不足！需要 ${item.quantity}，当前 ${item._stockQuantity}`
      } else {
        item.stockWarning = ''
      }
    } else {
      item.totalPrice = 0
      item.stockWarning = ''
    }
  }
  item.totalPriceDisplay = item.totalPrice > 0 ? item.totalPrice.toFixed(2) : ''
}

function addItem() {
  form.items.push(createEmptyItem())
}

function removeItem(index: number) {
  if (form.items.length > 1) {
    form.items.splice(index, 1)
  }
}

function resetForm() {
  form.type = 'income'
  form.relatedName = ''
  form.category = '订单收入'
  form.amount = 0
  form.paymentMethod = 'transfer'
  form.remark = ''
  form.items = [createEmptyItem()]
}

function openAdd() {
  resetForm()
  showAdd.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()

  if (!form.amount || form.amount <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }

  // 检查是否有库存不足的物料（仅警告，不阻止）
  const warnings = form.items.filter(
    item => item.materialId && item.stockWarning
  )
  if (warnings.length > 0) {
    try {
      await ElMessageBox.confirm(
        `有 ${warnings.length} 项物料库存不足，是否继续提交？`,
        '库存不足警告',
        { type: 'warning', confirmButtonText: '继续提交', cancelButtonText: '取消' }
      )
    } catch {
      return // 用户取消
    }
  }

  saving.value = true
  try {
    // 构建提交数据
    const items = form.items
      .filter(item => item.materialId && item.totalPrice > 0)
      .map(item => ({
        materialId: item.materialId,
        materialName: item.materialName,
        pricingType: item.pricingType,
        quantity: item.pricingType === 1 ? null : item.quantity,
        width: item.pricingType === 1 ? item.width : null,
        height: item.pricingType === 1 ? item.height : null,
        area: item.pricingType === 1 ? item.area : null,
        unitPrice: item.unitPrice,
        totalPrice: item.totalPrice
      }))

    await financeApi.createQuickRecord({
      type: form.type,
      category: form.category,
      amount: form.amount,
      relatedName: form.relatedName,
      paymentMethod: form.paymentMethod,
      remark: form.remark,
      items: items.length > 0 ? items : undefined
    })

    ElMessage.success('记账成功' + (items.length > 0 ? `，已扣减 ${items.length} 项物料库存` : ''))
    showAdd.value = false
    financeStore.triggerRefresh()
    load()
  } catch (e: any) {
    ElMessage.error(e?.message || '记账失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确定删除此记录？删除后关联的物料库存不会回退。', '提示', { type: 'warning' })
    await financeApi.deleteRecord(row.id)
    ElMessage.success('已删除')
    financeStore.triggerRefresh()
    load()
  } catch {}
}

async function viewDetail(row: any) {
  detailItems.value = []
  showDetail.value = true
  detailLoading.value = true
  try {
    const r = await financeApi.getRecordItems(row.id)
    detailItems.value = r.data || []
  } catch {
    detailItems.value = []
  } finally {
    detailLoading.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const r = await financeApi.getQuickRecords()
    const records = r.data || []
    // 尝试为每条记录加载物料明细数量
    for (const record of records) {
      try {
        const detail = await financeApi.getRecordItems(record.id)
        record._itemCount = (detail.data || []).length
      } catch {
        record._itemCount = 0
      }
    }
    list.value = records
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function loadCompanies() {
  try {
    const r = await customerApi.getList({ current: 1, size: 200 })
    companies.value = r.data?.records || []
  } catch {
    companies.value = []
  }
}

async function loadMaterials() {
  try {
    // 并行加载物料列表和分类
    const [materialsRes, categoriesRes] = await Promise.all([
      materialApi.listAll(),
      materialApi.getCategories()
    ])
    materialList.value = materialsRes.data || []
    materialCategories.value = categoriesRes.data || []
  } catch {
    materialList.value = []
    materialCategories.value = []
  }
}

onMounted(() => {
  load()
  loadCompanies()
  loadMaterials()
})
</script>

<style scoped lang="scss">
.material-items {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
}

.material-item-row {
  margin-bottom: 8px;
  padding: 8px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #ebeef5;

  &:last-child {
    margin-bottom: 0;
  }
}

.stock-warning {
  color: #e6a23c;
  font-size: 12px;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.total-price {
  font-size: 14px;
  color: #303133;

  strong {
    color: #409eff;
    font-size: 18px;
  }
}
</style>
