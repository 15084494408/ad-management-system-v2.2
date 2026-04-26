<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">每日流水</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">💵 流水记录 <span class="v2-badge">V2.2</span></h1>
      <div class="page-actions">
        <button class="btn btn-default">⬇️ 导出</button>
        <button class="btn btn-primary" @click="showQuickRecord">+ 新增流水</button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon green">💵</div>
        <div class="stat-info">
          <h3>¥{{ todayTotal?.toLocaleString() || 0 }}</h3>
          <p>今日流水</p>
          <div class="stat-trend up">↑ ¥320 较昨日</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon blue">📊</div>
        <div class="stat-info">
          <h3>{{ todayCount || 0 }}</h3>
          <p>今日笔数</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">📈</div>
        <div class="stat-info">
          <h3>¥{{ avgAmount || 0 }}</h3>
          <p>平均每笔</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">🏆</div>
        <div class="stat-info">
          <h3>¥{{ maxAmount || 0 }}</h3>
          <p>最高单笔</p>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-form">
      <div class="form-group">
        <label>日期范围</label>
        <input type="date" v-model="searchForm.startDate" class="form-control">
      </div>
      <div class="form-group">
        <label>流水类型</label>
        <select v-model="searchForm.type" class="form-control">
          <option value="">全部</option>
          <option value="print">图文打印</option>
          <option value="copy">复印扫描</option>
          <option value="binding">装订</option>
          <option value="ad">广告制作</option>
          <option value="other">其他</option>
        </select>
      </div>
      <div class="form-group">
        <label>支付方式</label>
        <select v-model="searchForm.paymentMethod" class="form-control">
          <option value="">全部</option>
          <option value="cash">现金</option>
          <option value="wechat">微信</option>
          <option value="alipay">支付宝</option>
          <option value="bank">银行卡</option>
        </select>
      </div>
      <div class="form-group" style="align-self:flex-end;">
        <button class="btn btn-primary" @click="loadData">🔍 搜索</button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="card">
      <table class="data-table">
        <thead>
          <tr>
            <th>流水编号</th>
            <th>金额</th>
            <th>类型</th>
            <th>支付方式</th>
            <th>备注</th>
            <th>记账人</th>
            <th>发生时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in tableData" :key="record.id">
            <td>{{ record.recordNo || 'LF' + record.id }}</td>
            <td :class="record.direction === 'income' ? 'income-amount' : 'expense-amount'">
              {{ record.direction === 'income' ? '+' : '-' }}¥{{ record.amount?.toLocaleString() || 0 }}
            </td>
            <td>
              <span class="tag" :class="'tag-' + (record.type || 'primary')">{{ record.category || '图文打印' }}</span>
            </td>
            <td>
              <span class="tag" :class="'tag-' + getPaymentTag(record.paymentMethod)">{{ getPaymentLabel(record.paymentMethod) }}</span>
            </td>
            <td>{{ record.remark || '-' }}</td>
            <td>{{ record.createBy || '系统' }}</td>
            <td>{{ record.createTime }}</td>
            <td class="action-btns">
              <button class="action-btn view" @click="showDetail(record)">详情</button>
              <button class="action-btn edit" @click="showEdit(record)">修改</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="pagination">
        <div class="pagination-info">共 {{ total }} 笔记录</div>
        <div class="pagination-buttons">
          <button class="page-btn" :disabled="currentPage <= 1" @click="currentPage--; loadData()">«</button>
          <button class="page-btn" v-for="p in pageNumbers" :key="p" :class="{ active: currentPage === p }" @click="currentPage = p; loadData()">{{ p }}</button>
          <button class="page-btn" :disabled="currentPage >= totalPages" @click="currentPage++; loadData()">»</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { financeApi } from '@/api/modules/finance'
import { ElMessage } from 'element-plus'

const router = useRouter()

const todayTotal = ref(0)
const todayCount = ref(0)
const avgAmount = ref(0)
const maxAmount = ref(0)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const tableData = ref<any[]>([])
const loading = ref(false)

const searchForm = reactive({
  startDate: '',
  type: '',
  paymentMethod: ''
})

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
const pageNumbers = computed(() => {
  const pages = []
  const maxPages = Math.min(totalPages.value, 5)
  for (let i = 1; i <= maxPages; i++) {
    pages.push(i)
  }
  return pages
})

function getPaymentLabel(method: string) {
  const map: Record<string, string> = {
    cash: '现金', wechat: '微信', alipay: '支付宝', bank: '银行卡'
  }
  return map[method] || method
}

function getPaymentTag(method: string) {
  const map: Record<string, string> = {
    cash: 'warning', wechat: 'success', alipay: 'primary', bank: 'default'
  }
  return map[method] || 'default'
}

async function loadData() {
  loading.value = true
  try {
    const res = await financeApi.getRecords({
      current: currentPage.value,
      size: pageSize.value,
      type: searchForm.type,
      paymentMethod: searchForm.paymentMethod
    })
    if (res.code === 200) {
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function showQuickRecord() {
  window.dispatchEvent(new CustomEvent('show-quick-account'))
}

function showDetail(record: any) {
  ElMessage.info('查看详情: ' + record.id)
}

function showEdit(record: any) {
  ElMessage.info('编辑: ' + record.id)
}

function exportData() {
  ElMessage.success('导出成功')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.page-container { padding: 20px 24px; }

.income-amount { color: #67c23a; font-weight: 600; }
.expense-amount { color: #f56c6c; font-weight: 600; }

.tag {
  display: inline-block; padding: 3px 8px; border-radius: 4px;
  font-size: 11px; margin-right: 5px;
}
.tag-primary { background: #ecf5ff; color: $primary; }
.tag-success { background: #f0f9eb; color: $success; }
.tag-warning { background: #fdf6ec; color: $warning; }
.tag-danger { background: #fef0f0; color: $danger; }
.tag-default { background: #f4f4f5; color: #909399; }
</style>