<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item">系统管理</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item active">系统配置</span>
    </div>

    <div class="page-header"><span class="page-title">⚙️ 系统配置</span></div>

    <div class="config-layout">
      <div class="config-sidebar card">
        <div v-for="tab in tabs" :key="tab.key" class="config-sidebar-item" :class="{active: activeTab===tab.key}" @click="activeTab=tab.key">
          <span>{{ tab.icon }} {{ tab.name }}</span>
          <el-tag v-if="tab.badge" size="small" type="success" effect="dark" round>{{ tab.badge }}</el-tag>
        </div>
      </div>

      <div class="config-content">
        <!-- 基础配置 -->
        <div v-show="activeTab==='base'" class="card">
          <div class="card-header-row"><span class="card-title">🏠 基础配置</span><el-button type="primary" size="small" @click="saveBase">💾 保存设置</el-button></div>
          <el-form :model="baseConfig" label-width="120px" style="max-width:600px;">
            <el-form-item label="企业名称"><el-input v-model="baseConfig.companyName" placeholder="请输入企业名称" /></el-form-item>
            <el-form-item label="联系电话"><el-input v-model="baseConfig.phone" placeholder="请输入联系电话" /></el-form-item>
            <el-form-item label="企业地址"><el-input v-model="baseConfig.address" placeholder="请输入企业地址" /></el-form-item>
            <el-form-item label="默认税率(%)"><el-input-number v-model="baseConfig.taxRate" :min="0" :max="100" /></el-form-item>
            <el-form-item label="系统主题色">
              <div style="display:flex;align-items:center;gap:10px;">
                <el-color-picker v-model="baseConfig.themeColor" @change="applyTheme" />
                <span style="color:#909399;font-size:13px;">点击选择自定义主题颜色</span>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <!-- 物料单价 -->
        <div v-show="activeTab==='material'" class="card">
          <div class="card-header-row"><span class="card-title">📦 物料单价</span><el-button type="primary" size="small" @click="saveMaterial">💾 保存</el-button></div>
          <p style="color:#909399;margin-bottom:15px;font-size:13px;">设置各类物料的参考单价，用于订单自动报价</p>
          <el-table :data="materialPrices" stripe>
            <el-table-column prop="name" label="物料名称" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column prop="price" label="参考单价" width="150">
              <template #default="{ row }"><el-input-number v-model="row.price" :min="0" :precision="2" size="small" /></template>
            </el-table-column>
            <el-table-column label="操作" width="80"><template #default><el-button size="small" type="primary">保存</el-button></template></el-table-column>
          </el-table>
        </div>

        <!-- 工艺价格 -->
        <div v-show="activeTab==='process'" class="card">
          <div class="card-header-row"><span class="card-title">⚒️ 工艺价格</span><el-button type="primary" size="small">💾 保存</el-button></div>
          <p style="color:#909399;margin-bottom:15px;font-size:13px;">设置各类工艺的加工价格</p>
          <el-table :data="processPrices" stripe>
            <el-table-column prop="name" label="工艺名称" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column prop="price" label="单价" width="150">
              <template #default="{ row }"><el-input-number v-model="row.price" :min="0" :precision="2" size="small" /></template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 订单状态 -->
        <div v-show="activeTab==='orderStatus'" class="card">
          <div class="card-header-row"><span class="card-title">📊 订单状态</span><el-button type="primary" size="small">💾 保存</el-button></div>
          <p style="color:#909399;margin-bottom:15px;font-size:13px;">配置订单流转状态及显示样式</p>
          <el-table :data="orderStatuses" stripe>
            <el-table-column prop="code" label="状态编码" width="120" />
            <el-table-column prop="name" label="状态名称" width="100" />
            <el-table-column label="颜色" width="150">
              <template #default="{ row }">
                <el-tag :type="row.tagType || ''" :style="row.tagType ? {} : {background:row.color,color:'#fff',border:'none'}">{{ row.name }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="next" label="流转至" />
          </el-table>
        </div>

        <!-- 客户标签 -->
        <div v-show="activeTab==='tags'" class="card">
          <div class="card-header-row"><span class="card-title">🏷️ 客户标签</span><el-button type="primary" size="small" @click="showAddTagDialog=true">+ 新增标签</el-button></div>
          <div class="tag-list">
            <el-tag v-for="t in customerTags" :key="t.id" :type="t.type" closable @close="removeTag(t)" style="margin:5px;padding:8px 14px;font-size:13px;">
              {{ t.name }}
            </el-tag>
          </div>
          <el-button class="btn-add-tag" size="small" @click="showAddTagDialog=true">+ 添加标签</el-button>
        </div>

        <!-- 支付方式 -->
        <div v-show="activeTab==='pay'" class="card">
          <div class="card-header-row"><span class="card-title">💳 支付方式</span><el-button type="primary" size="small" @click="savePayMethods">💾 保存</el-button></div>
          <el-table :data="payMethods" stripe>
            <el-table-column prop="code" label="方式编码" width="100" />
            <el-table-column prop="name" label="方式名称" width="120" />
            <el-table-column prop="icon" label="图标" width="80" />
            <el-table-column label="是否启用" width="100">
              <template #default="{ row }"><el-switch v-model="row.enabled" /></template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 流水类型 -->
        <div v-show="activeTab==='flow'" class="card">
          <div class="card-header-row">
            <span class="card-title">💵 流水类型 <el-tag type="success" size="small" effect="dark" round>V2.2</el-tag></span>
            <el-button type="primary" size="small" @click="openAddFlowType">+ 添加类型</el-button>
          </div>
          <el-table :data="flowTypes" stripe>
            <el-table-column prop="code" label="类型编码" width="100" />
            <el-table-column label="类型名称" width="150">
              <template #default="{ row }">{{ row.icon }} {{ row.name }}</template>
            </el-table-column>
            <el-table-column prop="icon" label="图标" width="80"><template #default="{ row }">{{ row.icon }}</template></el-table-column>
            <el-table-column prop="sort" label="排序" width="80" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }"><el-tag :type="row.status===1?'success':'info'" size="small">{{ row.status===1?'启用':'禁用' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="editFlowType(row)">编辑</el-button>
                <el-button size="small" :type="row.status===1?'warning':'success'" @click="toggleFlowType(row)">{{ row.status===1?'禁用':'启用' }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 新增标签弹窗 -->
    <el-dialog v-model="showAddTagDialog" title="🏷️ 添加客户标签" width="400px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="标签名称"><el-input v-model="newTagName" placeholder="请输入标签名称" /></el-form-item>
        <el-form-item label="标签颜色">
          <div style="display:flex;gap:10px;">
            <span v-for="c in tagColors" :key="c.type" class="color-dot" :class="{active: newTagType===c.type}" :style="{background:c.bg}" @click="newTagType=c.type" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddTagDialog=false">取消</el-button>
        <el-button type="primary" @click="addTag">💾 保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑流水类型弹窗 -->
    <el-dialog v-model="showFlowTypeDialog" title="✏️ 编辑流水类型" width="500px" destroy-on-close>
      <el-form :model="flowTypeForm" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型编码"><el-input :model-value="flowTypeForm.code" disabled /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型名称"><el-input v-model="flowTypeForm.name" placeholder="请输入" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="显示图标">
              <el-select v-model="flowTypeForm.icon" style="width:100%;">
                <el-option v-for="i in icons" :key="i" :label="i" :value="i" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序"><el-input-number v-model="flowTypeForm.sort" :min="1" style="width:100%;" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showFlowTypeDialog=false">取消</el-button>
        <el-button type="primary" @click="saveFlowType">💾 保存修改</el-button>
      </template>
    </el-dialog>

    <!-- 新增流水类型弹窗 -->
    <el-dialog v-model="showAddFlowDialog" title="➕ 添加流水类型" width="500px" destroy-on-close>
      <el-form :model="newFlowForm" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型编码"><el-input v-model="newFlowForm.code" placeholder="如: xcjz" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型名称"><el-input v-model="newFlowForm.name" placeholder="如: 写真机制作" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="显示图标">
              <el-select v-model="newFlowForm.icon" style="width:100%;">
                <el-option v-for="i in icons" :key="i" :label="i" :value="i" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序"><el-input-number v-model="newFlowForm.sort" :min="1" style="width:100%;" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="默认启用">
          <el-switch v-model="newFlowForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddFlowDialog=false">取消</el-button>
        <el-button type="primary" @click="addFlowType">💾 保存类型</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('base')
const tabs = [
  { key: 'base', name: '基础配置', icon: '🏠' },
  { key: 'material', name: '物料单价', icon: '📦' },
  { key: 'process', name: '工艺价格', icon: '⚒️' },
  { key: 'orderStatus', name: '订单状态', icon: '📊' },
  { key: 'tags', name: '客户标签', icon: '🏷️' },
  { key: 'pay', name: '支付方式', icon: '💳' },
  { key: 'flow', name: '流水类型', icon: '💵', badge: 'V2.2' },
]

const baseConfig = reactive({ companyName: '广告图文快印', phone: '', address: '', taxRate: 6, themeColor: '#409eff' })
const materialPrices = ref([
  { id: 1, name: 'A4铜版纸', unit: '张', price: 0.50 },
  { id: 2, name: '彩色油墨', unit: '瓶', price: 120.00 },
  { id: 3, name: '覆膜材料', unit: '㎡', price: 3.00 },
])
const processPrices = ref([
  { id: 1, name: '彩色印刷', unit: '张', price: 1.20 },
  { id: 2, name: '覆膜（光膜）', unit: '㎡', price: 8.00 },
  { id: 3, name: '装订', unit: '本', price: 5.00 },
])
const orderStatuses = ref([
  { code: 'pending', name: '待确认', color: '#409eff', tagType: '', next: '设计中' },
  { code: 'designing', name: '设计中', color: '#e6a23c', tagType: 'warning', next: '待交付' },
  { code: 'delivery', name: '待交付', color: '#67c23a', tagType: 'success', next: '已完成' },
  { code: 'completed', name: '已完成', color: '#1890ff', tagType: '', next: '-' },
  { code: 'cancelled', name: '已取消', color: '#f56c6c', tagType: 'danger', next: '-' },
])
const customerTags = ref([
  { id: 1, name: '优质客户', type: 'success' },
  { id: 2, name: '长期合作', type: 'warning' },
  { id: 3, name: '散客', type: '' },
  { id: 4, name: '政府客户', type: 'info' },
])
const payMethods = ref([
  { code: 'cash', name: '现金', icon: '💵', enabled: true },
  { code: 'wechat', name: '微信支付', icon: '💚', enabled: true },
  { code: 'alipay', name: '支付宝', icon: '💙', enabled: true },
  { code: 'bank', name: '银行卡/转账', icon: '💳', enabled: true },
  { code: 'other', name: '其他', icon: '📱', enabled: true },
])
const flowTypes = ref([
  { id: 1, code: 'twsx', name: '图文打印', icon: '📄', sort: 1, status: 1 },
  { id: 2, code: 'fysj', name: '复印扫描', icon: '📋', sort: 2, status: 1 },
  { id: 3, code: 'zd', name: '装订', icon: '📒', sort: 3, status: 1 },
  { id: 4, code: 'ggzz', name: '广告制作', icon: '🎨', sort: 4, status: 1 },
  { id: 5, code: 'qt', name: '其他', icon: '📦', sort: 5, status: 1 },
])
const icons = ['📄','📋','📒','🎨','🖼️','🖥️','📦','✏️']

// 标签
const showAddTagDialog = ref(false)
const newTagName = ref('')
const newTagType = ref('success')
const tagColors = [
  { type: '', bg: '#ecf5ff' },
  { type: 'success', bg: '#f0f9eb' },
  { type: 'warning', bg: '#fdf6ec' },
  { type: 'info', bg: '#f4f4f5' },
  { type: 'danger', bg: '#fef0f0' },
]

function addTag() {
  if (!newTagName.value.trim()) { ElMessage.warning('请输入标签名称'); return }
  customerTags.value.push({ id: Date.now(), name: newTagName.value.trim(), type: newTagType.value })
  ElMessage.success(`标签"${newTagName.value.trim()}"添加成功！`)
  newTagName.value = ''
  showAddTagDialog.value = false
}
function removeTag(t: any) { customerTags.value = customerTags.value.filter(x => x.id !== t.id) }

// 流水类型
const showFlowTypeDialog = ref(false)
const showAddFlowDialog = ref(false)
const flowTypeForm = reactive({ code: '', name: '', icon: '📄', sort: 1 })
const newFlowForm = reactive({ code: '', name: '', icon: '📄', sort: 6, enabled: true })

function editFlowType(row: any) {
  Object.assign(flowTypeForm, { code: row.code, name: row.name, icon: row.icon, sort: row.sort })
  showFlowTypeDialog.value = true
}
function openAddFlowType() {
  Object.assign(newFlowForm, { code: '', name: '', icon: '📄', sort: flowTypes.value.length + 1, enabled: true })
  showAddFlowDialog.value = true
}
function saveFlowType() {
  ElMessage.success('流水类型已更新！')
  showFlowTypeDialog.value = false
}
function addFlowType() {
  if (!newFlowForm.code || !newFlowForm.name) { ElMessage.warning('请填写编码和名称'); return }
  flowTypes.value.push({ id: Date.now(), code: newFlowForm.code, name: newFlowForm.name, icon: newFlowForm.icon, sort: newFlowForm.sort, status: newFlowForm.enabled ? 1 : 0 })
  ElMessage.success('流水类型添加成功！')
  showAddFlowDialog.value = false
}
function toggleFlowType(row: any) { row.status = row.status === 1 ? 0 : 1 }

// 保存
function saveBase() { ElMessage.success('基础配置已保存！') }
function saveMaterial() { ElMessage.success('物料单价已保存！') }
function savePayMethods() { ElMessage.success('支付方式已保存！') }
function applyTheme(color: string) { document.documentElement.style.setProperty('--el-color-primary', color) }
</script>

<style scoped lang="scss">
.config-layout { display: grid; grid-template-columns: 220px 1fr; gap: 24px; align-items: start; }
.config-sidebar { padding: 12px; position: sticky; top: 80px; }
.config-sidebar-item { padding: 12px 14px; border-radius: 6px; margin-bottom: 6px; cursor: pointer; display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #606266; transition: all 0.2s; border: none; background: transparent; }
.config-sidebar-item:hover { background: #f5f7fa; color: #409eff; }
.config-sidebar-item.active { background: linear-gradient(135deg, #409eff, #66b1ff); color: #fff; }
.tag-list { display: flex; flex-wrap: wrap; gap: 0; margin-bottom: 10px; }
.btn-add-tag { margin-top: 5px; }
.color-dot { width: 30px; height: 30px; border-radius: 6px; cursor: pointer; display: inline-block; border: 2px solid transparent; transition: all 0.2s; }
.color-dot.active { outline: 2px solid #409eff; }
</style>
