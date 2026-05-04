<template>
  <div class="page-container">
    <div class="page-header"><span class="page-title">🏪 设计广场 <el-tag type="success" size="small">V2.1</el-tag></span><div class="page-actions"><el-button type="primary" @click="showPublish=true"><el-icon><Plus /></el-icon> 发布需求</el-button></div></div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="需求类型">
          <el-select v-model="searchForm.category" placeholder="全部" clearable style="width:150px;">
            <el-option label="海报" value="poster" />
            <el-option label="LOGO" value="logo" />
            <el-option label="宣传册" value="brochure" />
            <el-option label="包装" value="package" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算范围">
          <el-select v-model="searchForm.budgetRange" placeholder="全部" clearable style="width:150px;">
            <el-option label="500以下" value="0-500" />
            <el-option label="500-1000" value="500-1000" />
            <el-option label="1000-2000" value="1000-2000" />
            <el-option label="2000以上" value="2000-999999" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">搜索</el-button></el-form-item>
      </el-form>
    </div>

    <div class="square-grid">
      <div v-for="r in tableData" :key="r.id" class="square-card card">
        <div class="square-card-header" :style="{background: categoryColors[r.category]}">
          <div class="square-card-title">{{ r.title }}</div>
          <div class="square-card-type">{{ categoryNames[r.category] }}</div>
        </div>
        <div class="square-card-body">
          <p class="square-desc">{{ r.description }}</p>
          <div class="square-card-info">
            <span>📅 截止：{{ r.deadline?.slice(0,10) }}</span>
            <span class="budget">¥{{ r.budget?.toLocaleString() }}</span>
          </div>
          <div class="square-stats">👁 {{ r.viewCount }} 浏览 · 📥 {{ r.applyCount }} 申请</div>
        </div>
        <div class="square-card-footer">
          <span>{{ timeAgo(r.createTime) }}</span>
          <el-button size="small" type="primary" @click="viewDetail(r)">查看详情</el-button>
        </div>
      </div>
      <el-empty v-if="!tableData.length" description="暂无需求" />
    </div>

    <el-dialog v-model="showPublish" title="发布需求" width="550px">
      <el-form :model="publishForm" label-width="90px">
        <el-form-item label="需求标题"><el-input v-model="publishForm.title" placeholder="请输入需求标题" /></el-form-item>
        <el-form-item label="需求类型">
          <el-select v-model="publishForm.category" style="width:100%;">
            <el-option label="海报设计" value="poster" />
            <el-option label="LOGO设计" value="logo" />
            <el-option label="宣传册设计" value="brochure" />
            <el-option label="包装设计" value="package" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算金额"><el-input-number v-model="publishForm.budget" :min="0" style="width:100%;" /></el-form-item>
        <el-form-item label="截止日期"><el-date-picker v-model="publishForm.deadline" type="date" value-format="YYYY-MM-DD" style="width:100%;" /></el-form-item>
        <el-form-item label="需求描述"><el-input v-model="publishForm.description" type="textarea" rows="4" placeholder="请详细描述设计需求" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showPublish=false">取消</el-button><el-button type="primary" @click="submitPublish">发布</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/api/request'

const showPublish = ref(false)
const tableData = ref<any[]>([])
const searchForm = reactive({ category: '', budgetRange: '' })
const publishForm = reactive({ title:'', category:'poster', budget:0, deadline:'', description:'' })
const categoryNames: Record<string,string> = { poster:'海报设计', logo:'LOGO设计', brochure:'宣传册', package:'包装', other:'其他' }
const categoryColors: Record<string,string> = {
  poster:'linear-gradient(135deg,#409eff,#66b1ff)',
  logo:'linear-gradient(135deg,#9c27b0,#ab47bc)',
  brochure:'linear-gradient(135deg,#67c23a,#85ce61)',
  package:'linear-gradient(135deg,#e6a23c,#ebb563)',
  other:'linear-gradient(135deg,#909399,#a6a9ad)',
}

async function loadData() {
  try {
    const params: any = {}
    if (searchForm.category) params.category = searchForm.category
    const res = await request.get('/square/requirement', { params })
    tableData.value = res.data?.records || []
  } catch { tableData.value = [] }
}
function viewDetail(r: any) { ElMessage.info('查看需求详情：' + r.title) }
async function submitPublish() {
  try { await request.post('/square/requirement', publishForm); ElMessage.success('发布成功'); showPublish.value = false; loadData() } catch { ElMessage.error('发布失败') }
}
function timeAgo(time: string) { if (!time) return ''; const d = new Date(time); const now = new Date(); const diff = Math.floor((now.getTime() - d.getTime()) / 1000); if (diff < 60) return '刚刚'; if (diff < 3600) return Math.floor(diff/60) + '分钟前'; if (diff < 86400) return Math.floor(diff/3600) + '小时前'; return Math.floor(diff/86400) + '天前'; }
onMounted(loadData)
</script>

<style scoped lang="scss">
.square-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
.square-card { overflow: hidden; padding: 0; }
.square-card-header { padding: 15px; color: #fff; }
.square-card-title { font-size: 15px; font-weight: 600; margin-bottom: 5px; }
.square-card-type { font-size: 12px; opacity: 0.9; }
.square-card-body { padding: 15px; }
.square-desc { font-size: 13px; color: $text-secondary; margin-bottom: 15px; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.square-card-info { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13px; }
.budget { color: $warning; font-weight: 600; }
.square-stats { font-size: 12px; color: $text-secondary; }
.square-card-footer { padding: 10px 15px; border-top: 1px solid $border-base; display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: $text-secondary; }
</style>
