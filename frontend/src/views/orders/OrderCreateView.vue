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
        <button v-if="selectedMaterials.length > 0" class="btn btn-success" @click="showQuotePreview = true">📋 预览报价单</button>
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
        <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap;">
          <input type="text" v-model="materialKeyword" class="form-control"
            placeholder="搜索物料名称..." style="width:180px;padding:4px 8px;"
            @input="filterMaterials" />
          <select v-model="materialCategoryFilter" class="form-control"
            style="width:130px;padding:4px 8px;" @change="filterMaterials">
            <option value="">全部分类</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
          <!-- ★ 新增：手动添加自定义物料按钮 -->
          <button class="btn btn-sm btn-custom-add" @click="addCustomMaterial" title="添加不在预设列表中的临时物料">
            ✏️ 手动物料
          </button>
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
        <div v-else class="materials-empty">暂无匹配的物料（可点击「✏️ 手动物料」手动添加）</div>
      </div>

      <!-- 加载中 -->
      <div v-else class="loading-placeholder">
        <p>正在加载物料数据...</p>
      </div>
    </div>

    <!-- ★ 新增：已选物料明细表（含自定义物料的可编辑行） -->
    <div class="card selected-materials-card" v-if="selectedMaterials.length > 0">
      <div class="card-title-bar">
        <span>🛒 已选物料清单</span>
        <span style="color:#67c23a;font-size:12px;">点击字段可直接编辑 · 自定义物料显示 🏷️ 标记</span>
        <span v-if="canViewCost" style="color:#909399;font-size:11px;margin-left:8px;">（管理员可见成本）</span>
      </div>
      <table class="data-table selected-table">
        <thead>
          <tr>
            <th style="width:40px;text-align:center;">#</th>
            <th style="width:20%;">物料名称</th>
            <th style="width:12%;">规格</th>
            <th style="width:55px;text-align:center;">单位</th>
            <th style="width:65px;text-align:center;">数量</th>
            <th style="width:80px;text-align:right;">单价(¥)</th>
            <th style="width:100px;text-align:right;">小计(¥)</th>
            <th v-if="canViewCost" style="width:80px;text-align:right;">成本(¥)</th>
            <th style="width:55px;text-align:center;">类型</th>
            <th style="width:65px;text-align:center;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(s, idx) in selectedMaterials" :key="s.materialId || idx"
            :class="{ 'custom-row': s.isCustom }">
            <td style="text-align:center;color:#909399;">{{ idx + 1 }}</td>
            <!-- 自定义物料：名称可编辑；预设物料：只读 -->
            <td v-if="s.isCustom">
              <input type="text" v-model="s.materialName" class="inline-edit-input"
                placeholder="输入物料名称" style="width:100%;" />
            </td>
            <td v-else><strong>{{ s.materialName }}</strong></td>
            <td v-if="s.isCustom">
              <input type="text" v-model="s.spec" class="inline-edit-input"
                placeholder="规格描述" style="width:100%;" />
            </td>
            <td v-else style="font-size:12px;">{{ s.spec || '-' }}</td>
            <td v-if="s.isCustom">
              <input type="text" v-model="s.unit" class="inline-edit-input center-text"
                placeholder="单位" style="width:50px;" />
            </td>
            <td v-else style="text-align:center;">{{ s.unit || '个' }}</td>
            <td style="text-align:center;">
              <input type="number" v-model.number="s.quantity"
                class="qty-inline" min="0.01" step="1"
                @change="recalcSelectedItem(idx)" />
            </td>
            <td style="text-align:right;">
              <input type="number" v-model.number="s.unitPrice"
                class="price-inline" min="0" step="0.01"
                @change="recalcSelectedItem(idx)" />
            </td>
            <td style="text-align:right;font-weight:600;color:#e6a23c;">
              ¥{{ formatMoney(s.amount) }}
            </td>
            <td v-if="canViewCost" style="text-align:right;">
              <input type="number" v-model.number="s.unitCost"
                class="cost-inline" min="0" step="0.01"
                @change="recalcSelectedItem(idx)"
                :disabled="!s.isCustom && s.unitCost != null" :title="s.isCustom ? '可编辑' : '预设物料成本'" />
            </td>
            <td style="text-align:center;">
              <span v-if="s.isCustom" class="tag-custom">🏷️ 自定义</span>
              <span v-else class="tag-preset">📦 预设</span>
            </td>
            <td style="text-align:center;">
              <button class="action-btn delete-sm" title="移除此物料"
                @click="removeMaterialByIndex(idx)">✕</button>
            </td>
          </tr>
        </tbody>
        <tfoot v-if="selectedMaterials.length > 0">
          <tr class="total-row">
            <td :colspan="canViewCost ? 7 : 6" style="text-align:right;font-weight:600;">物料合计：</td>
            <td :colspan="canViewCost ? 4 : 3" style="text-align:right;font-weight:700;color:#e6a23c;font-size:16px;">
              ¥{{ formatMoney(materialTotal) }}
            </td>
          </tr>
        </tfoot>
      </table>
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

    <!-- ============================================================ -->
    <!-- ★★★ 报价单预览弹窗（含导出功能） ★★★ -->
    <!-- ============================================================ -->
    <div class="quote-overlay" v-if="showQuotePreview" @click.self="showQuotePreview = false">
      <div class="quote-dialog">
        <!-- 报价单头部 -->
        <div class="quote-header">
          <div class="quote-header-left">
            <h2 class="quote-company-name">{{ companyInfo.name }}</h2>
            <div class="quote-company-detail">
              {{ companyInfo.address }} &nbsp;|&nbsp; 电话：{{ companyInfo.phone }}
              &nbsp;|&nbsp; {{ companyInfo.email }}
            </div>
          </div>
          <div class="quote-header-right">
            <div class="quote-badge">报 价 单</div>
            <div class="quote-no">编号：QT{{ new Date().getFullYear() }}{{ String(new Date().getMonth()+1).padStart(2,'0') }}{{ String(new Date().getDate()).padStart(2,'0') }}-001</div>
          </div>
        </div>

        <!-- 客户信息栏 -->
        <div class="quote-meta-bar">
          <div class="meta-item"><span class="meta-label">客户：</span><strong>{{ form.customerName || '-' }}</strong></div>
          <div class="meta-item"><span class="meta-label">项目：</span><strong>{{ form.title || '-' }}</strong></div>
          <div class="meta-item"><span class="meta-label">日期：</span>{{ todayStr }}</div>
          <div class="meta-item"><span class="meta-label">有效期至：</span>
            <input type="date" v-model="quoteValidUntil" class="date-small-input" />
          </div>
        </div>

        <!-- 物料报价明细 -->
        <div class="quote-section-title">一、物料明细</div>
        <table class="data-table quote-table">
          <thead>
            <tr>
              <th style="width:45px;text-align:center;">序号</th>
              <th style="width:22%;">物料名称</th>
              <th style="width:16%;">规格</th>
              <th style="width:50px;text-align:center;">单位</th>
              <th style="width:65px;text-align:center;">数量</th>
              <th style="width:85px;text-align:right;">单价(¥)</th>
              <th style="width:95px;text-align:right;">金额(¥)</th>
              <th style="width:140px;text-align:center;">备注</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(s, idx) in selectedMaterials" :key="idx">
              <td style="text-align:center;">{{ idx + 1 }}</td>
              <td>{{ s.materialName }}</td>
              <td style="font-size:12px;">{{ s.spec || '-' }}</td>
              <td style="text-align:center;">{{ s.unit || '个' }}</td>
              <td style="text-align:center;">{{ s.quantity }}</td>
              <td style="text-align:right;">{{ formatMoney(s.unitPrice) }}</td>
              <td style="text-align:right;font-weight:500;">{{ formatMoney(s.amount) }}</td>
              <td style="text-align:center;"><input type="text" v-model="s.quoteRemark" class="remark-inline" placeholder="-"/></td>
            </tr>
          </tbody>
        </table>

        <!-- 金额汇总区 -->
        <div class="quote-totals-area">
          <div class="totals-grid">
            <div class="total-line">
              <span class="tl-label">物料合计</span>
              <span class="tl-value">¥{{ formatMoney(materialTotal) }}</span>
            </div>
            <div class="total-line discount-line">
              <span class="tl-label">
                折扣率：
                <input type="number" v-model.number="quoteDiscount" min="0" max="100" step="1"
                  class="pct-input" @change="recalcQuote" /> %
              </span>
              <span class="tl-value discount-val">-¥{{ formatMoney(discountAmount) }}</span>
            </div>
            <div class="total-line tax-line">
              <span class="tl-label">
                税率：
                <input type="number" v-model.number="quoteTaxRate" min="0" max="30" step="0.1"
                  class="pct-input" @change="recalcQuote" /> %
              </span>
              <span class="tl-value tax-val">+¥{{ formatMoney(taxAmount) }}</span>
            </div>
            <div class="total-line grand-total-line">
              <span class="tl-label">报价总计（含税）</span>
              <span class="tl-value grand-total">¥{{ formatMoney(quoteGrandTotal) }}</span>
            </div>
          </div>
        </div>

        <!-- 备注与条款 -->
        <div class="quote-section-title">二、备注与说明</div>
        <textarea v-model="quoteRemark" class="quote-remark-textarea" rows="3"
          placeholder="报价备注、付款方式、交货周期等说明..."></textarea>
        <div class="quote-clause">
          本报价单有效期至 <strong>{{ quoteValidUntil || '(未设置)' }}</strong>，
          超过有效期需重新确认。最终价格以双方确认为准。
        </div>

        <!-- 操作按钮 -->
        <div class="quote-actions">
          <button class="btn btn-default" @click="showQuotePreview = false">← 返回编辑</button>
          <button class="btn btn-success" @click="exportQuoteToExcel">📥 导出Excel报价单</button>
          <button class="btn btn-primary" @click="confirmQuoteAndSubmit">✅ 确认并提交订单</button>
        </div>

        <!-- 关闭按钮 -->
        <button class="quote-close-btn" @click="showQuotePreview = false">✕</button>
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
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

const router = useRouter()
const authStore = useAuthStore()

// 成本列仅管理员/财务可见
const canViewCost = computed(() => {
  const roles = authStore.userInfo?.roles || []
  return roles.includes('SUPER_ADMIN') || roles.includes('FINANCE') || roles.includes('ADMIN')
})

// ========== 状态 ==========
const submitting = ref(false)
const materialsLoading = ref(true)

// ========== 公司信息（后续可改为从系统配置读取）==========
const companyInfo = reactive({
  name: 'XX广告印刷有限公司',
  address: 'XX市XX区XX路XX号XX大厦XX楼',
  phone: '0755-XXXXXXXX',
  fax: '0755-XXXXXXXX',
  email: 'sales@example.com',
  bankName: '中国银行XX支行',
  bankAccount: 'XXXXXXXXXXXXXXX',
  taxNo: '91XXXXXXXXXXXXX',
})

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
    const res: any = await request.get('/system/users/designer-list')
    designerList.value = (res.data || []).map((u: any) => ({
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
  return selectedMaterials.value.some(s => s.materialId === id && !s.isCustom)
}

// 两列布局：将物料列表一分为二
const materialColumns = computed(() => {
  const list = filteredMaterialList.value
  const mid = Math.ceil(list.length / 2)
  return [list.slice(0, mid), list.slice(mid)]
})

function getSelected(id: number): any {
  return selectedMaterials.value.find(s => s.materialId === id && !s.isCustom)
}

/** 从预设物料列表选用 */
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
    // ★ 预设物料自动带入成本价（仅管理员可见）
    unitCost: mat.cost_price != null ? Number(mat.cost_price) : null,
    isCustom: false,
    quoteRemark: '',
  })
}

function removeMaterial(id: number) {
  selectedMaterials.value = selectedMaterials.value.filter(s => !(s.materialId === id && !s.isCustom))
}

/** ★ 新增：按索引移除（支持自定义物料） */
function removeMaterialByIndex(idx: number) {
  selectedMaterials.value.splice(idx, 1)
}

/** ★ 新增：手动添加自定义物料 */
function addCustomMaterial() {
  // 生成一个负数临时ID避免与真实materialId冲突
  const tempId = -(Date.now())
  selectedMaterials.value.push({
    materialId: tempId,
    materialName: '',           // 用户自行填写
    spec: '',
    unit: '项',
    quantity: 1,
    unitPrice: 0,
    amount: 0,
    isCustom: true,             // 标记为自定义物料
    quoteRemark: '',
  })
  // 滚动到已选区域底部
  setTimeout(() => {
    const el = document.querySelector('.selected-materials-card')
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, 100)
}

function recalcLine(id: number) {
  const item = getSelected(id)
  if (!item) return
  const qty = Math.max(1, Number(item.quantity) || 0)
  const price = Math.max(0, Number(item.unitPrice) || 0)
  item.amount = Number((qty * price).toFixed(2))
}

/** ★ 新增：重算已选物料某一行（用于已选区域的内联编辑） */
function recalcSelectedItem(idx: number) {
  const item = selectedMaterials.value[idx]
  if (!item) return
  const qty = Math.max(0, Number(item.quantity) || 0)
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

const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
})

// ========== 报价单相关 ==========
const showQuotePreview = ref(false)
const quoteDiscount = ref(100)
const quoteTaxRate = ref(0)
const quoteValidUntil = ref('')
const quoteRemark = ref('')

const discountAmount = computed(() => {
  return Number((materialTotal.value * (1 - quoteDiscount.value / 100)).toFixed(2))
})
const afterDiscount = computed(() => {
  return Math.max(0, materialTotal.value - discountAmount.value)
})
const taxAmount = computed(() => {
  return Number((afterDiscount.value * (quoteTaxRate.value / 100)).toFixed(2))
})
const quoteGrandTotal = computed(() => {
  return Number((afterDiscount.value + taxAmount.value).toFixed(2))
})

function recalcQuote() {
  // 响应式计算自动更新，此函数保留供事件绑定使用
}

// ========== 导出Excel报价单 ==========
function exportQuoteToExcel() {
  // 构建工作簿数据
  const wb = XLSX.utils.book_new()

  // ---- Sheet 1: 报价单正文 ----
  const quoteData: any[][] = []

  // 公司名称头部
  quoteData.push([companyInfo.name])
  quoteData.push([companyInfo.address])
  quoteData.push([`电话: ${companyInfo.phone}  |  传真: ${companyInfo.fax}  |  邮箱: ${companyInfo.email}`])
  quoteData.push([])

  // 报价单信息
  quoteData.push(['报  价  单', '', '', '', `日期: ${todayStr.value}`])
  quoteData.push([`客户: ${form.customerName || '-'}`, '', '', '', `项目: ${form.title || '-'}`])
  quoteData.push([`有效期至: ${quoteValidUntil.value || '(未设置)'}`])
  quoteData.push([])

  // 物料明细表头
  quoteData.push(['序号', '物料名称', '规格', '单位', '数量', '单价(¥)', '金额(¥)', '备注'])

  // 物料数据
  selectedMaterials.value.forEach((s, idx) => {
    quoteData.push([
      idx + 1,
      s.materialName,
      s.spec || '-',
      s.unit || '个',
      s.quantity,
      s.unitPrice,
      s.amount,
      s.quoteRemark || '',
    ])
  })

  // 空行分隔
  quoteData.push([])

  // 金额汇总
  quoteData.push(['金额汇总', '', '', '', '', '', '', ''])
  quoteData.push(['', '物料合计', '', '', '', '', `¥${formatMoney(materialTotal.value)}`, ''])
  quoteData.push(['', `折扣 (${quoteDiscount.value}%)`, '', '', '', '', `-¥${formatMoney(discountAmount.value)}`, ''])
  if (quoteTaxRate.value > 0) {
    quoteData.push(['', `税率 (${quoteTaxRate.value}%)`, '', '', '', '', `+¥${formatMoney(taxAmount.value)}`, ''])
  }
  quoteData.push(['', '报价总计(含税)', '', '', '', '', `¥${formatMoney(quoteGrandTotal.value)}`, ''])
  quoteData.push([])

  // 备注
  if (quoteRemark.value.trim()) {
    quoteData.push(['备注:', quoteRemark.value])
    quoteData.push([])
  }

  // 条款
  quoteData.push(['本报价单仅供参考，最终价格以双方书面确认为准。'])
  quoteData.push([])

  // 公司账户信息
  quoteData.push(['--- 开户信息 ---'])
  quoteData.push(['开户银行:', companyInfo.bankName])
  quoteData.push(['银行账号:', companyInfo.bankAccount])
  quoteData.push(['税号:', companyInfo.taxNo])

  const ws1 = XLSX.utils.aoa_to_sheet(quoteData)
  // 设置列宽
  ws1['!cols'] = [
    { wch: 6 },   // 序号
    { wch: 24 },  // 名称
    { wch: 18 },  // 规格
    { wch: 8 },   // 单位
    { wch: 8 },   // 数量
    { wch: 12 },  // 单价
    { wch: 14 },  // 金额
    { wch: 20 },  // 备注
  ]
  // 合并公司名单元格
  ws1['!merges'] = [
    { s: { r: 0, c: 0 }, e: { r: 0, c: 7 } },
    { s: { r: 4, c: 0 }, e: { r: 4, c: 3 } },
    { s: { r: 13, c: 0 }, e: { r: 13, c: 6 } },   // 金额汇总标题
    { s: { r: 17, c: 0 }, e: { r: 17, c: 7 } },   // 备注
    { s: { r: 20, c: 0 }, e: { r: 20, c: 7 } },   // 开户信息标题
  ]

  XLSX.utils.book_append_sheet(wb, ws1, '报价单')

  // ---- Sheet 2: 公司信息 ----
  const companyData = [
    ['公司基础信息'],
    [''],
    ['项目', '内容'],
    ['公司全称', companyInfo.name],
    ['地址', companyInfo.address],
    ['电话', companyInfo.phone],
    ['传真', companyInfo.fax],
    ['邮箱', companyInfo.email],
    ['开户银行', companyInfo.bankName],
    ['银行账号', companyInfo.bankAccount],
    ['税号', companyInfo.taxNo],
  ]
  const ws2 = XLSX.utils.aoa_to_sheet(companyData)
  ws2['!cols'] = [{ wch: 12 }, { wch: 40 }]
  XLSX.utils.book_append_sheet(wb, ws2, '公司信息')

  // 导出文件
  const fileName = `报价单_${form.customerName || '客户'}_${todayStr.value}.xlsx`
  const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
  const blob = new Blob([wbout], { type: 'application/octet-stream' })
  saveAs(blob, fileName)

  ElMessage.success(`✅ 报价单已导出: ${fileName}`)
}

// ========== 从报价单确认并提交 ==========
async function confirmQuoteAndSubmit() {
  showQuotePreview.value = false
  // 将报价折扣同步到订单优惠
  if (quoteDiscount.value < 100) {
    form.discountAmount = discountAmount.value
  }
  await submitOrder()
}

// ========== 提交 ==========
async function submitOrder() {
  // 基本验证
  if (!form.title.trim()) { ElMessage.warning('请输入订单标题'); return }
  if (!form.orderType) { ElMessage.warning('请选择订单类型'); return }
  if (!form.customerId) { ElMessage.warning('请选择客户'); return }
  if (selectedMaterials.value.length === 0) { ElMessage.warning('请至少选择一件物料'); return }

  // 校验自定义物料的必填项
  for (const s of selectedMaterials.value) {
    if (s.isCustom && !s.materialName.trim()) {
      ElMessage.warning('自定义物料请填写物料名称')
      return
    }
  }

  await ElMessageBox.confirm(
    `即将创建订单「${form.title}」\n` +
    `客户：${form.customerName}\n` +
    `已选 ${selectedMaterials.value.length} 件物料（含 ${selectedMaterials.value.filter((s: any) => s.isCustom).length} 个自定义物料），总金额 ¥${formatMoney(orderTotalAmount)}\n\n` +
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
      materials: selectedMaterials.value.map((s: any) => ({
        materialName: s.materialName,
        spec: s.spec,
        unit: s.unit,
        quantity: s.quantity,
        unitPrice: s.unitPrice,
        amount: s.amount,
        // ★ 成本价（仅管理员填写，设计师创建时为 null）
        unitCost: canViewCost.value ? (s.unitCost || null) : null,
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

  // 设置默认报价有效期（30天后）
  const d = new Date()
  d.setDate(d.getDate() + 30)
  quoteValidUntil.value = d.toISOString().slice(0, 10)
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
  flex-wrap: wrap;
  gap: 8px;
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
.btn-custom-add {
  background: linear-gradient(135deg, #67c23a, #85ce61);
  color: #fff;
  border: none;
  font-size: 12px;
  padding: 4px 12px;
  white-space: nowrap;
}
.btn-custom-add:hover {
  opacity: .9;
  transform: translateY(-1px);
}
.btn-success {
  background: linear-gradient(135deg, #67c23a, #85ce61);
  color: #fff;
}
.btn-success:hover:not(:disabled) {
  opacity: .9;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(103,194,58,.35);
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
  flex-wrap: wrap;
  gap: 8px;
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

/* ============================================================
   ★ 已选物料清单样式
   ============================================================ */
.selected-materials-card {
  margin-top: 20px;
  border-color: #e6a23c33;
}
.selected-table {
  font-size: 13px;
}
.selected-table thead th {
  background: #fdf6ec;
  color: #e6a23c;
  font-size: 12px;
}
.selected-table tfoot td {
  background: #fdf6ec;
  font-size: 14px;
  padding: 10px 8px;
  border-top: 2px solid #e6a23c;
}
.total-row td {
  border-top: 2px solid #e6a23c !important;
}

/* 自定义物料行的特殊样式 */
.custom-row {
  background: linear-gradient(135deg, #f0f9eb, #fff) !important;
}
.custom-row:hover {
  background: linear-gradient(135deg, #e1f3d8, #fff) !important;
}

/* 内联编辑输入框 */
.inline-edit-input {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 3px 6px;
  font-size: 12px;
  outline: none;
  transition: all .2s;
  background: #fff;
  color: var(--text1);
}
.inline-edit-input:focus {
  border-color: #67c23a;
  box-shadow: 0 0 0 2px rgba(103,194,58,.15);
}
.center-text {
  text-align: center;
}
.qty-inline {
  width:55px; text-align:center; border:1px solid #dcdfe6;border-radius:4px;padding:2px 4px;font-size:12px;outline:none;
}
.qty-inline:focus{ border-color:var(--primary);box-shadow:0 0 0 2px rgba(var(--primary-rgb),.15); }
.price-inline {
  width:65px; text-align:right; border:1px solid #dcdfe6;border-radius:4px;padding:2px 4px;font-size:12px;outline:none;color:#e6a23c;font-weight:500;
}
.price-inline:focus{ border-color:#e6a23c;box-shadow:0 0 0 2px rgba(230,162,60,.15); }
.cost-inline {
  width:70px; text-align:right; border:1px solid #dcdfe6;border-radius:4px;padding:2px 4px;font-size:12px;outline:none;color:#909399;background:#f5f7fa;
}
.cost-inline:focus{ border-color:#909399;box-shadow:0 0 0 2px rgba(144,147,153,.1); }
.cost-inline:disabled{ cursor:default; opacity:0.8; }
.remark-inline {
  width:120px; border:1px solid #dcdfe6;border-radius:4px;padding:2px 4px;font-size:11px;outline:none;text-align:center;
}

/* 类型标签 */
.tag-custom {
  display:inline-block;padding:1px 6px;font-size:10px;background:#f0f9eb;color:#67c23a;border-radius:3px;
}
.tag-preset {
  display:inline-block;padding:1px 6px;font-size:10px;background:#ecf5ff;color:#409eff;border-radius:3px;
}

/* ============================================================
   ★ 报价单弹窗样式
   ============================================================ */
.quote-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,.5);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 30px 20px;
  overflow-y: auto;
}
.quote-dialog {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 80px rgba(0,0,0,.25);
  width: 900px;
  max-width: 100%;
  position: relative;
  animation: quoteIn .3s ease-out;
  margin: auto;
}
@keyframes quoteIn {
  from { opacity: 0; transform: scale(.96) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.quote-close-btn {
  position: absolute;
  top: 14px; right: 14px;
  width: 32px; height: 32px;
  border-radius: 50%;
  border: none;
  background: #f5f7fa;
  color: #909399;
  font-size: 16px;
  cursor: pointer;
  transition: all .2s;
  display: flex; align-items: center; justify-content: center;
}
.quote-close-btn:hover {
  background: #f56c6c; color: #fff;
}

/* 报价单头部 */
.quote-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 28px 28px 0;
}
.quote-company-name {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 800;
  color: #1a1a2e;
}
.quote-company-detail {
  font-size: 12px;
  color: #909399;
}
.quote-header-right {
  text-align: right;
}
.quote-badge {
  display: inline-block;
  background: linear-gradient(135deg, var(--primary), #3b82f6);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  padding: 8px 24px;
  border-radius: 8px;
  letter-spacing: 4px;
}
.quote-no {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

/* 元信息栏 */
.quote-meta-bar {
  display: flex;
  gap: 24px;
  padding: 14px 28px;
  background: #f8fafc;
  border-bottom: 1px solid #eee;
  font-size: 13px;
  flex-wrap: wrap;
}
.meta-item {
  display: flex; align-items: center; gap: 4px;
}
.meta-label { color: #909399; }
.date-small-input {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 12px;
  outline: none;
}
.date-small-input:focus { border-color: var(--primary); }

/* 区块标题 */
.quote-section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text1);
  padding: 16px 28px 10px;
  border-bottom: 1px solid #f0f0f0;
}

/* 报价表格 */
.quote-table {
  margin: 0 28px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}
.quote-table thead th {
  background: linear-gradient(135deg, #1a1a2e, #2d3748);
  color: #fff !important;
  font-size: 12px;
}
.quote-table tbody tr:nth-child(even) {
  background: #fafbfc;
}
.quote-table tbody td {
  font-size: 12px;
}

/* 金额汇总区 */
.quote-totals-area {
  margin: 0 28px 16px;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px 20px;
}
.totals-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.total-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  padding: 4px 0;
}
.tl-label {
  color: #606266;
  display: flex;
  align-items: center;
  gap: 6px;
}
.tl-value {
  font-weight: 600;
  color: #303133;
  min-width: 120px;
  text-align: right;
}
.discount-val { color: #67c23a !important; }
.tax-val { color: #e6a23c !important; }
.grand-total-line {
  margin-top: 6px;
  padding-top: 10px;
  border-top: 2px dashed #dcdfe6;
  font-size: 16px !important;
}
.grand-total {
  color: var(--primary) !important;
  font-size: 22px !important;
  font-weight: 800 !important;
}
.pct-input {
  width:55px; border:1px solid #dcdfe6;border-radius:4px;padding:2px 4px;font-size:13px;outline:none;text-align:center;
}
.pct-input:focus { border-color: var(--primary); }

/* 备注 */
.quote-remark-textarea {
  margin: 0 28px 12px;
  width: calc(100% - 56px);
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 13px;
  outline: none;
  resize: vertical;
  font-family: inherit;
}
.quote-remark-textarea:focus { border-color: var(--primary); }
.quote-clause {
  margin: 0 28px 16px;
  padding: 10px 14px;
  background: #fffbf0;
  border: 1px solid #faecd8;
  border-radius: 6px;
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.6;
}

/* 操作按钮 */
.quote-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 28px 24px;
  border-top: 1px solid #f0f0f0;
}
.quote-actions .btn {
  padding: 10px 22px;
  font-size: 14px;
}
</style>
