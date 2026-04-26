<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item active">会员管理</span>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">👑 会员列表</h1>
      <div class="page-actions">
        <button class="btn btn-default" @click="exportData">⬇️ 导出</button>
        <button class="btn btn-primary" @click="openAdd">+ 新增会员</button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon purple">👑</div>
        <div class="stat-info"><h3>{{ stats.total }}</h3><p>会员总数</p></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">✅</div>
        <div class="stat-info"><h3>{{ stats.active }}</h3><p>活跃会员</p></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon blue">💰</div>
        <div class="stat-info"><h3>¥{{ stats.totalBalance }}</h3><p>预存余额合计</p></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">📋</div>
        <div class="stat-info"><h3>{{ stats.totalOrders }}</h3><p>关联订单总数</p></div>
      </div>
    </div>

    <!-- 搜索表单 -->
    <div class="search-form">
      <div class="form-group">
        <label>会员名称</label>
        <input v-model="searchForm.keyword" type="text" class="form-control" placeholder="搜索会员名称/电话" />
      </div>
      <div class="form-group">
        <label>会员等级</label>
        <select v-model="searchForm.level" class="form-control">
          <option value="">全部等级</option>
          <option v-for="l in levelOptions" :key="l.value" :value="l.value">{{ l.label }}</option>
        </select>
      </div>
      <div class="form-group">
        <label>账户状态</label>
        <select v-model.number="searchForm.status" class="form-control">
          <option value="">全部状态</option>
          <option :value="1">正常</option>
          <option :value="0">停用</option>
        </select>
      </div>
      <div class="form-group" style="align-self:flex-end;">
        <button class="btn btn-primary" @click="handleSearch">🔍 搜索</button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="card">
      <table class="data-table member-table">
        <thead>
          <tr>
            <th style="width:50px;">ID</th>
            <th style="min-width:180px;">会员信息</th>
            <th style="width:120px;">联系电话</th>
            <th style="width:100px;">会员等级</th>
            <th style="width:105px;">预存余额</th>
            <th style="width:105px;">累计充值</th>
            <th style="width:105px;">累计消费</th>
            <th style="width:75px;">订单</th>
            <th style="width:160px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="10" style="text-align:center;padding:50px 20px;color:#909399;">
              <div style="font-size:28px;margin-bottom:8px;">⏳</div>
              <div>数据加载中...</div>
            </td>
          </tr>
          <tr v-else-if="list.length === 0">
            <td colspan="9" style="text-align:center;padding:50px 20px;color:#909399;">
              <div style="font-size:28px;margin-bottom:8px;">📭</div>
              <div>暂无会员数据</div>
            </td>
          </tr>
          <tr v-for="m in list" :key="m.id">
            <td style="color:#c0c4cc;font-size:12px;font-variant-numeric:tabular-nums;">{{ m.id }}</td>
            <td>
              <div class="member-info-cell">
                <div class="member-name" :title="m.memberName">{{ m.memberName }}</div>
                <span :class="['status-tag', m.status === 1 ? 'status-completed' : 'status-cancelled']">{{ m.status === 1 ? '正常' : '停用' }}</span>
              </div>
              <div style="font-size:11px;color:#c0c4cc;margin-top:2px;">加入：{{ fmtDate(m.createTime) }}</div>
            </td>
            <td style="color:#606266;font-variant-numeric:tabular-nums;">{{ m.phone || '-' }}</td>
            <td><span class="level-badge" :style="getLevelStyle(m.level)">{{ getLevelIcon(m.level) }} {{ getLevelName(m.level) }}</span></td>
            <td style="font-weight:700;font-variant-numeric:tabular-nums;" :style="{ color: Number(m.balance) > 0 ? '#67c23a' : '#c0c4cc' }">¥{{ formatMoney(m.balance) }}</td>
            <td style="color:#409eff;font-variant-numeric:tabular-nums;">¥{{ formatMoney(m.totalRecharge) }}</td>
            <td style="color:#e6a23c;font-variant-numeric:tabular-nums;">¥{{ formatMoney(m.totalConsume) }}</td>
            <td>
              <a style="color:#409eff;font-weight:600;cursor:pointer;font-variant-numeric:tabular-nums;" @click="openOrders(m)">{{ m.orderCount || 0 }}<span style="font-weight:400;font-size:11px;margin-left:2px;">单</span></a>
            </td>
            <td>
              <div class="action-btns">
                <button class="action-btn view" @click="openDetail(m)">详情</button>
                <button class="action-btn recharge" @click="openRecharge(m)">充值</button>
                <button class="action-btn edit" @click="openEdit(m)">编辑</button>
              </div>
            </td>
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

    <!-- 新增会员弹窗 -->
    <div class="modal-overlay" :class="{ show: addVisible }" @click.self="addVisible=false">
      <div class="modal">
        <div class="modal-header"><h3 class="modal-title">👑 新增会员</h3><button class="modal-close" @click="addVisible=false">×</button></div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group"><label class="form-label">会员名称 *</label><input v-model="addForm.memberName" class="form-input" placeholder="请输入公司或个人名称" /></div>
            <div class="form-group"><label class="form-label">联系电话 *</label><input v-model="addForm.phone" class="form-input" placeholder="请输入联系电话" /></div>
          </div>
          <div class="form-row">
            <div class="form-group"><label class="form-label">会员等级</label>
              <select v-model="addForm.level" class="form-input">
                <option value="normal">普通会员</option><option value="silver">银牌会员</option><option value="gold">金牌会员</option><option value="diamond">钻石会员</option>
              </select>
            </div>
            <div class="form-group"><label class="form-label">初始预存金额</label><input v-model.number="addForm.balance" type="number" class="form-input" placeholder="0.00（选填）" /></div>
          </div>
          <div class="form-row">
            <div class="form-group"><label class="form-label">联系人</label><input v-model="addForm.contactPerson" class="form-input" placeholder="主要联系人姓名" /></div>
            <div class="form-group"><label class="form-label">备注</label><input v-model="addForm.remark" class="form-input" placeholder="备注信息（选填）" /></div>
          </div>
          <div style="background:#ecf5ff;padding:10px 14px;border-radius:6px;font-size:12px;color:#409eff;margin-top:4px;">💡 若填写初始预存金额，系统将自动生成一条充值记录</div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="addVisible=false">取消</button>
          <button class="btn btn-primary" @click="submitAdd">✅ 确认新增</button>
        </div>
      </div>
    </div>

    <!-- 编辑会员弹窗 -->
    <div class="modal-overlay" :class="{ show: editVisible }" @click.self="editVisible=false">
      <div class="modal">
        <div class="modal-header"><h3 class="modal-title">✏️ 编辑会员 — {{ editForm.memberName }}</h3><button class="modal-close" @click="editVisible=false">×</button></div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group"><label class="form-label">会员名称 *</label><input v-model="editForm.memberName" class="form-input" /></div>
            <div class="form-group"><label class="form-label">联系电话</label><input v-model="editForm.phone" class="form-input" /></div>
          </div>
          <div class="form-row">
            <div class="form-group"><label class="form-label">会员等级</label>
              <select v-model="editForm.level" class="form-input">
                <option value="normal">普通会员</option><option value="silver">银牌会员</option><option value="gold">金牌会员</option><option value="diamond">钻石会员</option>
              </select>
            </div>
            <div class="form-group"><label class="form-label">账户状态</label>
              <select v-model.number="editForm.status" class="form-input"><option :value="1">正常</option><option :value="0">停用</option></select>
            </div>
          </div>
          <div style="background:#fdf6ec;padding:10px 14px;border-radius:6px;font-size:13px;color:#e6a23c;margin-top:4px;">⚠️ 修改会员等级不影响历史消费记录，但会影响后续折扣计算</div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="editVisible=false">取消</button>
          <button class="btn btn-primary" @click="submitEdit">💾 保存</button>
        </div>
      </div>
    </div>

    <!-- 会员详情弹窗 -->
    <div class="modal-overlay" :class="{ show: detailVisible }" @click.self="detailVisible=false">
      <div class="modal" style="max-width:560px;">
        <div class="modal-header"><h3 class="modal-title">👑 会员详情 — {{ currentMember?.memberName }}</h3><button class="modal-close" @click="detailVisible=false">×</button></div>
        <div class="modal-body" v-if="currentMember">
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:16px;">
            <div style="background:#f5f7fa;padding:12px;border-radius:8px;grid-column:span 2;">
              <div style="display:flex;align-items:center;justify-content:space-between;">
                <div>
                  <div style="font-size:16px;font-weight:700;margin-bottom:4px;">{{ currentMember.memberName }}</div>
                  <div style="font-size:12px;color:#909399;">ID: {{ currentMember.id }} · 加入日期: {{ fmtDate(currentMember.createTime) }}</div>
                </div>
                <span class="level-badge" :style="getLevelStyle(currentMember.level)">{{ getLevelIcon(currentMember.level) }} {{ getLevelName(currentMember.level) }}</span>
              </div>
            </div>
            <div style="background:linear-gradient(135deg,#409eff,#66b1ff);padding:14px;border-radius:8px;color:#fff;">
              <div style="font-size:11px;opacity:0.85;margin-bottom:4px;">💰 预存余额</div>
              <div style="font-size:20px;font-weight:700;">¥{{ formatMoney(currentMember.balance) }}</div>
            </div>
            <div style="background:#f0f9eb;padding:14px;border-radius:8px;">
              <div style="font-size:11px;color:#67c23a;margin-bottom:4px;">📈 累计充值</div>
              <div style="font-size:18px;font-weight:700;color:#67c23a;">¥{{ formatMoney(currentMember.totalRecharge) }}</div>
            </div>
            <div style="background:#fdf6ec;padding:14px;border-radius:8px;">
              <div style="font-size:11px;color:#e6a23c;margin-bottom:4px;">💸 累计消费</div>
              <div style="font-size:18px;font-weight:700;color:#e6a23c;">¥{{ formatMoney(currentMember.totalConsume) }}</div>
            </div>
            <div style="background:#f5f7fa;padding:14px;border-radius:8px;">
              <div style="font-size:11px;color:#909399;margin-bottom:4px;">📋 关联订单</div>
              <div style="font-size:18px;font-weight:700;">{{ currentMember.orderCount || 0 }} 单</div>
            </div>
            <div style="background:#f5f7fa;padding:12px;border-radius:8px;">
              <div style="font-size:11px;color:#909399;margin-bottom:4px;">联系电话</div>
              <div style="font-size:14px;font-weight:600;">{{ currentMember.phone }}</div>
            </div>
            <div :style="{ background: getLevelStyle(currentMember.level).background, padding: '12px', borderRadius: '8px' }">
              <div :style="{ fontSize: '11px', color: getLevelStyle(currentMember.level).color, marginBottom: '4px' }">享受折扣</div>
              <div :style="{ fontSize: '14px', fontWeight: 700, color: getLevelStyle(currentMember.level).color }">{{ getLevelDiscount(currentMember.level) }}</div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-success" @click="detailVisible=false;openRecharge(currentMember!)">充值</button>
          <button class="btn btn-primary" @click="detailVisible=false;openEdit(currentMember!)">编辑</button>
          <button class="btn btn-default" @click="detailVisible=false;openOrders(currentMember!)">关联订单</button>
          <button class="btn btn-default" @click="detailVisible=false">关闭</button>
        </div>
      </div>
    </div>

    <!-- 充值弹窗 -->
    <div class="modal-overlay" :class="{ show: rechargeVisible }" @click.self="rechargeVisible=false">
      <div class="modal" style="max-width:520px;">
        <div class="modal-header"><h3 class="modal-title">💳 会员充值</h3><button class="modal-close" @click="rechargeVisible=false">×</button></div>
        <div class="modal-body">
          <div v-if="currentMember" style="background:linear-gradient(135deg,#409eff,#66b1ff);padding:16px 20px;border-radius:10px;margin-bottom:18px;color:#fff;">
            <div style="font-size:12px;opacity:0.85;margin-bottom:4px;">当前预存余额</div>
            <div style="font-size:26px;font-weight:700;">¥ {{ formatMoney(currentMember.balance) }}</div>
            <div style="font-size:12px;opacity:0.8;margin-top:4px;"><span class="level-badge" style="background:rgba(255,255,255,0.2);color:#fff;">{{ getLevelIcon(currentMember.level) }} {{ getLevelName(currentMember.level) }}</span></div>
          </div>
          <div class="form-row">
            <div class="form-group"><label class="form-label">充值金额 *</label><input v-model.number="rechargeForm.amount" type="number" class="form-input" placeholder="请输入充值金额" @input="calcBonus" /></div>
            <div class="form-group"><label class="form-label">充值方式</label>
              <select v-model="rechargeForm.method" class="form-input"><option value="wechat">微信支付</option><option value="alipay">支付宝</option><option value="transfer">银行转账</option><option value="cash">现金</option><option value="other">其他</option></select>
            </div>
          </div>
          <div style="background:#f0f9eb;padding:12px 14px;border-radius:8px;margin-bottom:12px;">
            <div style="display:flex;justify-content:space-between;font-size:13px;margin-bottom:6px;"><span style="color:#606266;">充值金额</span><span style="font-weight:600;">¥ {{ (rechargeForm.amount || 0).toFixed(2) }}</span></div>
            <div style="display:flex;justify-content:space-between;font-size:13px;margin-bottom:6px;"><span style="color:#606266;">赠送金额{{ currentMember ? ' (' + getLevelDiscount(currentMember.level) + '会员)' : '' }}</span><span style="color:#67c23a;font-weight:600;">¥ {{ bonusAmount.toFixed(2) }}</span></div>
            <div style="display:flex;justify-content:space-between;font-size:14px;border-top:1px dashed #dcdfe6;padding-top:8px;"><span style="font-weight:600;">到账金额</span><span style="font-weight:700;color:#67c23a;font-size:16px;">¥ {{ totalAmount.toFixed(2) }}</span></div>
          </div>
          <div class="form-group"><label class="form-label">充值备注</label><input v-model="rechargeForm.remark" class="form-input" placeholder="如：季度充值、指定项目专用（选填）" /></div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="rechargeVisible=false">取消</button>
          <button class="btn btn-success" @click="submitRecharge">✅ 确认充值</button>
        </div>
      </div>
    </div>

    <!-- 关联订单弹窗 -->
    <div class="modal-overlay" :class="{ show: ordersVisible }" @click.self="ordersVisible=false">
      <div class="modal" style="max-width:600px;">
        <div class="modal-header"><h3 class="modal-title">📋 关联订单 — {{ currentMember?.memberName }}</h3><button class="modal-close" @click="ordersVisible=false">×</button></div>
        <div class="modal-body">
          <div style="margin-bottom:12px;display:flex;justify-content:space-between;align-items:center;">
            <span style="font-size:13px;color:#606266;">共 {{ currentMember?.orderCount || 0 }} 个关联订单</span>
          </div>
          <table>
            <thead><tr><th>订单编号</th><th>订单名称</th><th>金额</th><th>状态</th><th>创建时间</th></tr></thead>
            <tbody>
              <tr v-if="memberOrders.length === 0"><td colspan="5" style="text-align:center;color:#909399;padding:20px;">暂无关联订单</td></tr>
              <tr v-for="o in memberOrders" :key="o.id">
                <td style="color:var(--primary,#409eff);cursor:pointer;">#{{ o.orderNo || o.id }}</td>
                <td>{{ o.orderName || '-' }}</td>
                <td style="color:#67c23a;font-weight:600;">¥{{ formatMoney(o.amount) }}</td>
                <td><span :class="'status-tag status-' + o.status">{{ getStatusName(o.status) }}</span></td>
                <td>{{ fmtDate(o.createTime) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="modal-footer"><button class="btn btn-default" @click="ordersVisible=false">关闭</button></div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { memberApi } from '@/api'

const list = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 20

const searchForm = reactive({ keyword: '', level: '', status: '' as any })
const stats = reactive({ total: 0, active: 0, totalBalance: '0', totalOrders: 0 })

const levelOptions = [
  { label: '钻石会员', value: 'diamond' },
  { label: '金牌会员', value: 'gold' },
  { label: '银牌会员', value: 'silver' },
  { label: '普通会员', value: 'normal' },
]

const levelConfig: Record<string, any> = {
  diamond: { color: '#f56c6c', bg: '#fef0f0', icon: '💎', name: '钻石会员', discount: 8.5 },
  gold:    { color: '#e6a23c', bg: '#fdf6ec', icon: '🌟', name: '金牌会员', discount: 9.0 },
  silver:  { color: '#409eff', bg: '#ecf5ff', icon: '⭐', name: '银牌会员', discount: 9.5 },
  normal:  { color: '#909399', bg: '#f5f7fa', icon: '🥉', name: '普通会员', discount: 10.0 },
}
function getLevelStyle(level: string) { const c = levelConfig[level] || levelConfig.normal; return { background: c.bg, color: c.color } }
function getLevelIcon(level: string) { return (levelConfig[level] || levelConfig.normal).icon }
function getLevelName(level: string) { return (levelConfig[level] || levelConfig.normal).name }
function getLevelDiscount(level: string) { return (levelConfig[level] || levelConfig.normal).discount + '折' }

function formatMoney(v: any) {
  return Number(v || 0).toLocaleString('zh', { minimumFractionDigits: 2 })
}
function fmtDate(d: any) {
  if (!d) return '-'
  return typeof d === 'string' ? d.slice(0, 10) : d.toISOString?.().slice(0, 10) || String(d).slice(0, 10)
}

function getStatusName(s: number | string) {
  const m: Record<string, string> = { '1': '待确认', '2': '设计中', '3': '生产中', '4': '配送中', '5': '已完成', '0': '已取消' }
  return m[String(s)] || String(s)
}

const totalPages = computed(() => Math.ceil(total.value / pageSize))
const pageRange = computed(() => {
  const p = page.value, t = totalPages.value
  let arr = []
  for (let i = Math.max(1, p - 2); i <= Math.min(t, p + 2); i++) arr.push(i)
  return arr
})

// 弹窗状态
const currentMember = ref<any>(null)
const addVisible = ref(false)
const editVisible = ref(false)
const detailVisible = ref(false)
const rechargeVisible = ref(false)
const ordersVisible = ref(false)
const memberOrders = ref<any[]>([])

const addForm = reactive({ memberName: '', phone: '', level: 'normal', balance: 0, contactPerson: '', remark: '' })
const editForm = reactive<any>({})
const rechargeForm = reactive({ amount: 0, method: 'wechat', remark: '' })
const bonusAmount = ref(0)
const totalAmount = ref(0)

function calcBonus() {
  const amt = rechargeForm.amount || 0
  const level = currentMember.value?.level || 'normal'
  const bonusRate = level === 'diamond' ? 0.06 : level === 'gold' ? 0.04 : level === 'silver' ? 0.02 : 0
  bonusAmount.value = Math.floor(amt * bonusRate * 100) / 100
  totalAmount.value = amt + bonusAmount.value
}

function openAdd() { Object.assign(addForm, { memberName: '', phone: '', level: 'normal', balance: 0, contactPerson: '', remark: '' }); addVisible.value = true }
function openEdit(m: any) { Object.assign(editForm, m); editVisible.value = true }
function openDetail(m: any) { currentMember.value = m; detailVisible.value = true }
function openRecharge(m: any) { currentMember.value = m; Object.assign(rechargeForm, { amount: 0, method: 'wechat', remark: '' }); calcBonus(); rechargeVisible.value = true }
function openOrders(m: any) { currentMember.value = m; loadOrders(m.id); ordersVisible.value = true }

async function loadOrders(memberId: number) {
  try { const r = await memberApi.getTransactions(memberId); memberOrders.value = r.data || [] } catch { memberOrders.value = [] }
}

async function submitAdd() {
  if (!addForm.memberName.trim()) { ElMessage.warning('请输入会员名称！'); return }
  if (!addForm.phone.trim()) { ElMessage.warning('请输入联系电话！'); return }
  try {
    const data: Record<string, any> = { memberName: addForm.memberName, phone: addForm.phone, level: addForm.level }
    if (addForm.contactPerson) data.contactPerson = addForm.contactPerson
    if (addForm.balance > 0) data.balance = addForm.balance
    if (addForm.remark) data.remark = addForm.remark
    await memberApi.create(data)
    ElMessage.success(`会员「${addForm.memberName}」创建成功！`)
    addVisible.value = false
    load()
  } catch { ElMessage.error('创建失败') }
}

async function submitEdit() {
  try {
    const data: Record<string, any> = { memberName: editForm.memberName, phone: editForm.phone, level: editForm.level, status: editForm.status }
    await memberApi.update(editForm.id, data)
    ElMessage.success('会员信息已更新！')
    editVisible.value = false
    load()
  } catch { ElMessage.error('更新失败') }
}

async function submitRecharge() {
  const amt = rechargeForm.amount
  if (!amt || amt <= 0) { ElMessage.warning('请输入有效的充值金额！'); return }
  if (!currentMember.value) { ElMessage.warning('请先选择会员！'); return }
  try {
    const remark = rechargeForm.remark || `充值（${rechargeForm.method}）`
    await memberApi.recharge(currentMember.value.id, { amount: amt, remark })
    ElMessage.success(`充值成功！到账金额 ¥${totalAmount.value}`)
    rechargeVisible.value = false
    load()
  } catch { ElMessage.error('充值失败') }
}

function handleSearch() { page.value = 1; load() }
function exportData() { ElMessage.success('导出功能开发中') }

async function load() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, size: pageSize }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.level) params.level = searchForm.level
    if (searchForm.status !== '' && searchForm.status !== undefined) params.status = searchForm.status
    const r = await memberApi.getList(params)
    list.value = r.data?.records || r.data?.list || []
    total.value = r.data?.total || list.value.length
    // 统计
    stats.total = total.value
    stats.active = list.value.filter((m: any) => m.status === 1).length
    stats.totalBalance = list.value.reduce((s: number, m: any) => s + Number(m.balance || 0), 0).toLocaleString('zh', { minimumFractionDigits: 2 })
    stats.totalOrders = list.value.reduce((s: number, m: any) => s + Number(m.orderCount || 0), 0)
  } catch { list.value = [] }
  finally { loading.value = false }
}

onMounted(load)
</script>

<style scoped lang="scss">
/* 会员列表表格优化 */
:deep(.member-table) {
  thead th {
    padding: 13px 16px;
    font-size: 12px;
    font-weight: 600;
    color: #909399;
    background: #fafbfc;
    border-bottom: 1px solid #ebeef5;
    text-align: left;
    white-space: nowrap;
    letter-spacing: 0.3px;
    position: sticky;
    top: 0;
  }
  tbody td {
    padding: 16px 16px;
    font-size: 13px;
    color: #303133;
    border-bottom: 1px solid #f2f3f5;
    vertical-align: middle;
    line-height: 1.5;
  }
  tbody tr {
    transition: background 0.15s;
  }
  tbody tr:hover td {
    background: #f8faff;
  }
  tbody tr:last-child td {
    border-bottom: none;
  }
}

/* 等级徽章 */
:deep(.level-badge) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

/* 操作按钮优化 */
:deep(.action-btns) {
  display: flex;
  gap: 6px;
  align-items: center;
}

:deep(.action-btn) {
  padding: 5px 12px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;

  &.view {
    background: #ecf5ff;
    color: #409eff;
    &:hover { background: #d9ecff; }
  }
  &.recharge {
    background: #f0f9eb;
    color: #67c23a;
    &:hover { background: #e1f3d8; }
  }
  &.edit {
    background: #fdf6ec;
    color: #e6a23c;
    &:hover { background: #faecd8; }
  }
  &.delete {
    background: #fef0f0;
    color: #f56c6c;
    &:hover { background: #fde2e2; }
  }
}

/* 会员名称+状态横向排列 */
:deep(.member-info-cell) {
  display: flex;
  align-items: center;
  gap: 8px;
}
:deep(.member-name) {
  font-weight: 600;
  color: #303133;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}

/* 卡片内间距 */
:deep(.card) {
  overflow: hidden;
  padding: 0;

  table {
    width: 100%;
    border-collapse: collapse;
  }
}

/* 分页区域 */
:deep(.pagination) {
  padding: 16px 20px;
}
</style>
