<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item" @click="router.push('/finance/quote')">报价管理</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">新建报价</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">📝 新建报价单</h1>
      <div class="page-actions">
        <button class="btn btn-default" @click="router.back()">← 返回列表</button>
        <button v-if="selectedMaterials.length > 0" class="btn btn-outline-primary" @click="showPreviewDialog = true">
          👁 预览报价单
        </button>
        <button class="btn btn-danger" @click="handleGiveUp">
          ❌ 放弃报价
        </button>
        <button class="btn btn-primary" :disabled="submitting || selectedMaterials.length === 0"
          @click="handleSaveQuote">
          💾 保存报价
        </button>
      </div>
    </div>

    <!-- ========== 报价基本信息 ========== -->
    <div class="card order-info-card">
      <div class="card-title-bar">
        <span>📋 报价信息</span>
        <span style="color:#909399;font-size:12px;">* 为必填项</span>
      </div>

      <div class="form-row">
        <!-- 公司选择 -->
        <div class="form-group" style="flex:1;">
          <label><span class="required">*</span> 报价公司</label>
          <select v-model="form.companyId" class="form-control" @change="onCompanyChange">
            <option value="">请选择公司</option>
            <option v-for="c in companyList" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <div class="form-group" style="flex:2;">
          <label><span class="required">*</span> 报价标题 / 项目名称</label>
          <input type="text" v-model="form.projectName" class="form-control"
            placeholder="如：XX公司宣传物料报价" maxlength="200" />
        </div>
      </div>

      <div class="form-row">
        <div class="form-group" style="flex:2;">
          <label><span class="required">*</span> 客户</label>
          <select v-model="form.customerId" class="form-control" @change="onCustomerChange">
            <option value="">请选择客户</option>
            <option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name || c.customerName }}</option>
          </select>
        </div>
        <div class="form-group" style="flex:1;">
          <label>联系人</label>
          <input type="text" v-model="form.contactPerson" class="form-control" placeholder="联系人姓名" />
        </div>
        <div class="form-group" style="flex:1;">
          <label>联系电话</label>
          <input type="text" v-model="form.contactPhone" class="form-control" placeholder="手机或座机" />
        </div>
      </div>

      <div class="form-row">
        <div class="form-group" style="flex:1;">
          <label>折扣率 (%)</label>
          <input type="number" v-model.number="form.discount" class="form-control"
            min="0" max="100" step="1" placeholder="100 = 不打折" />
        </div>
        <div class="form-group" style="flex:1;">
          <label>税率 (%)</label>
          <input type="number" v-model.number="form.taxRate" class="form-control"
            min="0" max="30" step="0.1" placeholder="0 = 不含税" />
        </div>
        <div class="form-group" style="flex:1;">
          <label>有效期至</label>
          <input type="date" v-model="form.validUntil" class="form-control" />
        </div>
      </div>

      <div class="form-row">
        <div class="form-group" style="flex:1;">
          <label>备注</label>
          <textarea v-model="form.remark" class="form-control" rows="2"
            placeholder="付款方式、交货周期、特殊要求等..."></textarea>
        </div>
      </div>

      <!-- 当前公司信息预览 -->
      <div v-if="currentCompany.name" class="company-preview-bar">
        <span class="cp-label">🏢 出单公司：</span>
        <strong>{{ currentCompany.name }}</strong>
        <span class="cp-sep">|</span>
        <span>{{ currentCompany.address }}</span>
        <span class="cp-sep">|</span>
        <span>☎ {{ currentCompany.phone }}</span>
        <span v-if="currentCompany.taxNo" class="cp-sep">|</span>
        <span v-if="currentCompany.taxNo">税号：{{ currentCompany.taxNo }}</span>
      </div>
    </div>

    <!-- ========== 物料选择区域 ========== -->
    <div class="card materials-card">
      <div class="card-title-bar">
        <span>📦 选择物料</span>
        <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap;">
          <input type="text" v-model="materialKeyword" class="form-control"
            placeholder="搜索物料名称/编码..." style="width:180px;padding:4px 8px;"
            @input="filterMaterials" />
          <select v-model="materialCategoryFilter" class="form-control"
            style="width:130px;padding:4px 8px;" @change="filterMaterials">
            <option value="">全部分类</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
          <button class="btn btn-sm btn-custom-add" @click="addCustomMaterial" title="添加不在预设列表中的临时物料">
            ✏️ 手动物料
          </button>
          <span style="color:#909399;font-size:12px;">
            已选 {{ selectedMaterials.length }} 件 | 合计 ¥{{ formatMoney(materialTotal) }}
          </span>
        </div>
      </div>

      <!-- 物料表格两列并排 -->
      <div class="materials-two-col" v-if="!materialsLoading">
        <template v-if="filteredMaterialList.length > 0">
          <div class="materials-col" v-for="(col, colIdx) in materialColumns" :key="colIdx">
            <table class="data-table" style="margin:0;">
              <thead>
                <tr>
                  <th style="width:28px;text-align:center;">#</th>
                  <th style="width:30%;">物料名称</th>
                  <th style="width:13%;">分类</th>
                  <th style="width:55px;">规格</th>
                  <th style="width:38px;">单位</th>
                  <th style="width:58px;text-align:right;">零售价</th>
                  <th style="width:48px;text-align:center;">数量</th>
                  <th style="width:62px;text-align:right;">单价</th>
                  <th style="width:62px;text-align:right;">小计</th>
                  <th style="width:38px;text-align:center;">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(m, idx) in col" :key="m.id"
                  :class="{ 'row-selected': isMaterialSelected(m.id) }">
                  <td style="text-align:center;color:#c0c4cc;">{{ colIdx * Math.ceil(filteredMaterialList.length / 2) + idx + 1 }}</td>
                  <td>
                    <strong>{{ m.name }}</strong>
                    <div v-if="m.code" style="font-size:11px;color:#909399;">编码：{{ m.code }}</div>
                  </td>
                  <td><span class="tag" style="font-size:11px;">{{ m.categoryName }}</span></td>
                  <td style="font-size:12px;">{{ m.spec || '-' }}</td>
                  <td style="text-align:center;font-size:12px;">{{ m.unit || '个' }}</td>
                  <td style="text-align:right;color:#e6a23c;font-size:12px;">¥{{ formatMoney(m.price) }}</td>
                  <td style="text-align:center;">
                    <input v-if="isMaterialSelected(m.id)" type="number"
                      v-model.number="getSelected(m.id).quantity"
                      class="qty-input" min="1" step="1" @change="recalcLine(m.id)"
                      style="width:44px;text-align:center;padding:2px 3px;font-size:12px;" />
                    <span v-else style="color:#c0c4cc;">-</span>
                  </td>
                  <td style="text-align:right;">
                    <input v-if="isMaterialSelected(m.id)" type="number"
                      v-model.number="getSelected(m.id).unitPrice"
                      class="price-input" min="0" step="0.01" @change="recalcLine(m.id)"
                      style="width:54px;text-align:right;padding:2px 3px;font-size:12px;" />
                    <span v-else style="color:#c0c4cc;">-</span>
                  </td>
                  <td style="text-align:right;font-weight:600;color:#e6a23c;font-size:12px;">
                    <span v-if="isMaterialSelected(m.id)">¥{{ formatMoney(getSelected(m.id).amount) }}</span>
                    <span v-else>-</span>
                  </td>
                  <td style="text-align:center;">
                    <button v-if="isMaterialSelected(m.id)" class="action-btn delete-sm" title="移除" @click="removeMaterial(m.id)">✕</button>
                    <button v-else class="btn-sm btn-primary-outline" title="选用此物料" @click="addMaterial(m)">+</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
        <div v-else class="materials-empty">暂无匹配物料（可点击「✏️ 手动物料」手动添加）</div>
      </div>
      <div v-else class="loading-placeholder"><p>正在加载物料数据...</p></div>
    </div>

    <!-- ========== 已选物料清单 ========== -->
    <div class="card selected-materials-card" v-if="selectedMaterials.length > 0">
      <div class="card-title-bar">
        <span>🛒 已选物料清单</span>
        <span style="color:#67c23a;font-size:12px;">点击字段可直接编辑 · 🏷️ 标记为手动物料</span>
      </div>
      <table class="data-table selected-table">
        <thead>
          <tr>
            <th style="width:38px;text-align:center;">#</th>
            <th style="width:24%;">物料名称</th>
            <th style="width:14%;">规格</th>
            <th style="width:58px;text-align:center;">单位</th>
            <th style="width:68px;text-align:center;">数量</th>
            <th style="width:88px;text-align:right;">单价(¥)</th>
            <th style="width:98px;text-align:right;">小计(¥)</th>
            <th style="width:56px;text-align:center;">类型</th>
            <th style="width:100px;text-align:center;">备注</th>
            <th style="width:58px;text-align:center;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(s, idx) in selectedMaterials" :key="s.materialId || idx" :class="{ 'custom-row': s.isCustom }">
            <td style="text-align:center;color:#909399;">{{ idx + 1 }}</td>
            <td v-if="s.isCustom">
              <input type="text" v-model="s.materialName" class="inline-edit-input" placeholder="输入物料名称" style="width:100%;" />
            </td>
            <td v-else><strong>{{ s.materialName }}</strong></td>
            <td v-if="s.isCustom">
              <input type="text" v-model="s.spec" class="inline-edit-input" placeholder="规格描述" style="width:100%;" />
            </td>
            <td v-else style="font-size:12px;">{{ s.spec || '-' }}</td>
            <td v-if="s.isCustom">
              <input type="text" v-model="s.unit" class="inline-edit-input center-text" placeholder="单位" style="width:48px;" />
            </td>
            <td v-else style="text-align:center;">{{ s.unit || '个' }}</td>
            <td style="text-align:center;">
              <input type="number" v-model.number="s.quantity" class="qty-inline" min="0.01" step="1" @change="recalcSelectedItem(idx)" />
            </td>
            <td style="text-align:right;">
              <input type="number" v-model.number="s.unitPrice" class="price-inline" min="0" step="0.01" @change="recalcSelectedItem(idx)" />
            </td>
            <td style="text-align:right;font-weight:600;color:#e6a23c;">¥{{ formatMoney(s.amount) }}</td>
            <td style="text-align:center;">
              <span v-if="s.isCustom" class="tag-custom">🏷️ 手动</span>
              <span v-else class="tag-preset">📦 预设</span>
            </td>
            <td style="text-align:center;">
              <input type="text" v-model="s.quoteRemark" class="remark-inline" placeholder="-" />
            </td>
            <td style="text-align:center;">
              <button class="action-btn delete-sm" title="移除此物料" @click="removeMaterialByIndex(idx)">✕</button>
            </td>
          </tr>
        </tbody>
        <tfoot>
          <tr class="total-row">
            <td colspan="6" style="text-align:right;font-weight:600;">物料合计：</td>
            <td colspan="4" style="text-align:right;font-weight:700;color:#e6a23c;font-size:15px;">
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
          <span>已选：</span><strong>{{ selectedMaterials.length }} 件</strong>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-item">
          <span>物料合计：</span>
          <strong style="color:#e6a23c;font-size:17px;">¥{{ formatMoney(materialTotal) }}</strong>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-item">
          <span>折后金额：</span>
          <strong style="color:#67c23a;font-size:17px;">¥{{ formatMoney(afterDiscount) }}</strong>
        </div>
        <div v-if="form.taxRate > 0" class="summary-divider"></div>
        <div v-if="form.taxRate > 0" class="summary-item">
          <span>含税总价：</span>
          <strong style="color:var(--primary);font-size:17px;">¥{{ formatMoney(grandTotal) }}</strong>
        </div>
        <div class="summary-actions">
          <button class="btn btn-danger" @click="handleGiveUp">❌ 放弃报价</button>
          <button class="btn btn-success" @click="showPreviewDialog = true">📋 预览 / 导出</button>
          <button class="btn btn-primary submit-btn" :disabled="submitting" @click="handleSaveQuote">
            {{ submitting ? '保存中...' : '💾 保存报价' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ==================== 报价单预览弹窗 ==================== -->
    <div class="quote-overlay" v-if="showPreviewDialog" @click.self="showPreviewDialog = false">
      <div class="quote-dialog">
        <!-- 公司头部 -->
        <div class="quote-header">
          <div class="quote-header-left">
            <h2 class="quote-company-name">{{ currentCompany.name || 'XX广告印刷有限公司' }}</h2>
            <div class="quote-company-detail">
              {{ currentCompany.address }}
              <template v-if="currentCompany.phone">&nbsp;|&nbsp; 电话：{{ currentCompany.phone }}</template>
              <template v-if="currentCompany.email">&nbsp;|&nbsp; {{ currentCompany.email }}</template>
            </div>
          </div>
          <div class="quote-header-right">
            <div class="quote-badge">报 价 单</div>
            <div class="quote-no">编号：QT{{ quoteNoStr }}</div>
          </div>
        </div>

        <!-- 客户元信息 -->
        <div class="quote-meta-bar">
          <div class="meta-item"><span class="meta-label">客户：</span><strong>{{ form.customerName || '-' }}</strong></div>
          <div class="meta-item"><span class="meta-label">项目：</span><strong>{{ form.projectName || '-' }}</strong></div>
          <div class="meta-item"><span class="meta-label">联系人：</span>{{ form.contactPerson || '-' }}</div>
          <div class="meta-item"><span class="meta-label">日期：</span>{{ todayStr }}</div>
          <div class="meta-item"><span class="meta-label">有效期至：</span>
            <input type="date" v-model="form.validUntil" class="date-small-input" />
          </div>
        </div>

        <!-- 物料明细 -->
        <div class="quote-section-title">一、物料明细</div>
        <table class="data-table quote-table">
          <thead>
            <tr>
              <th style="width:42px;text-align:center;">序号</th>
              <th style="width:22%;">物料名称</th>
              <th style="width:15%;">规格</th>
              <th style="width:48px;text-align:center;">单位</th>
              <th style="width:62px;text-align:center;">数量</th>
              <th style="width:82px;text-align:right;">单价(¥)</th>
              <th style="width:92px;text-align:right;">金额(¥)</th>
              <th style="width:130px;text-align:center;">备注</th>
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
              <td style="text-align:center;">
                <input type="text" v-model="s.quoteRemark" class="remark-inline" placeholder="-" />
              </td>
            </tr>
          </tbody>
        </table>

        <!-- 金额汇总 -->
        <div class="quote-totals-area">
          <div class="totals-grid">
            <div class="total-line">
              <span class="tl-label">物料合计</span>
              <span class="tl-value">¥{{ formatMoney(materialTotal) }}</span>
            </div>
            <div class="total-line discount-line">
              <span class="tl-label">
                折扣率：
                <input type="number" v-model.number="form.discount" min="0" max="100" step="1" class="pct-input" /> %
              </span>
              <span class="tl-value discount-val">-¥{{ formatMoney(discountDeduct) }}</span>
            </div>
            <div class="total-line tax-line">
              <span class="tl-label">
                税率：
                <input type="number" v-model.number="form.taxRate" min="0" max="30" step="0.1" class="pct-input" /> %
              </span>
              <span class="tl-value tax-val">+¥{{ formatMoney(taxAmount) }}</span>
            </div>
            <div class="total-line grand-total-line">
              <span class="tl-label">报价总计（含税）</span>
              <span class="tl-value grand-total">¥{{ formatMoney(grandTotal) }}</span>
            </div>
          </div>
        </div>

        <!-- 备注 -->
        <div class="quote-section-title">二、备注与说明</div>
        <textarea v-model="form.remark" class="quote-remark-textarea" rows="3"
          placeholder="报价备注、付款方式、交货周期等说明..."></textarea>
        <div class="quote-clause">
          本报价单有效期至 <strong>{{ form.validUntil || '（未设置）' }}</strong>，超过有效期需重新确认。最终价格以双方确认为准。
          <template v-if="currentCompany.taxNo">
            &nbsp;|&nbsp; 税号：{{ currentCompany.taxNo }}
          </template>
          <template v-if="currentCompany.bankName">
            &nbsp;|&nbsp; 开户行：{{ currentCompany.bankName }}（{{ currentCompany.bankAccount }}）
          </template>
        </div>

        <!-- 弹窗操作 -->
        <div class="quote-actions">
          <button class="btn btn-default" @click="showPreviewDialog = false">← 返回编辑</button>
          <button class="btn btn-success" @click="exportQuoteToExcel">📥 导出 Excel</button>
          <button class="btn btn-danger" @click="showPreviewDialog = false; handleGiveUp()">❌ 放弃报价</button>
          <button class="btn btn-primary" :disabled="submitting" @click="showPreviewDialog = false; handleSaveQuote()">
            💾 保存报价
          </button>
        </div>
        <button class="quote-close-btn" @click="showPreviewDialog = false">✕</button>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { materialApi } from '@/api/modules/material'
import { customerApi } from '@/api/modules/customer'
import { financeApi } from '@/api/modules/finance'
import { systemApi } from '@/api/modules/system'
import request from '@/api/request'
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

const router = useRouter()
const authStore = useAuthStore()

// ===== 状态 =====
const submitting = ref(false)
const materialsLoading = ref(true)
const showPreviewDialog = ref(false)

// ===== 公司列表 =====
const companyList = ref<any[]>([])
const currentCompany = reactive({
  id: null as number | null,
  name: '',
  address: '',
  phone: '',
  fax: '',
  email: '',
  bankName: '',
  bankAccount: '',
  taxNo: '',
})

async function loadCompanies() {
  try {
    const res: any = await systemApi.getCompanyList()
    // 映射 company_name → name 供模板使用
    companyList.value = (res.data || res || []).map((c: any) => ({
      ...c,
      name: c.companyName || c.name,
    }))
    // 默认选第一个
    if (companyList.value.length > 0 && !form.companyId) {
      form.companyId = companyList.value[0].id
      onCompanyChange()
    }
  } catch {
    ElMessage.warning('加载公司列表失败，请手动填写公司信息')
  }
}

function onCompanyChange() {
  const c = companyList.value.find((x: any) => x.id === form.companyId)
  if (c) {
    Object.assign(currentCompany, {
      id: c.id,
      name: c.name || '',
      address: c.address || '',
      phone: c.phone || '',
      fax: c.fax || '',
      email: c.email || '',
      bankName: c.bankName || c.bank_name || '',
      bankAccount: c.bankAccount || c.bank_account || '',
      taxNo: c.taxNo || c.tax_no || '',
    })
  }
}

// ===== 表单数据 =====
const form = reactive({
  companyId: null as number | null,
  projectName: '',
  customerId: null as number | null,
  customerName: '',
  contactPerson: '',
  contactPhone: '',
  discount: 100,
  taxRate: 0,
  validUntil: '',
  remark: '',
})

// ===== 客户列表 =====
const customers = ref<any[]>([])
async function loadCustomers() {
  try {
    const res: any = await customerApi.getList({ current: 1, size: 500 })
    customers.value = res.data?.records || res.data || []
  } catch (e: any) {
    ElMessage.error('加载客户列表失败：' + (e?.message || '请检查网络'))
  }
}

function onCustomerChange() {
  const c = customers.value.find((cc: any) => cc.id === form.customerId)
  if (c) {
    form.customerName = c.name || c.customerName || ''
    form.contactPerson = c.contactPerson || c.contact_person || ''
    form.contactPhone = c.phone || ''
  }
}

// ===== 物料 =====
const allMaterials = ref<any[]>([])
const filteredMaterialList = ref<any[]>([])
const materialKeyword = ref('')
const materialCategoryFilter = ref('')
const categories = ref<any[]>([])
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
  if (materialKeyword.value.trim()) {
    const kw = materialKeyword.value.toLowerCase()
    list = list.filter((m: any) => (m.name || '').toLowerCase().includes(kw) || (m.code || '').toLowerCase().includes(kw))
  }
  if (materialCategoryFilter.value) {
    list = list.filter((m: any) => Number(m.categoryId) === Number(materialCategoryFilter.value))
  }
  filteredMaterialList.value = list
}

const materialColumns = computed(() => {
  const list = filteredMaterialList.value
  const mid = Math.ceil(list.length / 2)
  return [list.slice(0, mid), list.slice(mid)]
})

function isMaterialSelected(id: number) {
  return selectedMaterials.value.some(s => s.materialId === id && !s.isCustom)
}
function getSelected(id: number) {
  return selectedMaterials.value.find(s => s.materialId === id && !s.isCustom)
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
    isCustom: false,
    quoteRemark: '',
  })
}
function removeMaterial(id: number) {
  selectedMaterials.value = selectedMaterials.value.filter(s => !(s.materialId === id && !s.isCustom))
}
function removeMaterialByIndex(idx: number) {
  selectedMaterials.value.splice(idx, 1)
}
function addCustomMaterial() {
  selectedMaterials.value.push({
    materialId: -(Date.now()),
    materialName: '',
    spec: '',
    unit: '项',
    quantity: 1,
    unitPrice: 0,
    amount: 0,
    isCustom: true,
    quoteRemark: '',
  })
  setTimeout(() => {
    const el = document.querySelector('.selected-materials-card')
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, 100)
}
function recalcLine(id: number) {
  const item = getSelected(id)
  if (!item) return
  item.amount = Number((Math.max(1, item.quantity) * Math.max(0, item.unitPrice)).toFixed(2))
}
function recalcSelectedItem(idx: number) {
  const item = selectedMaterials.value[idx]
  if (!item) return
  item.amount = Number((Math.max(0, item.quantity) * Math.max(0, item.unitPrice)).toFixed(2))
}

// ===== 汇总计算 =====
const materialTotal = computed(() => selectedMaterials.value.reduce((s, i) => s + (i.amount || 0), 0))
const discountDeduct = computed(() => Number((materialTotal.value * (1 - (form.discount ?? 100) / 100)).toFixed(2)))
const afterDiscount = computed(() => Math.max(0, materialTotal.value - discountDeduct.value))
const taxAmount = computed(() => Number((afterDiscount.value * ((form.taxRate ?? 0) / 100)).toFixed(2)))
const grandTotal = computed(() => Number((afterDiscount.value + taxAmount.value).toFixed(2)))

// ===== 工具 =====
function formatMoney(val: any) {
  return (Number(val) || 0).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}
const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})
const quoteNoStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}-${String(Math.floor(Math.random() * 900) + 100)}`
})

// ===== 放弃报价 =====
async function handleGiveUp() {
  try {
    await ElMessageBox.confirm(
      '确认放弃本次报价？所有已填写的内容将不会保存。',
      '放弃报价', {
        confirmButtonText: '确认放弃',
        cancelButtonText: '继续编辑',
        confirmButtonClass: 'el-button--danger',
        type: 'warning',
      }
    )
    router.push({ name: 'FinanceQuote' })
  } catch {
    // 用户点了"继续编辑"，不做任何事
  }
}

// ===== 保存报价（不自动下单）=====
async function handleSaveQuote() {
  if (!form.projectName.trim()) { ElMessage.warning('请填写报价标题/项目名称'); return }
  if (!form.customerId) { ElMessage.warning('请选择客户'); return }
  if (selectedMaterials.value.length === 0) { ElMessage.warning('请至少选择一件物料'); return }
  for (const s of selectedMaterials.value) {
    if (s.isCustom && !s.materialName.trim()) { ElMessage.warning('自定义物料请填写物料名称'); return }
  }

  try {
    await ElMessageBox.confirm(
      `确认保存报价单「${form.projectName}」？\n` +
      `客户：${form.customerName}\n` +
      `共 ${selectedMaterials.value.length} 件物料，报价总计 ¥${formatMoney(grandTotal.value)}\n\n` +
      `保存后可在报价管理中查看，客户确认后再转为订单。`,
      '确认保存报价', { confirmButtonText: '确认保存', cancelButtonText: '取消' }
    )
  } catch { return }

  submitting.value = true
  try {
    const quotePayload = {
      companyId: form.companyId,
      projectName: form.projectName,
      customerId: form.customerId,
      customerName: form.customerName,
      contactPerson: form.contactPerson,
      contactPhone: form.contactPhone,
      discount: form.discount,
      taxRate: form.taxRate,
      validUntil: form.validUntil,
      remark: form.remark,
      totalAmount: materialTotal.value,
      finalAmount: grandTotal.value,
      taxAmount: taxAmount.value,
      status: 'pending',
      details: selectedMaterials.value.map(s => ({
        materialId: s.isCustom ? null : s.materialId,
        materialName: s.materialName,
        spec: s.spec,
        unit: s.unit,
        quantity: s.quantity,
        unitPrice: s.unitPrice,
        amount: s.amount,
        remark: s.quoteRemark || '',
        isCustom: s.isCustom ? 1 : 0,
      })),
    }
    await financeApi.createQuote(quotePayload)
    ElMessage.success('✅ 报价单已保存！')
    router.push({ name: 'FinanceQuote' })
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败，请重试')
  } finally {
    submitting.value = false
  }
}

// ===== 导出 Excel =====
function exportQuoteToExcel() {
  const wb = XLSX.utils.book_new()
  const qd: any[][] = []

  // 公司头部
  qd.push([currentCompany.name || '报价公司'])
  qd.push([currentCompany.address || ''])
  qd.push([
    [currentCompany.phone && `电话: ${currentCompany.phone}`,
     currentCompany.fax && `传真: ${currentCompany.fax}`,
     currentCompany.email && `邮箱: ${currentCompany.email}`]
      .filter(Boolean).join('  |  ')
  ])
  qd.push([])

  // 报价信息
  qd.push(['报  价  单', '', '', '', `日期: ${todayStr.value}`])
  qd.push([`客户: ${form.customerName || '-'}`, '', '', '', `项目: ${form.projectName || '-'}`])
  qd.push([`联系人: ${form.contactPerson || '-'}`, '', '', '', `电话: ${form.contactPhone || '-'}`])
  qd.push([`有效期至: ${form.validUntil || '（未设置）'}`])
  qd.push([])

  // 明细表头
  qd.push(['序号', '物料名称', '规格', '单位', '数量', '单价(¥)', '金额(¥)', '备注'])

  // 明细
  selectedMaterials.value.forEach((s, idx) => {
    qd.push([idx + 1, s.materialName, s.spec || '-', s.unit || '个', s.quantity, s.unitPrice, s.amount, s.quoteRemark || ''])
  })
  qd.push([])

  // 金额汇总
  qd.push(['金额汇总'])
  qd.push(['', '物料合计', '', '', '', '', `¥${formatMoney(materialTotal.value)}`])
  qd.push(['', `折扣 (${form.discount}%)`, '', '', '', '', `-¥${formatMoney(discountDeduct.value)}`])
  if ((form.taxRate ?? 0) > 0) {
    qd.push(['', `税率 (${form.taxRate}%)`, '', '', '', '', `+¥${formatMoney(taxAmount.value)}`])
  }
  qd.push(['', '报价总计(含税)', '', '', '', '', `¥${formatMoney(grandTotal.value)}`])
  qd.push([])

  if (form.remark) {
    qd.push(['备注:', form.remark])
    qd.push([])
  }

  qd.push(['本报价单仅供参考，最终价格以双方书面确认为准。'])
  qd.push([])

  // 公司账户信息
  if (currentCompany.bankName || currentCompany.taxNo) {
    qd.push(['--- 开户信息 ---'])
    if (currentCompany.bankName) qd.push(['开户银行:', currentCompany.bankName])
    if (currentCompany.bankAccount) qd.push(['银行账号:', currentCompany.bankAccount])
    if (currentCompany.taxNo) qd.push(['税号:', currentCompany.taxNo])
  }

  const ws1 = XLSX.utils.aoa_to_sheet(qd)
  ws1['!cols'] = [{ wch: 6 }, { wch: 24 }, { wch: 18 }, { wch: 8 }, { wch: 8 }, { wch: 12 }, { wch: 14 }, { wch: 20 }]
  ws1['!merges'] = [
    { s: { r: 0, c: 0 }, e: { r: 0, c: 7 } },
    { s: { r: 4, c: 0 }, e: { r: 4, c: 3 } },
  ]
  XLSX.utils.book_append_sheet(wb, ws1, '报价单')

  // Sheet2: 公司信息
  const companyData = [
    ['公司基础信息'], [''],
    ['项目', '内容'],
    ['公司全称', currentCompany.name],
    ['地址', currentCompany.address],
    ['电话', currentCompany.phone],
    ['传真', currentCompany.fax],
    ['邮箱', currentCompany.email],
    ['开户银行', currentCompany.bankName],
    ['银行账号', currentCompany.bankAccount],
    ['税号', currentCompany.taxNo],
  ]
  const ws2 = XLSX.utils.aoa_to_sheet(companyData)
  ws2['!cols'] = [{ wch: 12 }, { wch: 40 }]
  XLSX.utils.book_append_sheet(wb, ws2, '公司信息')

  const fileName = `报价单_${form.customerName || '客户'}_${todayStr.value}.xlsx`
  const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
  saveAs(new Blob([wbout], { type: 'application/octet-stream' }), fileName)
  ElMessage.success(`✅ 报价单已导出: ${fileName}`)
}

// ===== 初始化 =====
onMounted(async () => {
  // 设置默认有效期（30天后）
  const d = new Date()
  d.setDate(d.getDate() + 30)
  form.validUntil = d.toISOString().slice(0, 10)

  await Promise.all([
    loadCompanies(),
    loadCustomers(),
    loadMaterialsAndCategories(),
  ])
})
</script>

<style scoped>
/* ===== 基础卡片 ===== */
.card {
  background: var(--bg);
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 20px;
  margin-bottom: 20px;
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
/* ===== 面包屑 ===== */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  margin-bottom: 16px;
  color: var(--text2);
}
.breadcrumb-item { cursor: pointer; transition: color .2s; }
.breadcrumb-item:hover:not(.active) { color: var(--primary); }
.breadcrumb-separator { color: #c0c4cc; }
.breadcrumb-item.active { color: var(--text1); font-weight: 600; }
/* ===== 页头 ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  flex-wrap: wrap;
  gap: 8px;
}
.page-title { font-size: 20px; font-weight: 700; color: var(--text1); margin: 0; }
.page-actions { display: flex; gap: 8px; flex-wrap: wrap; }
/* ===== 按钮 ===== */
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
.btn-primary { background: var(--primary); color: #fff; }
.btn-primary:hover:not(:disabled) {
  background: var(--primary-dark, var(--primary));
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(var(--primary-rgb),.35);
}
.btn-primary:disabled { opacity: .55; cursor: not-allowed; }
.btn-default { background: #f5f7fa; color: var(--text2); border: 1px solid #dcdfe6; }
.btn-default:hover { background: #ebeef5; }
.btn-success { background: linear-gradient(135deg, #67c23a, #85ce61); color: #fff; }
.btn-success:hover { opacity: .9; transform: translateY(-1px); }
.btn-danger { background: linear-gradient(135deg, #f56c6c, #fa897b); color: #fff; }
.btn-danger:hover { opacity: .9; transform: translateY(-1px); }
.btn-outline-primary {
  background: #ecf5ff; color: var(--primary); border: 1px solid #b3d8ff;
}
.btn-outline-primary:hover { background: var(--primary); color: #fff; }
/* ===== 表单 ===== */
.form-row { display: flex; gap: 16px; margin-bottom: 14px; }
.form-group { display: flex; flex-direction: column; gap: 5px; }
.form-group label { font-size: 13px; font-weight: 500; color: var(--text2); white-space: nowrap; }
.required { color: #f56c6c; margin-right: 2px; }
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
.form-control:focus { border-color: var(--primary); box-shadow: 0 0 0 2px rgba(var(--primary-rgb),.15); }
textarea.form-control { resize: vertical; }

/* ===== 公司信息预览条 ===== */
.company-preview-bar {
  margin-top: 12px;
  padding: 10px 16px;
  background: linear-gradient(135deg, rgba(var(--primary-rgb),.07), rgba(var(--primary-rgb),.02));
  border-radius: 8px;
  font-size: 13px;
  color: var(--text2);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.cp-label { font-weight: 600; color: var(--primary); }
.cp-sep { color: #dcdfe6; }

/* ===== 物料区域 ===== */
.materials-two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  max-height: calc(100vh - 520px);
  min-height: 240px;
  overflow-y: auto;
}
.materials-two-col::-webkit-scrollbar { width: 6px; }
.materials-two-col::-webkit-scrollbar-thumb { background: #c0c4cc; border-radius: 3px; }
.materials-col { border: 1px solid var(--border); border-radius: 8px; overflow: hidden; background: #fafafa; }
.materials-empty { grid-column: 1/-1; text-align: center; padding: 40px; color: #909399; }
.loading-placeholder { text-align: center; padding: 60px 0; color: #909399; }

/* ===== 数据表格 ===== */
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table thead th {
  background: #f5f7fa; color: var(--text2); font-weight: 600;
  padding: 9px 8px; text-align: left; border-bottom: 1px solid var(--border);
  font-size: 12px; position: sticky; top: 0; z-index: 1;
}
.data-table tbody td { padding: 7px 8px; border-bottom: 1px solid #f0f0f0; vertical-align: middle; }
.data-table tbody tr:hover { background: #f8faff; }
.row-selected { background: rgba(var(--primary-rgb),.05) !important; }
.tag { display: inline-block; padding: 2px 8px; font-size: 11px; background: #ecf5ff; color: #409eff; border-radius: 4px; }

/* ===== 输入框 ===== */
.qty-input, .price-input {
  border: 1px solid #dcdfe6; border-radius: 4px; outline: none; font-size: 13px; color: var(--text1); transition: all .2s;
}
.qty-input:focus, .price-input:focus { border-color: var(--primary); }
.price-input { color: #e6a23c; font-weight: 500; }

/* 小按钮 */
.btn-sm { padding: 3px 8px; font-size: 12px; border-radius: 4px; cursor: pointer; border: 1px solid transparent; transition: all .2s; }
.btn-primary-outline { color: var(--primary); background: #ecf5ff; border-color: #b3d8ff; }
.btn-primary-outline:hover { background: var(--primary); color: #fff; }
.btn-custom-add { background: linear-gradient(135deg, #67c23a, #85ce61); color: #fff; border: none; font-size: 12px; padding: 4px 12px; white-space: nowrap; }
.btn-custom-add:hover { opacity: .9; transform: translateY(-1px); }
.delete-sm { width: 24px; height: 24px; padding: 0; font-size: 12px; color: #f56c6c; border: none; background: #fef0f0; border-radius: 4px; cursor: pointer; transition: all .2s; }
.delete-sm:hover { background: #f56c6c; color: #fff; }

/* ===== 已选物料清单 ===== */
.selected-table thead th { background: #fdf6ec; color: #e6a23c; font-size: 12px; }
.selected-table tfoot td { background: #fdf6ec; font-size: 14px; padding: 10px 8px; border-top: 2px solid #e6a23c; }
.total-row td { border-top: 2px solid #e6a23c !important; }
.custom-row { background: linear-gradient(135deg, #f0f9eb, #fff) !important; }
.custom-row:hover { background: linear-gradient(135deg, #e1f3d8, #fff) !important; }
.inline-edit-input { border: 1px solid #dcdfe6; border-radius: 4px; padding: 3px 6px; font-size: 12px; outline: none; transition: all .2s; background: #fff; color: var(--text1); }
.inline-edit-input:focus { border-color: #67c23a; box-shadow: 0 0 0 2px rgba(103,194,58,.15); }
.center-text { text-align: center; }
.qty-inline { width: 54px; text-align: center; border: 1px solid #dcdfe6; border-radius: 4px; padding: 2px 4px; font-size: 12px; outline: none; }
.qty-inline:focus { border-color: var(--primary); }
.price-inline { width: 64px; text-align: right; border: 1px solid #dcdfe6; border-radius: 4px; padding: 2px 4px; font-size: 12px; outline: none; color: #e6a23c; font-weight: 500; }
.price-inline:focus { border-color: #e6a23c; }
.remark-inline { width: 90px; border: 1px solid #dcdfe6; border-radius: 4px; padding: 2px 4px; font-size: 11px; outline: none; text-align: center; }
.tag-custom { display: inline-block; padding: 1px 6px; font-size: 10px; background: #f0f9eb; color: #67c23a; border-radius: 3px; }
.tag-preset { display: inline-block; padding: 1px 6px; font-size: 10px; background: #ecf5ff; color: #409eff; border-radius: 3px; }

/* ===== 底部汇总栏 ===== */
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
  gap: 16px;
  padding: 14px 20px;
  flex-wrap: wrap;
}
.summary-item { font-size: 14px; color: var(--text2); display: flex; align-items: center; gap: 4px; }
.summary-divider { width: 1px; height: 22px; background: var(--border); }
.summary-actions { display: flex; gap: 8px; margin-left: auto; }
.submit-btn { padding: 10px 28px; font-size: 15px; font-weight: 600; border-radius: 8px; }

/* ===== 报价单预览弹窗 ===== */
.quote-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,.5); backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex; justify-content: center; align-items: flex-start;
  padding: 30px 20px; overflow-y: auto;
}
.quote-dialog {
  background: #fff; border-radius: 16px;
  box-shadow: 0 20px 80px rgba(0,0,0,.25);
  width: 920px; max-width: 100%; position: relative;
  animation: quoteIn .3s ease-out; margin: auto;
}
@keyframes quoteIn {
  from { opacity: 0; transform: scale(.96) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.quote-close-btn {
  position: absolute; top: 14px; right: 14px;
  width: 32px; height: 32px; border-radius: 50%;
  border: none; background: #f5f7fa; color: #909399;
  font-size: 16px; cursor: pointer; transition: all .2s;
  display: flex; align-items: center; justify-content: center;
}
.quote-close-btn:hover { background: #f56c6c; color: #fff; }
.quote-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  padding: 28px 28px 0;
}
.quote-company-name { margin: 0 0 6px; font-size: 22px; font-weight: 800; color: #1a1a2e; }
.quote-company-detail { font-size: 12px; color: #909399; }
.quote-header-right { text-align: right; }
.quote-badge {
  display: inline-block;
  background: linear-gradient(135deg, var(--primary), #3b82f6);
  color: #fff; font-size: 18px; font-weight: 700;
  padding: 8px 24px; border-radius: 8px; letter-spacing: 4px;
}
.quote-no { margin-top: 8px; font-size: 12px; color: #909399; }
.quote-meta-bar {
  display: flex; gap: 20px; padding: 14px 28px;
  background: #f8fafc; border-bottom: 1px solid #eee;
  font-size: 13px; flex-wrap: wrap;
}
.meta-item { display: flex; align-items: center; gap: 4px; }
.meta-label { color: #909399; }
.date-small-input { border: 1px solid #dcdfe6; border-radius: 4px; padding: 2px 6px; font-size: 12px; outline: none; }
.date-small-input:focus { border-color: var(--primary); }
.quote-section-title { font-size: 15px; font-weight: 700; color: var(--text1); padding: 16px 28px 10px; border-bottom: 1px solid #f0f0f0; }
.quote-table { margin: 0 28px 16px; border: 1px solid #ebeef5; border-radius: 8px; overflow: hidden; }
.quote-table thead th { background: linear-gradient(135deg, #1a1a2e, #2d3748); color: #fff !important; font-size: 12px; }
.quote-table tbody tr:nth-child(even) { background: #fafbfc; }
.quote-table tbody td { font-size: 12px; }
.quote-totals-area { margin: 0 28px 16px; background: linear-gradient(135deg, #f8fafc, #f1f5f9); border: 1px solid #e2e8f0; border-radius: 10px; padding: 16px 20px; }
.totals-grid { display: flex; flex-direction: column; gap: 8px; }
.total-line { display: flex; justify-content: space-between; align-items: center; font-size: 14px; padding: 4px 0; }
.tl-label { color: #606266; display: flex; align-items: center; gap: 6px; }
.tl-value { font-weight: 600; color: #303133; min-width: 120px; text-align: right; }
.discount-val { color: #67c23a !important; }
.tax-val { color: #e6a23c !important; }
.grand-total-line { margin-top: 6px; padding-top: 10px; border-top: 2px dashed #dcdfe6; font-size: 16px !important; }
.grand-total { color: var(--primary) !important; font-size: 22px !important; font-weight: 800 !important; }
.pct-input { width: 55px; border: 1px solid #dcdfe6; border-radius: 4px; padding: 2px 4px; font-size: 13px; outline: none; text-align: center; }
.pct-input:focus { border-color: var(--primary); }
.quote-remark-textarea {
  margin: 0 28px 12px; width: calc(100% - 56px);
  border: 1px solid #dcdfe6; border-radius: 6px; padding: 10px 12px;
  font-size: 13px; outline: none; resize: vertical; font-family: inherit;
}
.quote-remark-textarea:focus { border-color: var(--primary); }
.quote-clause {
  margin: 0 28px 16px; padding: 10px 14px;
  background: #fffbf0; border: 1px solid #faecd8;
  border-radius: 6px; font-size: 12px; color: #e6a23c; line-height: 1.6;
}
.quote-actions {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 16px 28px 24px; border-top: 1px solid #f0f0f0;
}
.quote-actions .btn { padding: 10px 22px; font-size: 14px; }
</style>
