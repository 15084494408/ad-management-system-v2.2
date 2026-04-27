<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item" @click="router.push('/orders')">订单管理</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">创建订单</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">📝 创建新订单</h1>
      <div class="page-actions">
        <button class="btn btn-default" @click="router.back()">← 返回列表</button>
        <button class="btn btn-primary" :disabled="submitting || selectedMaterials.length === 0"
          @click="submitOrder">✅ 提交订单</button>
      </div>
    </div>

    <!-- ========== 上半部分：订单基本信息 ========== -->
    <div class="card order-info-card">
      <div class="card-title-bar">
        <span>📋 订单信息</span>
        <span style="color:#909399;font-size:12px;">必填项请填写完整</span>
      </div>

      <!-- 第一行：标题 + 类型 -->
      <div class="form-row">
        <div class="form-group" style="flex:2;">
          <label><span class="required">*</span> 订单标题</label>
          <input type="text" v-model="form.title" class="form-control"
            placeholder="如：某某公司宣传册印刷订单" maxlength="200" />
        </div>
        <div class="form-group" style="flex:1;">
          <label><span class="required">*</span> 订单类型</label>
          <select v-model="form.orderType" class="form-control">
            <option value="">请选择</option>
            <option value="1">🖨️ 印刷</option>
            <option value="2">📢 广告</option>
            <option value="3">✏️ 设计</option>
          </select>
        </div>
      </div>

      <!-- 第二行：客户 + 设计师 -->
      <div class="form-row">
        <div class="form-group" style="flex:2;">
          <label><span class="required">*</span> 客户</label>
          <select v-model="form.customerId" class="form-control" @change="onCustomerChange">
            <option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name || c.customerName }}</option>
          </select>
        </div>
        <div class="form-group" style="flex:1;">
          <label>设计师（关联抽成）</label>
          <select v-model="form.designerId" class="form-control" @change="onDesignerChange" style="cursor:pointer;">
            <option :value="null" disabled>选择设计师...</option>
            <option v-for="u in designerList" :key="u.id" :value="u.id">{{ u.realName || u.username }}</option>
          </select>
        </div>
      </div>

      <!-- 第三行：联系人 + 电话 -->
      <div class="form-row">
        <div class="form-group" style="flex:1;">
          <label>联系人</label>
          <input type="text" v-model="form.contactPerson" class="form-control"
            placeholder="联系人姓名" />
        </div>
        <div class="form-group" style="flex:1;">
          <label>联系电话</label>
          <input type="text" v-model="form.contactPhone" class="form-control"
            placeholder="手机或座机" />
        </div>
      </div>

      <!-- 第四行：交付日期 + 优先级 -->
      <div class="form-row">
        <div class="form-group" style="flex:1;">
          <label>交付日期</label>
          <input type="date" v-model="form.deliveryDate" class="form-control" />
        </div>
        <div class="form-group" style="flex:1;">
          <label>优先级</label>
          <select v-model="form.priority" class="form-control">
            <option :value="1">⬜ 普通</option>
            <option :value="2">🟡 紧急</option>
            <option :value="3">🔴 加急</option>
          </select>
        </div>
      </div>

      <!-- 第五行：交付地址 -->
      <div class="form-row">
        <div class="form-group" style="flex:1;">
          <label>交付地址</label>
          <input type="text" v-model="form.deliveryAddress" class="form-control"
            placeholder="详细地址" />
        </div>
      </div>

      <!-- 第六行：备注 -->
      <div class="form-row">
        <div class="form-group" style="flex:1;">
          <label>备注说明</label>
          <textarea v-model="form.remark" class="form-control" rows="2"
            placeholder="特殊要求、注意事项等"></textarea>
        </div>
      </div>

      <!-- 提交人显示 -->
      <div class="creator-info">
        <span>👤 提交人：<strong>{{ authStore.userInfo?.username || '未知' }}</strong></span>
        <span v-if="authStore.userInfo?.roles?.length" style="margin-left:16px;">
          🎭 角色：<strong>{{ authStore.userInfo.roles.join(' / ') }}</strong>
        </span>
      </div>
    </div>

    <!-- ========== 下半部分：物料选择区域 ========== -->
    <div class="card materials-card">
      <div class="card-title-bar">
        <span>📦 选择物料</span>
        <div style="display:flex;align-items:center;gap:10px;">
          <input type="text" v-model="materialKeyword" class="form-control"
            placeholder="搜索物料名称..." style="width:180px;padding:4px 8px;"
            @input="filterMaterials" />
          <select v-model="materialCategoryFilter" class="form-control"
            style="width:130px;padding:4px 8px;" @change="filterMaterials">
            <option value="">全部分类</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
          <span style="color:#909399;font-size:12px;">
            已选 {{ selectedMaterials.length }} 件 | 总计 ¥{{ formatMoney(materialTotal) }}
          </span>
        </div>
      </div>

      <!-- 物料表格（两列并排） -->
      <div class="materials-two-col" v-if="!materialsLoading">
        <template v-if="filteredMaterialList.length > 0">
          <div class="materials-col" v-for="(col, colIdx) in materialColumns" :key="colIdx">
            <table class="data-table" style="margin:0;">
              <thead>
                <tr>
                  <th style="width:30px;text-align:center;">#</th>
                  <th style="width:32%;">物料名称</th>
                  <th style="width:14%;">分类</th>
                  <th style="width:60px;">规格</th>
                  <th style="width:40px;">单位</th>
                  <th style="width:60px;text-align:right;">零售价</th>
                  <th style="width:50px;text-align:center;">数量</th>
                  <th style="width:65px;text-align:right;">单价</th>
                  <th style="width:65px;text-align:right;">小计</th>
                  <th style="width:40px;text-align:center;">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(m, idx) in col" :key="m.id"
                  :class="{ 'row-selected': isMaterialSelected(m.id) }">
                  <td style="text-align:center;color:#c0c4cc;">{{ colIdx * Math.ceil(filteredMaterialList.length / 2) + idx + 1 }}</td>
                  <td>
                    <strong>{{ m.name }}</strong>
                    <div v-if="m.code" style="font-size:11px;color:#909399;margin-top:2px;">编码：{{ m.code }}</div>
                  </td>
                  <td><span class="tag" style="font-size:11px;">{{ m.categoryName }}</span></td>
                  <td style="font-size:12px;">{{ m.spec || '-' }}</td>
                  <td style="text-align:center;font-size:12px;">{{ m.unit || '个' }}</td>
                  <td style="text-align:right;color:#e6a23c;font-size:12px;">¥{{ formatMoney(m.price) }}</td>
                  <td style="text-align:center;">
                    <input v-if="isMaterialSelected(m.id)" type="number"
                      v-model.number="getSelected(m.id).quantity"
                      class="qty-input"
                      min="1" step="1" @change="recalcLine(m.id)"
                      style="width:45px;text-align:center;padding:2px 3px;font-size:12px;" />
                    <span v-else style="color:#c0c4cc;">-</span>
                  </td>
                  <td style="text-align:right;">
                    <input v-if="isMaterialSelected(m.id)" type="number"
                      v-model.number="getSelected(m.id).unitPrice"
                      class="price-input"
                      min="0" step="0.01" @change="recalcLine(m.id)"
                      style="width:55px;text-align:right;padding:2px 3px;font-size:12px;" />
                    <span v-else style="color:#c0c4cc;">-</span>
                  </td>
                  <td style="text-align:right;font-weight:600;color:#e6a23c;font-size:12px;">
                    <span v-if="isMaterialSelected(m.id)">¥{{ formatMoney(getSelected(m.id).amount) }}</span>
                    <span v-else>-</span>
                  </td>
                  <td style="text-align:center;">
                    <button v-if="isMaterialSelected(m.id)"
                      class="action-btn delete-sm" title="移除"
                      @click="removeMaterial(m.id)">✕</button>
                    <button v-else class="btn-sm btn-primary-outline" title="选用此物料"
                      @click="addMaterial(m)">+</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
        <div v-else class="materials-empty">暂无匹配的物料</div>
      </div>

      <!-- 加载中 -->
      <div v-else class="loading-placeholder">
        <p>正在加载物料数据...</p>
      </div>
    </div>

    <!-- ========== 底部汇总栏 ========== -->
    <div class="bottom-summary" v-if="selectedMaterials.length > 0">
      <div class="summary-content">
        <div class="summary-item">
          <span>已选物料：</span>
          <strong>{{ selectedMaterials.length }} 件</strong>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-item">
          <span>物料合计：</span>
          <strong style="color:#e6a23c;font-size:18px;">¥{{ formatMoney(materialTotal) }}</strong>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-item">
          <label style="margin:0;display:flex;align-items:center;gap:4px;">
            优惠金额：
            <input type="number" v-model.number="form.discountAmount"
              class="price-input" min="0" step="0.01" style="width:85px;" />
          </label>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-item summary-total">
          <span>订单总额：</span>
          <strong>¥{{ formatMoney(orderTotalAmount) }}</strong>
        </div>
        <button class="btn btn-primary submit-btn" :disabled="submitting" @click="submitOrder">
          {{ submitting ? '提交中...' : '✅ 提交创建' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { orderApi } from '@/api/modules/order'
import { materialApi } from '@/api/modules/material'
import { customerApi } from '@/api/modules/customer'

const router = useRouter()
const authStore = useAuthStore()

// ========== 状态 ==========
const submitting = ref(false)
const materialsLoading = ref(true)

// ========== 表单 ==========
const form = reactive({
  title: '',
  orderType: '',
  customerId: null as number | null,
  customerName: '',
  designerName: '',                          // 初始化时自动填入当前登录用户
  designerId: null as number | null,         // 设计师用户ID
  contactPerson: '',
  contactPhone: '',
  deliveryAddress: '',
  deliveryDate: '',
  priority: 1,
  discountAmount: 0,
  remark: '',
})

// ========== 客户下拉 ==========
const customers = ref<any[]>([])
const designerList = ref<any[]>([])

async function loadCustomers() {
  try {
    const res: any = await customerApi.getList({ current: 1, size: 500 })
    const list = res.data?.records || res.data || []
    customers.value = list
  } catch (e: any) {
    ElMessage.error('加载客户列表失败：' + (e?.message || '请检查网络'))
  }
}

function onCustomerChange() {
  const c = customers.value.find((cc: any) => cc.id === form.customerId)
  if (c) {
    form.customerName = c.name || c.customerName
    // 自动填充联系人（后端返回字段可能是 contactPerson 或 contact_person）
    form.contactPerson = c.contactPerson || c.contact_person || ''
    form.contactPhone = c.phone || ''
  }
}

// ========== 设计师下拉 ==========
async function loadDesigners() {
  try {
    const res: any = await request.get('/system/users', { params: { current: 1, size: 200 } })
    designerList.value = (res.data?.records || res.data || []).map((u: any) => ({
      id: u.id,
      realName: u.realName || u.real_name,
      username: u.username,
    }))
  } catch {
    designerList.value = []
  }
}

function onDesignerChange() {
  const u = designerList.value.find((d: any) => d.id === form.designerId)
  if (u) {
    form.designerName = u.realName || u.username
  } else {
    form.designerName = ''
  }
}

// ========== 物料 ==========
const allMaterials = ref<any[]>([])
const filteredMaterialList = ref<any[]>([])
const materialKeyword = ref('')
const materialCategoryFilter = ref('')
const categories = ref<any[]>([])

// 已选物料列表（每项包含 materialId, materialName, spec, unit, quantity, unitPrice, amount）
const selectedMaterials = ref<any[]>([])

async function loadMaterialsAndCategories() {
  materialsLoading.value = true
  try {
    const [matRes, catRes] = await Promise.all([
      materialApi.listAll(),
      materialApi.getCategories(),
    ])
    allMaterials.value = matRes.data || matRes || []
    categories.value = catRes.data || catRes || []
    filterMaterials()
  } catch (e: any) {
    ElMessage.error('加载物料数据失败：' + (e?.message || '请检查网络'))
  } finally {
    materialsLoading.value = false
  }
}

function filterMaterials() {
  let list = allMaterials.value

  // 关键词过滤
  if (materialKeyword.value.trim()) {
    const kw = materialKeyword.value.toLowerCase()
    list = list.filter((m: any) => (m.name || '').toLowerCase().includes(kw)
      || (m.code || '').toLowerCase().includes(kw))
  }

  // 分类过滤
  if (materialCategoryFilter.value) {
    list = list.filter((m: any) => Number(m.categoryId) === Number(materialCategoryFilter.value))
  }

  filteredMaterialList.value = list
}

function isMaterialSelected(id: number): boolean {
  return selectedMaterials.value.some(s => s.materialId === id)
}

// 两列布局：将物料列表一分为二
const materialColumns = computed(() => {
  const list = filteredMaterialList.value
  const mid = Math.ceil(list.length / 2)
  return [list.slice(0, mid), list.slice(mid)]
})

function getSelected(id: number): any {
  return selectedMaterials.value.find(s => s.materialId === id)
}

function addMaterial(mat: any) {
  if (isMaterialSelected(mat.id)) return
  selectedMaterials.value.push({
    materialId: mat.id,
    materialName: mat.name,
    spec: mat.spec || '',
    unit: mat.unit || '个',
    quantity: 1,
    unitPrice: mat.price ? Number(mat.price) : 0,
    amount: mat.price ? Number(mat.price) : 0,
  })
}

function removeMaterial(id: number) {
  selectedMaterials.value = selectedMaterials.value.filter(s => s.materialId !== id)
}

function recalcLine(id: number) {
  const item = getSelected(id)
  if (!item) return
  const qty = Math.max(1, Number(item.quantity) || 0)
  const price = Math.max(0, Number(item.unitPrice) || 0)
  item.amount = Number((qty * price).toFixed(2))
}

// ========== 汇总计算 ==========
const materialTotal = computed(() => {
  return selectedMaterials.value.reduce((sum, s) => sum + (s.amount || 0), 0)
})

const orderTotalAmount = computed(() => {
  const total = materialTotal.value - (Number(form.discountAmount) || 0)
  return Math.max(0, total)
})

// ========== 格式化 ==========
function formatMoney(val: any): string {
  const n = Number(val) || 0
  return n.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

// ========== 提交 ==========
async function submitOrder() {
  // 基本验证
  if (!form.title.trim()) { ElMessage.warning('请输入订单标题'); return }
  if (!form.orderType) { ElMessage.warning('请选择订单类型'); return }
  if (!form.customerId) { ElMessage.warning('请选择客户'); return }
  if (selectedMaterials.value.length === 0) { ElMessage.warning('请至少选择一件物料'); return }

  await ElMessageBox.confirm(
    `即将创建订单「${form.title}」\n` +
    `客户：${form.customerName}\n` +
    `已选 ${selectedMaterials.value.length} 件物料，总金额 ¥${formatMoney(orderTotalAmount)}\n\n` +
    `确认提交？`,
    '确认创建订单', { confirmButtonText: '确定提交', cancelButtonText: '取消' }
  )

  submitting.value = true
  try {
    const payload = {
      ...form,
      customerId: form.customerId,
      customerName: form.customerName,
      totalAmount: orderTotalAmount.value,
      creatorId: authStore.userInfo?.id,
      source: 1,
      materials: selectedMaterials.value.map(s => ({
        materialName: s.materialName,
        spec: s.spec,
        unit: s.unit,
        quantity: s.quantity,
        unitPrice: s.unitPrice,
        amount: s.amount,
      })),
    }
    const res: any = await orderApi.create(payload)
    ElMessage.success(`✅ 订单创建成功！编号将自动生成`)
    router.push({ name: 'Orders' })
  } catch (e: any) {
    ElMessage.error(e?.message || '创建失败，请检查输入后重试')
  } finally {
    submitting.value = false
  }
}

// ========== 初始化 ==========
onMounted(async () => {
  await Promise.all([
    loadCustomers(),
    loadMaterialsAndCategories(),
    loadDesigners(),
  ])
  // 设计师默认为当前登录用户
  if (authStore.userInfo?.id) {
    form.designerId = authStore.userInfo.id
    form.designerName = authStore.userInfo.real_name || authStore.userInfo.username || ''
  }
})
</script>

<style scoped>
/* ====== 卡片样式 ====== */
.order-info-card {
  margin-bottom: 20px;
}
.card {
  background: var(--bg);
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 20px;
}
.card-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: var(--text1);
  padding-bottom: 14px;
  margin-bottom: 16px;
  border-bottom: 1.5px solid var(--border);
}

/* ====== 表单行 ====== */
.form-row {
  display: flex;
  gap: 16px;
  margin-bottom: 14px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text2);
  white-space: nowrap;
}
.required {
  color: #f56c6c;
  margin-right: 2px;
}
.form-control {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 14px;
  outline: none;
  transition: all .2s;
  box-sizing: border-box;
  width: 100%;
  background: #fff;
  color: var(--text1);
}
.form-control:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.15);
}
textarea.form-control { resize: vertical; }

/* 提交人信息 */
.creator-info {
  margin-top: 14px;
  padding: 10px 14px;
  background: linear-gradient(135deg, rgba(var(--primary-rgb),0.06), rgba(var(--primary-rgb),0.02));
  border-radius: 8px;
  font-size: 13px;
  color: var(--text2);
  display: flex;
  align-items: center;
}

/* ====== 物料区域 ====== */
.materials-card {
  position: relative;
}
.materials-scroll-area {
  max-height: calc(100vh - 520px);
  min-height: 240px;
  overflow-y: auto;
  overflow-x: hidden;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fafafa;
}
.materials-scroll-area::-webkit-scrollbar { width: 6px; }
.materials-scroll-area::-webkit-scrollbar-thumb {
  background: #c0c4cc; border-radius: 3px;
}

/* 两列并排物料布局 */
.materials-two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  max-height: calc(100vh - 520px);
  min-height: 240px;
  overflow-y: auto;
}
.materials-two-col::-webkit-scrollbar { width: 6px; }
.materials-two-col::-webkit-scrollbar-thumb {
  background: #c0c4cc; border-radius: 3px;
}
.materials-col {
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
  background: #fafafa;
}
.materials-col .data-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
}
.materials-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: #909399;
}

.row-selected {
  background: rgba(var(--primary-rgb), 0.05) !important;
}

/* 数据表格 */
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.data-table thead th {
  background: #f5f7fa;
  color: var(--text2);
  font-weight: 600;
  padding: 9px 8px;
  text-align: left;
  position: sticky;
  top: 0;
  z-index: 1;
  border-bottom: 1px solid var(--border);
  font-size: 12px;
}
.data-table tbody td {
  padding: 7px 8px;
  border-bottom: 1px solid #f0f0f0;
  vertical-align: middle;
  transition: background .15s;
}
.data-table tbody tr:hover {
  background: #f8faff;
}

.tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 11px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
}

/* 输入框微调 */
.qty-input, .price-input {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
  font-size: 13px;
  color: var(--text1);
  transition: all .2s;
}
.qty-input:focus, .price-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.15);
}
.price-input {
  color: #e6a23c;
  font-weight: 500;
}

/* 小按钮 */
.btn-sm {
  padding: 3px 8px;
  font-size: 12px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all .2s;
}
.btn-primary-outline {
  color: var(--primary);
  background: #ecf5ff;
  border-color: #b3d8ff;
}
.btn-primary-outline:hover {
  background: var(--primary);
  color: #fff;
}

.delete-sm {
  width:24px;height:24px;padding:0;font-size:12px;color:#f56c6c;border:none;background:#fef0f0;
  border-radius:4px;cursor:pointer;transition:all .2s;
}
.delete-sm:hover:not(:disabled){background:#f56c6c;color:#fff;}

.loading-placeholder {
  text-align: center;
  padding: 60px 0;
  color: #909399;
}

/* ====== 底部汇总栏 ====== */
.bottom-summary {
  position: sticky;
  bottom: 0;
  z-index: 50;
  margin-top: 20px;
  background: #fff;
  border: 1.5px solid var(--primary);
  border-radius: 12px;
  box-shadow: 0 -4px 24px rgba(0,0,0,.08);
  animation: slideUp .25s ease-out;
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
.summary-content {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 14px 20px;
  flex-wrap: wrap;
}
.summary-item {
  font-size: 14px;
  color: var(--text2);
  display: flex;
  align-items: center;
  gap: 4px;
}
.summary-divider {
  width: 1px;
  height: 22px;
  background: var(--border);
}
.summary-total {
  font-size: 16px !important;
  color: var(--text1);
}
.summary-total strong {
  color: var(--primary);
  font-size: 20px;
}
.submit-btn {
  padding: 10px 28px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px;
  margin-left: auto;
}

/* ====== 面包屑 & 页面头 ====== */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  margin-bottom: 16px;
  color: var(--text2);
}
.breadcrumb-item {
  cursor: pointer;
  transition: color .2s;
}
.breadcrumb-item:hover:not(.active) { color: var(--primary); }
.breadcrumb-separator { color: #c0c4cc; }
.breadcrumb-item.active { color: var(--text1); font-weight: 600; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text1);
  margin: 0;
}
.page-actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all .2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.btn-primary {
  background: var(--primary);
  color: #fff;
}
.btn-primary:hover:not(:disabled) {
  background: var(--primary-dark, var(--primary));
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(var(--primary-rgb),.35);
}
.btn-primary:disabled { opacity: .55; cursor: not-allowed; }
.btn-default {
  background: #f5f7fa;
  color: var(--text2);
  border: 1px solid #dcdfe6;
}
.btn-default:hover { background: #ebeef5; }
</style>
