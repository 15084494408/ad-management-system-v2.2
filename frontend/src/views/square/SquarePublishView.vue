<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">📢 需求发布</span>
      <div class="page-actions">
        <el-button type="primary" @click="showDialog()"><el-icon><Plus /></el-icon> 发布需求</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">📢</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalDemands }}</div>
          <div class="stat-label">需求总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ completedCount }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">⏳</div>
        <div class="stat-info">
          <div class="stat-value">{{ inProgressCount }}</div>
          <div class="stat-label">进行中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">🔔</div>
        <div class="stat-info">
          <div class="stat-value">{{ openCount }}</div>
          <div class="stat-label">待接单</div>
        </div>
      </div>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="需求标题">
          <el-input v-model="searchForm.title" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px;">
            <el-option label="待接单" value="open" />
            <el-option label="进行中" value="in_progress" />
            <el-option label="已完成" value="completed" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable style="width:120px;">
            <el-option label="Logo设计" value="logo" />
            <el-option label="海报设计" value="poster" />
            <el-option label="VI设计" value="vi" />
            <el-option label="包装设计" value="package" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="demand-grid" v-loading="loading">
      <div v-for="item in tableData" :key="item.id" class="demand-card card">
        <div class="demand-header">
          <span class="demand-type">{{ { logo:'Logo设计', poster:'海报设计', vi:'VI设计', package:'包装设计' }[item.type] }}</span>
          <el-tag :type="statusMap[item.status]" size="small">{{ { open:'待接单', in_progress:'进行中', completed:'已完成', closed:'已关闭' }[item.status] }}</el-tag>
        </div>
        <h3 class="demand-title">{{ item.title }}</h3>
        <p class="demand-desc">{{ item.description }}</p>
        <div class="demand-meta">
          <span>💰 预算: ¥{{ item.budget?.toLocaleString() }}</span>
          <span>⏰ 截止: {{ item.deadline }}</span>
          <span>👤 发布人: {{ item.publisherName }}</span>
        </div>
        <div class="demand-actions">
          <el-button v-if="item.status === 'open'" type="primary" size="small" @click="acceptDemand(item)">接单</el-button>
          <el-button size="small" @click="viewDetail(item)">查看详情</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="发布设计需求" width="600px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="需求标题"><el-input v-model="formData.title" placeholder="请输入" /></el-form-item>
        <el-form-item label="需求类型">
          <el-select v-model="formData.type" style="width:100%;">
            <el-option label="Logo设计" value="logo" />
            <el-option label="海报设计" value="poster" />
            <el-option label="VI设计" value="vi" />
            <el-option label="包装设计" value="package" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细描述"><el-input v-model="formData.description" type="textarea" :rows="4" placeholder="请描述需求细节" /></el-form-item>
        <el-form-item label="预算金额"><el-input-number v-model="formData.budget" :min="0" :precision="2" style="width:100%;" /></el-form-item>
        <el-form-item label="截止日期"><el-date-picker v-model="formData.deadline" type="date" value-format="YYYY-MM-DD" style="width:100%;" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const dialogVisible = ref(false)
const totalDemands = ref(0)
const completedCount = ref(0)
const inProgressCount = ref(0)
const openCount = ref(0)
const tableData = ref<any[]>([])
const searchForm = ref({ title: '', status: '', type: '' })
const formData = reactive({ title: '', type: 'logo', description: '', budget: 0, deadline: '' })
const statusMap: Record<string, string> = { open: 'warning', in_progress: 'primary', completed: 'success', closed: 'info' }

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/square/requirement', { params: searchForm.value })
    tableData.value = res.data?.records || res.data || []
  } catch {
    ElMessage.error('加载数据失败')
  }
  loading.value = false
}

function resetSearch() { searchForm.value = { title: '', status: '', type: '' }; loadData() }
function showDialog() { dialogVisible.value = true }
function acceptDemand(item: any) { ElMessage.success('已接单: ' + item.title) }
function viewDetail(item: any) { ElMessage.info('查看详情: ' + item.title) }
async function submitForm() {
  try { await request.post('/square/requirement', formData); ElMessage.success('发布成功'); dialogVisible.value = false; loadData() } catch {}
}
onMounted(loadData)
</script>

<style scoped lang="scss">
.demand-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px; }
.demand-card { padding: 20px; }
.demand-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.demand-type { font-size: 12px; color: #909399; }
.demand-title { font-size: 16px; font-weight: 600; margin-bottom: 8px; }
.demand-desc { font-size: 13px; color: #606266; margin-bottom: 12px; line-height: 1.5; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.demand-meta { display: flex; flex-wrap: wrap; gap: 15px; font-size: 12px; color: #909399; margin-bottom: 12px; }
.demand-actions { display: flex; gap: 8px; }
</style>
