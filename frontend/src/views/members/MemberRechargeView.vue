<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item" @click="$router.push('/members')">会员管理</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item active">充值管理</span>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">💳 充值管理</h1>
      <div class="page-actions">
        <button class="btn btn-default" @click="exportData">⬇️ 导出</button>
        <button class="btn btn-primary" @click="openQuickRecharge">+ 快速充值</button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon blue">💳</div>
        <div class="stat-info"><h3>¥{{ stats.monthTotal }}</h3><p>本月充值总额</p></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">📈</div>
        <div class="stat-info"><h3>{{ stats.monthCount }}</h3><p>本月充值笔数</p></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">💰</div>
        <div class="stat-info"><h3>¥{{ stats.totalBalance }}</h3><p>预存余额合计</p></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">🎁</div>
        <div class="stat-info"><h3>¥{{ stats.monthBonus }}</h3><p>本月赠送金额</p></div>
      </div>
    </div>

    <!-- 搜索表单 -->
    <div class="search-form">
      <div class="form-group">
        <label>充值时间</label>
        <div style="display:flex;align-items:center;gap:6px;">
          <input v-model="searchForm.startDate" type="date" class="form-control" />
          <span style="color:#909399;">—</span>
          <input v-model="searchForm.endDate" type="date" class="form-control" />
        </div>
      </div>
      <div class="form-group">
        <label>会员名称</label>
        <input v-model="searchForm.keyword" type="text" class="form-control" placeholder="搜索会员名称" />
      </div>
      <div class="form-group" style="align-self:flex-end;">
        <button class="btn btn-primary" @click="handleSearch">🔍 搜索</button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="card">
      <table class="data-table recharge-table">
        <thead>
          <tr>
            <th style="width:140px;">流水单号</th>
            <th style="min-width:140px;">会员名称</th>
            <th style="width:110px;">充值金额</th>
            <th style="width:110px;">充值前余额</th>
            <th style="width:110px;">充值后余额</th>
            <th style="width:110px;">时间</th>
            <th>备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" style="text-align:center;padding:50px 20px;color:#909399;">
              <div style="font-size:28px;margin-bottom:8px;">⏳</div>
              <div>数据加载中...</div>
            </td>
          </tr>
          <tr v-else-if="list.length === 0">
            <td colspan="7" style="text-align:center;padding:50px 20px;color:#909399;">
              <div style="font-size:28px;margin-bottom:8px;">📭</div>
              <div>暂无充值记录</div>
            </td>
          </tr>
          <tr v-for="r in list" :key="r.id">
            <td style="color:#c0c4cc;font-size:12px;font-variant-numeric:tabular-nums;">#{{ r.transactionNo || r.id }}</td>
            <td style="font-weight:500;">{{ r.memberName || '会员ID:' + r.memberId }}</td>
            <td style="font-weight:600;color:#409eff;font-variant-numeric:tabular-nums;">¥{{ formatMoney(r.amount) }}</td>
            <td style="color:#909399;font-variant-numeric:tabular-nums;">¥{{ formatMoney(r.balanceBefore) }}</td>
            <td style="font-weight:700;color:#67c23a;font-variant-numeric:tabular-nums;">¥{{ formatMoney(r.balanceAfter) }}</td>
            <td style="color:#606266;font-size:12px;">{{ fmtDate(r.createTime) }}</td>
            <td style="color:#909399;font-size:12px;">{{ r.remark || '—' }}</td>
          </tr>
        </tbody>
      </table>
      <div class="pagination">
        <div class="pagination-info">共 {{ total }} 条记录</div>
        <div class="pagination-buttons">
          <button class="page-btn" :disabled="page <= 1" @click="page--;load()">«</button>
          <button v-for="p in pageRange" :key="p" :class="['page-btn', { active: p === page }]" @click="page=p;load()">{{ p }}</button>
          <button class="page-btn" :disabled="page >= totalPages" @click="page++;load()">»</button>
        </div>
      </div>
    </div>

    <!-- 快速充值弹窗 -->
    <div class="modal-overlay" :class="{ show: rechargeVisible }" @click.self="rechargeVisible=false">
      <div class="modal" style="max-width:520px;">
        <div class="modal-header"><h3 class="modal-title">💳 会员充值</h3><button class="modal-close" @click="rechargeVisible=false">×</button></div>
        <div class="modal-body">
          <div class="form-group"><label class="form-label">选择会员 *</label>
            <select v-model="rechargeForm.memberId" class="form-input">
              <option value="">请选择会员</option>
              <option v-for="m in memberList" :key="m.id" :value="m.id">{{ m.memberName }} (余额: ¥{{ Number(m.balance).toLocaleString() }})</option>
            </select>
          </div>
          <div class="form-row">
            <div class="form-group"><label class="form-label">充值金额 *</label><input v-model.number="rechargeForm.amount" type="number" class="form-input" placeholder="请输入充值金额" @input="calcBonus" /></div>
            <div class="form-group"><label class="form-label">充值方式</label>
              <select v-model="rechargeForm.method" class="form-input"><option value="wechat">微信支付</option><option value="alipay">支付宝</option><option value="transfer">银行转账</option><option value="cash">现金</option><option value="other">其他</option></select>
            </div>
          </div>
          <div style="background:#f0f9eb;padding:12px 14px;border-radius:8px;margin-bottom:12px;">
            <div style="display:flex;justify-content:space-between;font-size:13px;margin-bottom:6px;"><span style="color:#606266;">充值金额</span><span style="font-weight:600;">¥ {{ (rechargeForm.amount || 0).toFixed(2) }}</span></div>
            <div style="display:flex;justify-content:space-between;font-size:13px;margin-bottom:6px;"><span style="color:#606266;">赠送金额</span><span style="color:#67c23a;font-weight:600;">¥ {{ bonusAmount.toFixed(2) }}</span></div>
            <div style="display:flex;justify-content:space-between;font-size:14px;border-top:1px dashed #dcdfe6;padding-top:8px;"><span style="font-weight:600;">到账金额</span><span style="font-weight:700;color:#67c23a;font-size:16px;">¥ {{ totalAmount.toFixed(2) }}</span></div>
          </div>
          <div class="form-group"><label class="form-label">充值备注</label><input v-model="rechargeForm.remark" class="form-input" placeholder="如：季度充值（选填）" /></div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="rechargeVisible=false">取消</button>
          <button class="btn btn-success" @click="submitRecharge">✅ 确认充值</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { memberApi } from '@/api'

const list = ref<any[]>([])
const memberList = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 20

const searchForm = reactive({ keyword: '', startDate: '', endDate: '' })
const stats = reactive({ monthTotal: '0', monthCount: 0, totalBalance: '0', monthBonus: '0' })

const rechargeVisible = ref(false)
const rechargeForm = reactive({ memberId: 0 as number, amount: 0, method: 'wechat', remark: '' })
const bonusAmount = ref(0)
const totalAmount = ref(0)

function formatMoney(v: any) { return Number(v || 0).toLocaleString('zh', { minimumFractionDigits: 2 }) }
function fmtDate(d: any) {
  if (!d) return '-'
  return typeof d === 'string' ? d.slice(0, 10) : d.toISOString?.().slice(0, 10) || String(d).slice(0, 10)
}

const totalPages = computed(() => Math.ceil(total.value / pageSize))
const pageRange = computed(() => {
  const p = page.value, t = totalPages.value
  let arr = []
  for (let i = Math.max(1, p - 2); i <= Math.min(t, p + 2); i++) arr.push(i)
  return arr
})

function calcBonus() {
  const amt = rechargeForm.amount || 0
  const bonusRate = 0.04
  bonusAmount.value = Math.floor(amt * bonusRate * 100) / 100
  totalAmount.value = amt + bonusAmount.value
}

function openQuickRecharge() {
  Object.assign(rechargeForm, { memberId: 0, amount: 0, method: 'wechat', remark: '' })
  calcBonus()
  rechargeVisible.value = true
}

async function submitRecharge() {
  if (!rechargeForm.memberId) { ElMessage.warning('请选择会员！'); return }
  if (!rechargeForm.amount || rechargeForm.amount <= 0) { ElMessage.warning('请输入有效的充值金额！'); return }
  try {
    const remark = rechargeForm.remark || `充值（${rechargeForm.method}）`
    await memberApi.recharge(rechargeForm.memberId, { amount: rechargeForm.amount, remark })
    ElMessage.success(`充值成功！到账金额 ¥${totalAmount.value}`)
    rechargeVisible.value = false
    load()
    loadMembers() // 刷新会员列表余额
  } catch { ElMessage.error('充值失败') }
}

function handleSearch() { page.value = 1; load() }
function exportData() { ElMessage.success('导出功能开发中') }

async function load() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, size: pageSize }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.startDate) params.startDate = searchForm.startDate
    if (searchForm.endDate) params.endDate = searchForm.endDate
    const r = await memberApi.getRechargeRecords(params)
    list.value = r.data?.records || r.data?.list || []
    total.value = r.data?.total || list.value.length
    stats.monthCount = total.value
    stats.monthTotal = list.value.reduce((s: number, r: any) => s + Number(r.amount || 0), 0).toLocaleString('zh', { minimumFractionDigits: 2 })
    stats.monthBonus = list.value.reduce((s: number, r: any) => s + Number(r.giftAmount || 0), 0).toLocaleString('zh', { minimumFractionDigits: 2 })
    stats.totalBalance = memberList.value.reduce((s: number, m: any) => s + Number(m.balance || 0), 0).toLocaleString('zh', { minimumFractionDigits: 2 })
  } catch { list.value = [] }
  finally { loading.value = false }
}

async function loadMembers() {
  try { const r = await memberApi.getList({ size: 999 }); memberList.value = r.data?.records || r.data?.list || [] } catch { memberList.value = [] }
}

onMounted(() => { loadMembers(); load() })
</script>

<style scoped lang="scss">
:deep(.card) {
  overflow: hidden;
  padding: 0;
  table { width: 100%; border-collapse: collapse; }
}
:deep(.recharge-table) {
  thead th {
    padding: 13px 16px;
    font-size: 12px;
    font-weight: 600;
    color: #909399;
    background: #fafbfc;
    border-bottom: 1px solid #ebeef5;
    text-align: left;
    white-space: nowrap;
  }
  tbody td {
    padding: 15px 16px;
    font-size: 13px;
    color: #303133;
    border-bottom: 1px solid #f2f3f5;
    vertical-align: middle;
  }
  tbody tr:hover td { background: #f8faff; }
  tbody tr:last-child td { border-bottom: none; }
}
:deep(.pagination) { padding: 16px 20px; }
</style>
