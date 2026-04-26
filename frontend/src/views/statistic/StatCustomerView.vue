<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">👥 客户统计</span>
      <div class="page-actions">
        <el-button @click="exportData">
          <el-icon><Download /></el-icon> 导出报表
        </el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">👥</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalCustomers }}</div>
          <div class="stat-label">客户总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">🆕</div>
        <div class="stat-info">
          <div class="stat-value">{{ newCustomers }}</div>
          <div class="stat-label">本月新增</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">💰</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ totalConsumption?.toLocaleString() }}</div>
          <div class="stat-label">累计消费</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">⭐</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ avgConsumption?.toFixed(0) }}</div>
          <div class="stat-label">平均消费</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header-row">
        <span class="card-title">🏆 客户消费排行 TOP20</span>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="rank" label="排名" width="80">
          <template #default="{ $index }">
            <span :class="getRankClass($index + 1)">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="客户名称" min-width="180">
          <template #default="{ row }">
            <div class="customer-info">
              <span class="customer-avatar">{{ row.name?.charAt(0) }}</span>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="客户等级" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="getLevelType(row.level)">{{ row.levelName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="100" />
        <el-table-column prop="totalAmount" label="累计消费" width="150">
          <template #default="{ row }">
            <span class="amount">¥{{ row.totalAmount?.toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastOrderDate" label="最近订单" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '活跃' : '沉默' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card">
      <div class="card-header-row">
        <span class="card-title">📊 客户等级分布</span>
      </div>
      <div class="level-distribution">
        <div v-for="item in levelData" :key="item.name" class="level-item">
          <div class="level-bar" :style="{ width: `${item.percent}%`, background: item.color }"></div>
          <div class="level-info">
            <span class="level-name">{{ item.name }}</span>
            <span class="level-count">{{ item.count }}人 ({{ item.percent }}%)</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const tableData = ref<any[]>([])
const levelData = ref<any[]>([])

const totalCustomers = ref(0)
const newCustomers = ref(0)
const totalConsumption = computed(() => tableData.value.reduce((sum, t) => sum + (t.totalAmount || 0), 0))
const avgConsumption = computed(() => {
  const total = totalConsumption.value
  const count = totalCustomers.value
  return count > 0 ? total / count : 0
})

const getRankClass = (rank: number) => {
  if (rank === 1) return 'rank-gold'
  if (rank === 2) return 'rank-silver'
  if (rank === 3) return 'rank-bronze'
  return ''
}

const getLevelType = (level: number) => {
  if (level >= 90) return 'danger'
  if (level >= 70) return 'warning'
  if (level >= 50) return 'success'
  return 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/statistics/customer/summary')
    const data = res.data || {}
    tableData.value = data.list || data.records || []
    totalCustomers.value = data.totalCustomers || 0
    newCustomers.value = data.newCustomers || 0
    levelData.value = data.levelDistribution || []
  } catch {
    tableData.value = []
    totalCustomers.value = 0
    newCustomers.value = 0
    levelData.value = []
  } finally {
    loading.value = false
  }
}

const exportData = () => {
  ElMessage.info('导出功能开发中')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

.customer-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.customer-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.amount {
  color: #e6a23c;
  font-weight: 600;
}

.rank-gold {
  color: #ffd700;
  font-weight: 700;
  font-size: 16px;
}

.rank-silver {
  color: #c0c0c0;
  font-weight: 700;
  font-size: 16px;
}

.rank-bronze {
  color: #cd7f32;
  font-weight: 700;
  font-size: 16px;
}

.level-distribution {
  padding: 20px;
}

.level-item {
  margin-bottom: 20px;
}

.level-bar {
  height: 24px;
  border-radius: 12px;
  transition: width 0.6s ease;
  margin-bottom: 8px;
  max-width: 100%;
}

.level-info {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.level-name {
  color: #303133;
  font-weight: 500;
}

.level-count {
  color: #909399;
}
</style>
