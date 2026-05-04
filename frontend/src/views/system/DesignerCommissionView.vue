<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">👔 设计师提成配置</span>
      <div class="page-actions">
        <el-button type="primary" @click="loadData">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <div class="card">
      <div class="info-banner">
        <span class="info-icon">💡</span>
        <span>提成比例默认 <strong>0%</strong>，管理员可按设计师设置不同比例。修改后自动影响该设计师的所有订单。</span>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="designerName" label="设计师" min-width="120">
          <template #default="{ row }">
            <div class="designer-cell">
              <span class="avatar">{{ row.designerName?.charAt(0) || 'D' }}</span>
              <span class="name">{{ row.designerName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="提成比例" width="200">
          <template #default="{ row }">
            <div v-if="editingId === row.designerId" class="rate-edit">
              <el-input-number
                v-model="editingRate"
                :min="0"
                :max="100"
                :precision="2"
                :step="0.5"
                size="default"
                style="width: 120px;"
              />
              <span style="margin-left: 4px; color: #606266;">%</span>
            </div>
            <span v-else class="rate-display" :class="{ zero: row.commissionRate == 0 }">
              {{ row.commissionRate }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :active-value="1"
              :inactive-value="0"
              inline-prompt
              active-text="启用"
              inactive-text="关闭"
              style="--el-switch-on-color: #67c23a; --el-switch-off-color: #dcdfe6;"
              @change="toggleEnabled(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="已分配订单" width="120" align="center">
          <template #default="{ row }">
            <span class="order-count">{{ row.orderCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预估提成（元）" width="150" align="right">
          <template #default="{ row }">
            <span class="commission-amount">
              ¥{{ formatMoney((row.totalAmount || 0) * (row.commissionRate / 100)) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="editingId === row.designerId">
              <el-button type="primary" size="small" @click="saveRate(row)">保存</el-button>
              <el-button size="small" @click="cancelEdit">取消</el-button>
            </template>
            <template v-else>
              <el-button type="primary" link size="small" @click="startEdit(row)">编辑</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 提成说明卡片 -->
    <div class="card" style="margin-top: 16px;">
      <div class="card-title-bar">📖 提成计算说明</div>
      <div class="formula-box">
        <div class="formula">设计师提成 = 订单总额 × 提成比例</div>
        <div class="examples">
          <div class="example">
            <span class="example-label">示例 1：</span>
            <span>订单总额 ¥1,000 × 比例 5% = 提成 ¥50</span>
          </div>
          <div class="example">
            <span class="example-label">示例 2：</span>
            <span>订单总额 ¥5,000 × 比例 3% = 提成 ¥150</span>
          </div>
          <div class="example">
            <span class="example-label">示例 3：</span>
            <span>订单总额 ¥10,000 × 比例 0% = 提成 ¥0（默认）</span>
          </div>
        </div>
        <div class="note">
          ⚠️ 提成仅在订单完成后计入设计师绩效，目前暂不直接从订单金额中扣除。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { designerApi } from '@/api/modules/designer'

const loading = ref(false)
const tableData = ref<any[]>([])
const editingId = ref<number | null>(null)
const editingRate = ref(0)

async function loadData() {
  loading.value = true
  try {
    const res = await designerApi.getCommissionList()
    tableData.value = res.data || []
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function startEdit(row: any) {
  editingId.value = row.designerId
  editingRate.value = Number(row.commissionRate)
}

function cancelEdit() {
  editingId.value = null
  editingRate.value = 0
}

async function saveRate(row: any) {
  try {
    await designerApi.configCommission({
      designerId: row.designerId,
      commissionRate: editingRate.value,
      enabled: row.enabled === 1
    })
    ElMessage.success('提成比例已更新')
    editingId.value = null
    loadData()
  } catch {
    ElMessage.error('保存失败')
  }
}

async function toggleEnabled(row: any) {
  const newEnabled = row.enabled === 1
  const action = newEnabled ? '启用' : '关闭'
  try {
    await designerApi.configCommission({
      designerId: row.designerId,
      commissionRate: Number(row.commissionRate),
      enabled: newEnabled
    })
    ElMessage.success(`${action}成功`)
    loadData()
  } catch {
    row.enabled = row.enabled === 1 ? 0 : 1  // 回滚
    ElMessage.error(`${action}失败`)
  }
}

function formatMoney(v: number) {
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.info-banner {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 8px;
  .info-icon { font-size: 16px; }
  strong { color: #67c23a; }
}

.designer-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  .avatar {
    width: 32px;
    height: 32px;
    background: linear-gradient(135deg, #409eff, #67c23a);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-weight: 700;
    font-size: 14px;
  }
  .name { font-weight: 600; }
}

.rate-display {
  font-weight: 700;
  font-size: 15px;
  color: #409eff;
  &.zero { color: #909399; }
}

.rate-edit {
  display: flex;
  align-items: center;
}

.order-count {
  font-weight: 600;
  color: #606266;
}

.commission-amount {
  font-weight: 600;
  color: #67c23a;
  font-size: 14px;
}

.formula-box {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.formula {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  text-align: center;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  border: 2px dashed #409eff;
  margin-bottom: 16px;
  font-family: monospace;
}

.examples {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.example {
  font-size: 13px;
  color: #606266;
  .example-label {
    font-weight: 600;
    color: #409eff;
    margin-right: 8px;
  }
}

.note {
  font-size: 12px;
  color: #e6a23c;
  padding: 8px 12px;
  background: #fdf6ec;
  border-radius: 6px;
}
</style>
