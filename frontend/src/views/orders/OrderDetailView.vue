<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item" @click="router.push('/orders')">订单列表</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">订单详情</span>
    </div>

    <div v-if="loading" style="text-align:center;padding:40px;color:#909399;">加载中...</div>
    <template v-else-if="detailData">
      <!-- 头部信息 -->
      <div class="card detail-header">
        <div class="dh-left">
          <div class="dh-no">{{ detailData.orderNo }}</div>
          <span class="status-tag" :class="'status-' + statusKeyMap[detailData.status]">
            {{ statusLabelMap[detailData.status] }}
          </span>
        </div>
        <div class="dh-actions">
          <button class="btn btn-default" @click="router.push('/orders')">← 返回列表</button>
          <button class="btn btn-success" v-if="detailData.status === 1" @click="processVisible = true">⚡ 处理订单</button>
          <button class="btn btn-success" v-if="detailData.status === 2" @click="confirmDelivery">📦 确认交付</button>
          <button class="btn btn-primary" @click="paymentVisible = true">💰 登记收款</button>
        </div>
      </div>

      <!-- Tab 区 -->
      <div class="card" style="padding:0;">
        <div class="detail-tabs">
          <div class="detail-tab" :class="{ active: activeTab === 'basic' }" @click="activeTab = 'basic'">📋 基本信息</div>
          <div class="detail-tab" :class="{ active: activeTab === 'material' }" @click="activeTab = 'material'">📦 物料明细</div>
          <div class="detail-tab" :class="{ active: activeTab === 'finance' }" @click="activeTab = 'finance'">💰 财务信息</div>
        </div>

        <!-- 基本信息 -->
        <div v-if="activeTab === 'basic'" class="tab-content">
          <div class="info-grid">
            <div class="info-cell">
              <div class="info-label">订单编号</div>
              <div class="info-value">{{ detailData.orderNo }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">订单状态</div>
              <div><span class="status-tag" :class="'status-' + statusKeyMap[detailData.status]">{{ statusLabelMap[detailData.status] }}</span></div>
            </div>
            <div class="info-cell">
              <div class="info-label">客户名称</div>
              <div class="info-value">{{ detailData.customerName }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">订单金额</div>
              <div class="info-value" style="color:#f56c6c;font-weight:700;">¥{{ formatMoney(detailData.totalAmount) }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">交付日期</div>
              <div class="info-value">{{ detailData.deliveryDate || '-' }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">设计师</div>
              <div class="info-value">{{ detailData.designerName || '待分配' }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">订单类型</div>
              <div class="info-value">{{ orderTypeMap[detailData.orderType] || '其他' }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">支付状态</div>
              <div><span class="status-tag" :class="'status-' + payKeyMap[detailData.paymentStatus]">{{ payLabelMap[detailData.paymentStatus] }}</span></div>
            </div>
            <div class="info-cell">
              <div class="info-label">创建时间</div>
              <div class="info-value">{{ formatTime(detailData.createTime) }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">联系人</div>
              <div class="info-value">{{ detailData.contactPerson || '-' }}</div>
            </div>
          </div>
          <div style="margin-top:16px;">
            <div style="font-size:12px;color:#909399;margin-bottom:6px;">订单描述</div>
            <div style="background:#f5f7fa;padding:12px;border-radius:8px;font-size:13px;line-height:1.7;">{{ detailData.description || '暂无描述' }}</div>
          </div>
          <div style="margin-top:20px;">
            <div style="font-weight:600;margin-bottom:10px;">处理进度</div>
            <div class="status-flow" style="justify-content:flex-start;">
              <div class="flow-step" v-for="(s, i) in statusFlow" :key="i">
                <div class="flow-dot" :class="{ completed: i < detailStatusIdx, active: i === detailStatusIdx }">
                  {{ i < detailStatusIdx ? '✓' : i + 1 }}
                </div>
                <div class="flow-label" :class="{ active: i <= detailStatusIdx }">{{ s }}</div>
              </div>
              <div class="flow-line" v-for="i in statusFlow.length - 1" :key="'line-' + i"></div>
            </div>
          </div>
        </div>

        <!-- 物料明细 -->
        <div v-if="activeTab === 'material'" class="tab-content">
          <div style="margin-bottom:12px;display:flex;justify-content:space-between;align-items:center;">
            <span style="font-size:13px;color:#606266;">共 {{ detailMaterials.length }} 项物料/工艺</span>
          </div>
          <table class="data-table" v-if="detailMaterials.length">
            <thead>
              <tr><th>物料/工艺</th><th>规格</th><th>数量</th><th>单价</th><th>小计</th></tr>
            </thead>
            <tbody>
              <tr v-for="m in detailMaterials" :key="m.id">
                <td>{{ m.materialName }}</td>
                <td>{{ m.spec || '-' }}</td>
                <td>{{ m.quantity }} {{ m.unit || '' }}</td>
                <td>¥{{ formatMoney(m.unitPrice) }}</td>
                <td style="font-weight:600;color:#67c23a;">¥{{ formatMoney(m.amount) }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else style="text-align:center;color:#c0c4cc;padding:20px;">暂无物料明细</p>
          <div v-if="detailMaterialTotal > 0" style="margin-top:14px;padding:12px 16px;background:#f0f9eb;border-radius:8px;display:flex;justify-content:space-between;align-items:center;">
            <span style="font-size:13px;color:#606266;">物料合计：</span>
            <span style="font-size:18px;font-weight:700;color:#67c23a;">¥ {{ formatMoney(detailMaterialTotal) }}</span>
          </div>
        </div>

        <!-- 财务信息 -->
        <div v-if="activeTab === 'finance'" class="tab-content">
          <div class="finance-summary">
            <div class="finance-card" style="border-color:#67c23a;">
              <div class="finance-label" style="color:#67c23a;">订单总额</div>
              <div class="finance-value" style="color:#67c23a;">¥{{ formatMoney(detailData.totalAmount) }}</div>
            </div>
            <div class="finance-card" style="border-color:#409eff;">
              <div class="finance-label" style="color:#409eff;">已收金额</div>
              <div class="finance-value" style="color:#409eff;">¥{{ formatMoney(detailData.paidAmount) }}</div>
            </div>
            <div class="finance-card" style="border-color:#f56c6c;">
              <div class="finance-label" style="color:#f56c6c;">待收余额</div>
              <div class="finance-value" style="color:#f56c6c;">¥{{ formatMoney((detailData.totalAmount || 0) - (detailData.paidAmount || 0)) }}</div>
            </div>
          </div>
        </div>
      </div>
    </template>
    <div v-else style="text-align:center;padding:40px;color:#909399;">订单不存在</div>

    <!-- 处理订单弹窗 -->
    <div class="modal-overlay" v-if="processVisible" @click.self="processVisible = false">
      <div class="modal-container" style="max-width:520px;">
        <div class="modal-header"><h3>⚡ 处理订单</h3><button class="modal-close" @click="processVisible = false">&times;</button></div>
        <div class="modal-body">
          <div style="text-align:center;padding:16px 0;">
            <div style="width:60px;height:60px;background:#ecf5ff;border-radius:50%;display:flex;align-items:center;justify-content:center;margin:0 auto 16px;font-size:30px;">📋</div>
            <h3 style="margin-bottom:8px;">确认接收此订单？</h3>
            <p style="color:#909399;font-size:13px;">订单 {{ detailData?.orderNo }} 将进入设计阶段</p>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">分配设计师 *</label>
              <select class="form-input" v-model="processForm.designerName">
                <option value="">请选择</option><option>李明</option><option>王设计</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">交付日期</label>
              <input type="date" class="form-input" v-model="processForm.deliveryDate">
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="processVisible = false">取消</button>
          <button class="btn btn-success" @click="submitProcess">✅ 确认接收</button>
        </div>
      </div>
    </div>

    <!-- 登记收款弹窗 -->
    <div class="modal-overlay" v-if="paymentVisible" @click.self="paymentVisible = false">
      <div class="modal-container" style="max-width:460px;">
        <div class="modal-header"><h3>💰 登记收款</h3><button class="modal-close" @click="paymentVisible = false">&times;</button></div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">收款金额 *</label>
            <input type="number" class="form-input" v-model.number="paymentForm.amount" placeholder="0.00" min="0" step="0.01">
          </div>
          <div class="form-group">
            <label class="form-label">收款方式</label>
            <select class="form-input" v-model="paymentForm.method">
              <option value="微信">微信</option><option value="支付宝">支付宝</option>
              <option value="银行转账">银行转账</option><option value="现金">现金</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">备注</label>
            <textarea class="form-input" v-model="paymentForm.remark" rows="2" placeholder="可选"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="paymentVisible = false">取消</button>
          <button class="btn btn-success" @click="submitPayment">💰 确认收款</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderApi } from '@/api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeTab = ref('basic')

const statusFlow = ['待确认', '进行中', '已完成', '已取消']
const statusLabelMap: Record<number, string> = { 1: '待确认', 2: '进行中', 3: '已完成', 4: '已取消' }
const statusKeyMap: Record<number, string> = { 1: 'pending', 2: 'designing', 3: 'completed', 4: 'cancelled' }
const payLabelMap: Record<number, string> = { 1: '未付款', 2: '部分付', 3: '已付清' }
const payKeyMap: Record<number, string> = { 1: 'cancelled', 2: 'delivery', 3: 'completed' }
const orderTypeMap: Record<number, string> = { 1: '图文打印', 2: '广告制作', 3: '设计服务', 4: '装订服务', 5: '其他' }

const detailData = ref<any>(null)
const detailMaterials = ref<any[]>([])
const detailMaterialTotal = computed(() => detailMaterials.value.reduce((s: number, m: any) => s + ((m.quantity || 0) * (m.unitPrice || 0)), 0))
const detailStatusIdx = computed(() => {
  const s = detailData.value?.status
  if (s === 1) return 0; if (s === 2) return 1; if (s === 3) return 2; return 0
})

const processVisible = ref(false)
const processForm = reactive({ designerName: '', deliveryDate: '' })
const paymentVisible = ref(false)
const paymentForm = reactive({ amount: 0, method: '微信', remark: '' })

async function loadDetail() {
  loading.value = true
  try {
    const res = await orderApi.getDetail(Number(route.params.id))
    const d = res.data
    detailData.value = d?.order || d
    detailMaterials.value = d?.materials || []
  } catch { detailData.value = null }
  finally { loading.value = false }
}

async function submitProcess() {
  if (!processForm.designerName) { alert('请选择设计师'); return }
  await orderApi.update(detailData.value.id, {
    status: 2, designerName: processForm.designerName,
    deliveryDate: processForm.deliveryDate || null,
  })
  processVisible.value = false; loadDetail()
}

async function confirmDelivery() {
  if (!confirm('确认订单已完成交付？')) return
  await orderApi.updateStatus(detailData.value.id, 3)
  loadDetail()
}

async function submitPayment() {
  if (!paymentForm.amount || paymentForm.amount <= 0) { alert('请输入有效金额'); return }
  await orderApi.addPayment(detailData.value.id, paymentForm)
  paymentVisible.value = false; paymentForm.amount = 0; loadDetail()
}

function formatMoney(v: any) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatTime(t: any) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(loadDetail)
</script>

<style scoped lang="scss">
.page-container { padding: 20px 24px; }

.detail-header {
  display: flex; align-items: center; justify-content: space-between;
}
.dh-left { display: flex; align-items: center; gap: 12px; }
.dh-no { font-size: 18px; font-weight: 700; }
.dh-actions { display: flex; gap: 10px; }

.detail-tabs {
  display: flex; gap: 0; border-bottom: 1px solid #f0f0f0;
  margin: 0 20px; padding: 0;
}
.detail-tab {
  padding: 14px 20px; cursor: pointer; font-size: 13px; font-weight: 500;
  border-bottom: 2px solid transparent; color: #909399; transition: all 0.2s;
  &:hover { color: #606266; }
  &.active { color: #409eff; border-bottom-color: #409eff; }
}
.tab-content { padding: 20px; }

.status-flow {
  display: flex; align-items: center; gap: 0; flex-wrap: wrap;
}
.flow-step {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
}
.flow-dot {
  width: 36px; height: 36px; border-radius: 50%;
  background: #e4e7ed; color: #909399;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600;
  &.active { background: #409eff; color: #fff; }
  &.completed { background: #67c23a; color: #fff; }
}
.flow-label {
  font-size: 12px; color: #909399;
  &.active { color: #409eff; font-weight: 600; }
}
.flow-line {
  width: 60px; height: 2px; background: #e4e7ed; margin: 0 10px;
  align-self: flex-start; margin-top: 18px;
}

.info-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
}
.info-cell { background: #f5f7fa; padding: 12px; border-radius: 8px; }
.info-label { font-size: 11px; color: #909399; margin-bottom: 4px; }
.info-value { font-size: 14px; font-weight: 600; }

.finance-summary {
  display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px;
}
.finance-card {
  background: #f5f7fa; border-radius: 8px; padding: 14px; text-align: center;
  border-top: 3px solid;
}
.finance-label { font-size: 11px; margin-bottom: 6px; }
.finance-value { font-size: 18px; font-weight: 700; }

.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th, .data-table td {
  padding: 10px 12px; text-align: left; border-bottom: 1px solid #f0f0f0;
}
.data-table th { background: #fafbfc; color: #606266; font-weight: 600; font-size: 12px; }

.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modal-container {
  background: #fff; border-radius: 12px; width: 90%;
  box-shadow: 0 8px 30px rgba(0,0,0,0.15);
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 24px; border-bottom: 1px solid #f0f0f0;
  h3 { font-size: 16px; margin: 0; }
}
.modal-close {
  width: 30px; height: 30px; border: none; background: none;
  font-size: 20px; cursor: pointer; color: #909399; border-radius: 6px;
  &:hover { background: #f5f7fa; color: #303133; }
}
.modal-body { padding: 20px 24px; }
.modal-footer {
  padding: 14px 24px; border-top: 1px solid #f0f0f0;
  display: flex; justify-content: flex-end; gap: 10px;
}
</style>
