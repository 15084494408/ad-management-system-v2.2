<template>
  <header class="app-header">
    <!-- 左侧：Logo -->
    <div class="header-left">
      <div class="logo">
        <div class="logo-icon">企</div>
        <span class="logo-text">企业版图文广告管理系统 V2.2</span>
      </div>
    </div>

    <!-- 右侧：快速记账 / 公司切换 / 通知 / 用户 -->
    <div class="header-right">
      <button class="quick-btn" @click="showQuickAccount = true">
        <span>+</span> 快速记账
      </button>

      <!-- ★ 多公司切换（仅管理员可见） -->
      <el-dropdown v-if="authStore.isSuperAdmin" @command="handleSwitchCompany" trigger="click" style="margin-right: 12px;">
        <button class="company-switch-btn">
          🏢 {{ authStore.currentCompany?.companyName || '切换公司' }}
          <span style="font-size:10px;opacity:0.7;">▼</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="c in authStore.companyList"
              :key="c.id"
              :command="c.id"
              :class="{ 'company-active': c.id === authStore.currentCompanyId }"
            >
              <span>{{ c.companyType === 'headquarters' ? '🏛️' : '🏪' }}</span>
              {{ c.companyName }}
              <span v-if="c.id === authStore.currentCompanyId" style="color:#67c23a;margin-left:6px;">✓</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <div class="notif-wrapper">
        <button class="notif-btn" @click="showNotif = !showNotif">
          <span class="notif-icon">🔔</span>
          <span class="notif-badge">3</span>
        </button>
      </div>

      <el-dropdown @command="handleUserCmd">
        <div class="user-info">
          <div class="user-avatar">{{ avatarText }}</div>
          <div class="user-meta" v-if="!collapsed">
            <div class="user-name">{{ authStore.userInfo?.realName || authStore.userInfo?.username || '系统管理员' }}</div>
            <div class="user-role">{{ roleDisplay }}</div>
          </div>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">👤 个人信息</el-dropdown-item>
            <el-dropdown-item command="settings">⚙️ 系统设置</el-dropdown-item>
            <el-dropdown-item divided command="logout">🚪 退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>

  <!-- 快速记账弹窗（Element Plus 版，含物料明细） -->
  <Teleport to="body">
    <el-dialog v-model="showQuickAccount" title="快速记账" width="720px"
      @closed="resetQuickForm" :close-on-click-modal="false" append-to-body>
      <el-form :model="quickForm" label-width="90px" :rules="quickRules" ref="quickFormRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型" prop="type">
              <el-radio-group v-model="quickForm.type">
                <el-radio value="income">收入</el-radio>
                <el-radio value="expense">支出</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="quickForm.category" style="width:100%" placeholder="选择分类">
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
                  <el-option label="图文类" value="图文类"/>
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
              <div style="display:flex;gap:6px;width:100%;">
                <el-select v-model="quickForm.relatedName" filterable allow-create default-first-option placeholder="选择或输入" style="flex:1;">
                  <el-option v-if="retailCustomer" :label="'🏷️ ' + (retailCustomer.customerName || retailCustomer.name) + '（公共客户）'" :value="retailCustomer.customerName || retailCustomer.name" style="color:#e6a23c;font-weight:600;"/>
                  <el-option v-for="c in companyList" :key="c.id || c" :label="c.name || c" :value="c.name || c"/>
                </el-select>
                <el-button size="default" @click="quickForm.relatedName = retailCustomer ? (retailCustomer.customerName || retailCustomer.name) : '零售客户'" style="flex-shrink:0;border-color:#e6a23c;color:#e6a23c;background:#fdf6ec;" title="零散记账默认选择零售客户">🏷️ 零售</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支付方式" prop="paymentMethod">
              <el-select v-model="quickForm.paymentMethod" style="width:100%" placeholder="选择支付方式">
                <el-option label="现金" value="cash"/>
                <el-option label="微信" value="wechat"/>
                <el-option label="支付宝" value="alipay"/>
                <el-option label="银行转账" value="transfer"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="quickForm.remark" type="textarea" :rows="2" placeholder="可选"/>
        </el-form-item>

        <!-- 物料明细区域 -->
        <el-divider content-position="left">
          <span style="font-size:14px;">物料明细</span>
          <span style="color:#909399;font-size:12px;margin-left:8px;">（添加物料将自动扣减库存）</span>
        </el-divider>

        <div class="quick-material-items">
          <div v-for="(item, index) in quickForm.items" :key="index" class="quick-material-row">
            <el-row :gutter="8" align="middle">
              <el-col :span="7">
                <el-select v-model="item.materialId" filterable placeholder="选择物料" style="width:100%"
                  @change="(val: any) => onQuickMaterialSelect(item, val)" value-key="id">
                  <el-option v-for="m in materialList" :key="m.id" :label="m.name" :value="m.id">
                    <div style="display:flex;justify-content:space-between;align-items:center;">
                      <span>{{ m.name }}</span>
                      <span style="color:#909399;font-size:12px;">
                        ¥{{ m.price?.toFixed(2) }}{{ m.pricingType === 1 ? '/㎡' : '' }}
                        <el-tag v-if="m.pricingType !== 1" size="small" type="info" style="margin-left:4px;">库存:{{ m.stockQuantity }}</el-tag>
                      </span>
                    </div>
                  </el-option>
                </el-select>
              </el-col>
              <!-- 按数量模式 - 左右型数量选择器 -->
              <el-col :span="5" v-if="item.pricingType !== 1">
                <div class="qty-stepper">
                  <button type="button" class="qty-btn qty-dec" @click="item.quantity = Math.max(1, (item.quantity || 1) - 1); calcQuickItemTotal(item)">−</button>
                  <input
                    type="number"
                    class="qty-input"
                    v-model.number="item.quantity"
                    min="1"
                    step="1"
                    @change="item.quantity = Math.max(1, Math.floor(item.quantity || 1)); calcQuickItemTotal(item)"
                  />
                  <button type="button" class="qty-btn qty-inc" @click="item.quantity = (item.quantity || 1) + 1; calcQuickItemTotal(item)">+</button>
                </div>
              </el-col>
              <!-- 按面积模式 -->
              <template v-else>
                <el-col :span="3">
                  <el-input-number v-model="item.width" :min="0.01" :precision="2" placeholder="宽"
                    controls-position="right" style="width:100%;" @change="calcQuickItemTotal(item)"/>
                </el-col>
                <el-col :span="1" style="text-align:center;line-height:32px;color:#909399;">×</el-col>
                <el-col :span="3">
                  <el-input-number v-model="item.height" :min="0.01" :precision="2" placeholder="高"
                    controls-position="right" style="width:100%;" @change="calcQuickItemTotal(item)"/>
                </el-col>
                <el-col :span="1" style="text-align:center;line-height:32px;color:#909399;">㎡</el-col>
              </template>
              <el-col :span="4">
                <el-input v-model="item.totalPriceDisplay" placeholder="小计" readonly style="width:100%;">
                  <template #prefix>¥</template>
                </el-input>
              </el-col>
              <el-col :span="2">
                <el-button type="danger" link @click="removeQuickItem(index)" :disabled="quickForm.items.length <= 1">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-col>
            </el-row>
            <div v-if="item.stockWarning" class="quick-stock-warning">
              ⚠️ {{ item.stockWarning }}
            </div>
          </div>
        </div>

        <div style="display:flex;justify-content:space-between;align-items:center;margin-top:8px;">
          <el-button size="small" @click="addQuickItem">+ 添加物料行</el-button>
          <div class="quick-total-price">
            物料合计：<strong>¥{{ quickMaterialTotal }}</strong>
          </div>
        </div>

        <el-form-item label="记账金额" prop="amount" style="margin-top:16px;">
          <el-input-number v-model="quickForm.amount" :min="0.01" :precision="2" :step="100" style="width:100%;"/>
          <el-button v-if="Number(quickMaterialTotal) > 0" type="primary" link size="small"
            @click="quickForm.amount = Math.ceil(Number(quickMaterialTotal))" style="margin-left:8px;">
            填入物料合计（向上取整）
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showQuickAccount=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitQuickAccount">确认记账</el-button>
      </template>
    </el-dialog>
  </Teleport>

  <!-- 通知面板（样式由 prototype-ui.scss 全局提供） -->
  <Teleport to="body">
    <div v-if="showNotif" class="notif-overlay" @click.self="showNotif = false">
      <div class="notif-panel">
        <div class="notif-header">
          <span>系统通知</span>
          <span class="notif-clear" @click="showNotif = false">关闭</span>
        </div>
        <div class="notif-list">
          <div class="notif-item">
            <div class="notif-item-title">新订单待处理</div>
            <div class="notif-item-time">刚刚</div>
          </div>
          <div class="notif-item">
            <div class="notif-item-title">工厂账单已生成</div>
            <div class="notif-item-time">10分钟前</div>
          </div>
          <div class="notif-item">
            <div class="notif-item-title">会员充值成功 ¥500</div>
            <div class="notif-item-time">30分钟前</div>
          </div>
        </div>
        <div class="notif-footer">查看全部通知</div>
      </div>
    </div>
  </Teleport>

  <!-- 个人信息弹窗（样式由 prototype-ui.scss 全局提供） -->
  <Teleport to="body">
    <div class="modal-overlay" :class="{ show: showUserProfile }" @click.self="showUserProfile = false">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">👤 个人信息</h3>
          <button class="modal-close" @click="showUserProfile = false">×</button>
        </div>
        <div class="modal-body">
          <div class="profile-info">
            <div class="profile-avatar">{{ avatarText }}</div>
            <div class="profile-name">{{ authStore.userInfo?.realName || authStore.userInfo?.username || '未知用户' }}</div>
            <div class="profile-role">{{ roleDisplay }}</div>
          </div>
          <div class="profile-details">
            <div class="profile-row">
              <span class="profile-label">用户名</span>
              <span class="profile-value">{{ authStore.userInfo?.username || '-' }}</span>
            </div>
            <div class="profile-row">
              <span class="profile-label">真实姓名</span>
              <span class="profile-value">{{ authStore.userInfo?.realName || '-' }}</span>
            </div>
            <div class="profile-row">
              <span class="profile-label">角色</span>
              <span class="profile-value">{{ roleDisplay }}</span>
            </div>
            <div class="profile-row">
              <span class="profile-label">手机号</span>
              <span class="profile-value">{{ authStore.userInfo?.phone || '-' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useFinanceStore } from '@/stores/finance'
import { financeApi, customerApi, systemApi, materialApi } from '@/api'

defineEmits(['toggle-sidebar'])
const props = defineProps<{ collapsed?: boolean }>()

const router = useRouter()
const authStore = useAuthStore()
const financeStore = useFinanceStore()
const showNotif = ref(false)

const roleDisplay = computed(() => {
  const roles = authStore.userInfo?.roles
  if (!roles || roles.length === 0) return '普通用户'
  const roleMap: Record<string, string> = {
    'SUPER_ADMIN': '超级管理员',
    'ADMIN': '管理员',
    'FINANCE': '财务',
    'OPERATOR': '操作员'
  }
  return roles.map(r => roleMap[r] || r).join('/')
})

// ========== 快速记账弹窗（含物料明细） ==========
interface QuickMaterialItem {
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

const showQuickAccount = ref(false)
const submitting = ref(false)
const quickFormRef = ref<any>(null)
const companyList = ref<any[]>([])
const materialList = ref<any[]>([])
const retailCustomer = ref<any>(null)

async function loadRetailCustomer() {
  try {
    const r: any = await customerApi.getRetailCustomer()
    retailCustomer.value = r.data
  } catch { /* 静默 */ }
}

function createQuickEmptyItem(): QuickMaterialItem {
  return {
    materialId: null, materialName: '', pricingType: 0,
    quantity: 1, width: 0, height: 0, area: 0,
    unitPrice: 0, totalPrice: 0, totalPriceDisplay: '',
    stockWarning: '', _stockQuantity: 0
  }
}

const quickForm = reactive({
  type: 'income' as string,
  relatedName: '' as string,
  category: '图文类' as string,
  amount: 0 as number,
  paymentMethod: 'cash' as string,
  remark: '' as string,
  items: [createQuickEmptyItem()] as QuickMaterialItem[]
})

const quickRules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  relatedName: [{ required: true, message: '请选择或输入公司/客户名', trigger: 'change' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  paymentMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

const quickMaterialTotal = computed(() => {
  const sum = quickForm.items.reduce((acc, item) => acc + (item.totalPrice || 0), 0)
  return sum > 0 ? Math.ceil(sum).toFixed(2) : '0.00'
})

function onQuickMaterialSelect(item: QuickMaterialItem, materialId: number) {
  const m = materialList.value.find((mat: any) => mat.id === materialId)
  if (!m) return
  item.materialName = m.name
  item.pricingType = m.pricingType || 0
  item.unitPrice = m.price || 0
  item._stockQuantity = m.stockQuantity || 0
  item.quantity = 1; item.width = 0; item.height = 0; item.area = 0
  item.stockWarning = ''; item.totalPrice = 0; item.totalPriceDisplay = ''
  calcQuickItemTotal(item)
}

function calcQuickItemTotal(item: QuickMaterialItem) {
  if (item.pricingType === 1) {
    // 按面积计费（广告物料）：不检查库存
    if (item.width > 0 && item.height > 0) {
      item.area = Math.round(item.width * item.height * 100) / 100
      item.totalPrice = Math.ceil(item.area * item.unitPrice)
      item.stockWarning = '' // 广告物料不检查库存
    } else { item.totalPrice = 0; item.stockWarning = '' }
  } else {
    // 按数量计费（印刷物料）：检查库存
    if (item.quantity > 0 && item.unitPrice > 0) {
      item.totalPrice = Math.ceil(item.quantity * item.unitPrice)
      item.stockWarning = item.quantity > item._stockQuantity
        ? `库存不足！需要 ${item.quantity}，当前 ${item._stockQuantity}` : ''
    } else { item.totalPrice = 0; item.stockWarning = '' }
  }
  item.totalPriceDisplay = item.totalPrice > 0 ? item.totalPrice.toFixed(2) : ''
}

function addQuickItem() { quickForm.items.push(createQuickEmptyItem()) }
function removeQuickItem(index: number) {
  if (quickForm.items.length > 1) quickForm.items.splice(index, 1)
}

function resetQuickForm() {
  quickForm.type = 'income'
  quickForm.relatedName = ''
  quickForm.category = '图文类'
  quickForm.amount = 0
  quickForm.paymentMethod = 'cash'
  quickForm.remark = ''
  quickForm.items = [createQuickEmptyItem()]
}

// 提交快速记账
async function submitQuickAccount() {
  if (!quickFormRef.value) return
  await quickFormRef.value.validate()

  if (!quickForm.amount || quickForm.amount <= 0) {
    ElMessage.warning('请输入有效金额'); return
  }

  // 检查库存不足（仅警告）
  const warnings = quickForm.items.filter(item => item.materialId && item.stockWarning)
  if (warnings.length > 0) {
    try {
      await ElMessageBox.confirm(
        `有 ${warnings.length} 项物料库存不足，是否继续提交？`,
        '库存不足警告',
        { type: 'warning', confirmButtonText: '继续提交', cancelButtonText: '取消' }
      )
    } catch { return }
  }

  submitting.value = true
  try {
    const items = quickForm.items
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
      type: quickForm.type,
      category: quickForm.category,
      amount: quickForm.amount,
      relatedName: quickForm.relatedName,
      paymentMethod: quickForm.paymentMethod,
      remark: quickForm.remark,
      items: items.length > 0 ? items : undefined
    })

    ElMessage.success('记账成功' + (items.length > 0 ? `，已扣减 ${items.length} 项物料库存` : ''))
    showQuickAccount.value = false
    financeStore.triggerRefresh()
  } catch (e: any) {
    ElMessage.error(e?.message || '记账失败')
  } finally {
    submitting.value = false
  }
}

// 加载客户列表
async function loadCompanyList() {
  try {
    const r = await customerApi.getList({ current: 1, size: 200 })
    companyList.value = r.data?.records || []
  } catch { companyList.value = [] }
}

// 加载物料列表
async function loadMaterialList() {
  try {
    const r = await materialApi.listAll()
    materialList.value = r.data || []
  } catch { materialList.value = [] }
}

const avatarText = computed(() => {
  const name = authStore.userInfo?.realName || authStore.userInfo?.username || '管'
  return name.slice(-1)
})

// 用户信息弹窗
const showUserProfile = ref(false)

function handleUserCmd(cmd: string) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        await authStore.logout()
        router.push('/login')
      })
      .catch(() => {})
  } else if (cmd === 'profile') {
    showUserProfile.value = true
  } else if (cmd === 'settings') {
    router.push('/settings')
  }
}

// ★ 加载系统公司列表（多公司切换）
async function loadSystemCompanies() {
  try {
    const r = await systemApi.getCompanyList()
    authStore.companyList = r.data || []
  } catch {
    authStore.companyList = []
  }
}

// 切换公司
function handleSwitchCompany(id: number) {
  authStore.switchCompany(id)
  ElMessage.success('已切换到 ' + (authStore.companyList.find(c => c.id === id)?.companyName || ''))
}

// 弹窗打开后刷新客户和物料列表
watch(showQuickAccount, (val) => {
  if (val) {
    loadRetailCustomer()
    loadCompanyList()
    loadMaterialList()
  }
})

// 监听全局快速记账事件（Dashboard 等页面触发）
function onShowQuickAccount() {
  showQuickAccount.value = true
}
onMounted(() => {
  window.addEventListener('show-quick-account', onShowQuickAccount)
  loadCompanyList()
  loadMaterialList()
  loadSystemCompanies()
})
onUnmounted(() => {
  window.removeEventListener('show-quick-account', onShowQuickAccount)
})
</script>

<style scoped lang="scss">
.app-header {
  position: fixed; top: 0; left: 0; right: 0; z-index: 1000;
  height: 60px;
  background: $bg-white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  border-bottom: 1px solid #f0f0f0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px;
}
.header-left { display: flex; align-items: center; gap: 10px; }
.logo {
  display: flex; align-items: center; gap: 10px;
  font-size: 17px; font-weight: 700; color: $primary;
  letter-spacing: 0.3px;
}
.logo-icon {
  width: 34px; height: 34px; border-radius: 9px;
  background: linear-gradient(135deg, $primary, #66b1ff);
  color: #fff; font-weight: bold; font-size: 15px;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 3px 8px rgba(64,158,255,0.35);
}
.header-right { display: flex; align-items: center; gap: 12px; }
.quick-btn {
  background: linear-gradient(135deg, $success, #85ce61);
  color: #fff; border: none; padding: 8px 18px;
  border-radius: 20px; cursor: pointer; font-size: 13px;
  font-weight: 500; transition: all 0.3s;
  display: flex; align-items: center; gap: 6px;
  box-shadow: 0 2px 8px rgba(103,194,58,0.3);
  &:hover {
    transform: scale(1.05);
    box-shadow: 0 5px 15px rgba(103, 194, 58, 0.45);
  }
}
.company-switch-btn {
  background: #f0f9eb; color: #67c23a; border: 1px solid #c2e7b0;
  padding: 6px 14px; border-radius: 16px; cursor: pointer; font-size: 13px;
  font-weight: 500; transition: all 0.3s; display: flex; align-items: center; gap: 6px;
  &:hover { background: #e1f3d8; border-color: #67c23a; }
}
.notif-wrapper { position: relative; }
.notif-btn {
  position: relative; width: 38px; height: 38px;
  border-radius: 50%; background: #f5f7fa;
  border: 1px solid #eee; cursor: pointer;
  font-size: 17px; display: flex;
  align-items: center; justify-content: center;
  transition: all 0.2s;
  &:hover { background: #ecf5ff; border-color: #409eff; }
}
.notif-badge {
  position: absolute; top: -3px; right: -3px;
  min-width: 18px; height: 18px; background: #f56c6c;
  color: #fff; border-radius: 9px; font-size: 10px;
  font-weight: 600; display: flex; align-items: center;
  justify-content: center; padding: 0 4px;
  border: 2px solid #fff;
  animation: badgePulse 2s infinite;
}
@keyframes badgePulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(245,108,108,0.4); }
  50% { box-shadow: 0 0 0 4px rgba(245,108,108,0); }
}
.user-info {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 12px; border-radius: 22px; cursor: pointer;
  border: 1px solid transparent; transition: all 0.2s;
  &:hover { background: $bg-base; border-color: #eee; }
}
.user-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: linear-gradient(135deg, $primary, #66b1ff);
  color: #fff; font-size: 14px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 6px rgba(64,158,255,0.3);
}
.user-meta { display: flex; flex-direction: column; }
.user-name { font-size: 13px; color: $text-primary; }
.user-role { font-size: 11px; color: $text-secondary; }

// 快速记账 - 物料明细
.quick-material-items {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
}
.quick-material-row {
  margin-bottom: 8px;
  padding: 8px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  &:last-child { margin-bottom: 0; }
}
.quick-stock-warning {
  color: #e6a23c;
  font-size: 12px;
  margin-top: 4px;
}
.quick-total-price {
  font-size: 14px;
  color: #303133;
  strong { color: #409eff; font-size: 18px; }
}

/* 数量左右型选择器 */
.qty-stepper {
  display: flex;
  align-items: center;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
  height: 32px;
}
.qty-btn {
  width: 32px;
  height: 100%;
  border: none;
  background: #f5f7fa;
  color: #409eff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  &:hover { background: #409eff; color: #fff; }
  &:active { background: #337ecc; color: #fff; }
}
.qty-dec {
  border-right: 1px solid #dcdfe6;
  border-radius: 6px 0 0 6px;
  &:hover { border-radius: 6px 0 0 6px; }
}
.qty-inc {
  border-left: 1px solid #dcdfe6;
  border-radius: 0 6px 6px 0;
  &:hover { border-radius: 0 6px 6px 0; }
}
.qty-input {
  flex: 1;
  border: none;
  outline: none;
  text-align: center;
  font-size: 13px;
  color: #303133;
  background: transparent;
  width: 0;
  min-width: 0;
  -moz-appearance: textfield;
  &::-webkit-outer-spin-button,
  &::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
}
</style>
