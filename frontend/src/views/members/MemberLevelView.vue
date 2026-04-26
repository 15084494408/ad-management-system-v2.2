<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item" @click="$router.push('/members')">会员管理</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item active">会员等级</span>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">🏆 会员等级配置</h1>
      <div class="page-actions">
        <button class="btn btn-primary" @click="openAddLevel">+ 新增等级</button>
      </div>
    </div>

    <!-- 等级卡片网格 -->
    <div class="level-grid">
      <div v-for="(cfg, name) in levelConfig" :key="name" class="level-card" :style="{ border: '2px solid ' + cfg.bg }">
        <div class="level-card-header">
          <div style="display:flex;align-items:center;gap:10px;">
            <span style="font-size:28px;">{{ cfg.icon }}</span>
            <div>
              <div style="font-size:16px;font-weight:700;" :style="{ color: cfg.color }">{{ name }}</div>
              <div style="font-size:12px;color:#909399;">共 {{ getMemberCount(cfg.key) }} 名会员</div>
            </div>
          </div>
          <button class="btn btn-default btn-sm" @click="openEditLevel(name)">编辑</button>
        </div>
        <div class="level-card-body">
          <div class="level-stat-item" :style="{ background: cfg.bg }">
            <div class="level-stat-label" :style="{ color: cfg.color }">升级条件</div>
            <div class="level-stat-value">累计消费 ≥ ¥{{ (cfg.minAmount / 10000).toFixed(0) }}万</div>
          </div>
          <div class="level-stat-item" :style="{ background: cfg.bg }">
            <div class="level-stat-label" :style="{ color: cfg.color }">享受折扣</div>
            <div class="level-stat-value">{{ cfg.discount }}折</div>
          </div>
          <div class="level-stat-item" style="background:#f5f7fa;">
            <div class="level-stat-label" style="color:#909399;">预存余额合计</div>
            <div class="level-stat-value" style="color:#67c23a;">¥{{ getLevelBalance(cfg.key) }}</div>
          </div>
          <div class="level-stat-item" style="background:#f5f7fa;">
            <div class="level-stat-label" style="color:#909399;">会员权益</div>
            <div class="level-stat-value" style="color:#409eff;">充值送 {{ cfg.bonusPercent }}%</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 升级规则说明 -->
    <div class="card">
      <div class="card-header"><div class="card-title">📋 升级规则说明</div></div>
      <div style="padding:16px;">
        <div class="rules-grid">
          <div class="rule-card" style="background:#f5f7fa;">
            <div style="font-weight:600;margin-bottom:8px;color:#909399;">🥉 普通会员</div>
            <ul style="padding-left:16px;color:#606266;line-height:2;font-size:13px;">
              <li>注册即享受</li><li>原价消费</li><li>无赠送金额</li>
            </ul>
          </div>
          <div class="rule-card" style="background:#ecf5ff;">
            <div style="font-weight:600;margin-bottom:8px;color:#409eff;">⭐ 银牌会员</div>
            <ul style="padding-left:16px;color:#606266;line-height:2;font-size:13px;">
              <li>累计消费≥¥2万</li><li>9.5折优惠</li><li>充值赠送2%</li>
            </ul>
          </div>
          <div class="rule-card" style="background:#fdf6ec;">
            <div style="font-weight:600;margin-bottom:8px;color:#e6a23c;">🌟 金牌会员</div>
            <ul style="padding-left:16px;color:#606266;line-height:2;font-size:13px;">
              <li>累计消费≥¥5万</li><li>9折优惠</li><li>充值赠送4%</li>
            </ul>
          </div>
          <div class="rule-card" style="background:#fef0f0;">
            <div style="font-weight:600;margin-bottom:8px;color:#f56c6c;">💎 钻石会员</div>
            <ul style="padding-left:16px;color:#606266;line-height:2;font-size:13px;">
              <li>累计消费≥¥10万</li><li>8.5折优惠</li><li>充值赠送6%</li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增等级弹窗 -->
    <div class="modal-overlay" :class="{ show: addLevelVisible }" @click.self="addLevelVisible=false">
      <div class="modal">
        <div class="modal-header"><h3 class="modal-title">🏆 新增会员等级</h3><button class="modal-close" @click="addLevelVisible=false">×</button></div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group"><label class="form-label">等级名称 *</label><input v-model="levelForm.name" class="form-input" placeholder="如：铂金会员" /></div>
            <div class="form-group"><label class="form-label">等级图标</label><input v-model="levelForm.icon" class="form-input" placeholder="如：🏅" /></div>
          </div>
          <div class="form-row">
            <div class="form-group"><label class="form-label">升级条件（累计消费）</label><input v-model.number="levelForm.minAmount" type="number" class="form-input" placeholder="0" /></div>
            <div class="form-group"><label class="form-label">享受折扣（如9.5）</label><input v-model.number="levelForm.discount" type="number" class="form-input" step="0.5" min="1" max="10" placeholder="10" /></div>
          </div>
          <div class="form-row">
            <div class="form-group"><label class="form-label">充值赠送比例(%)</label><input v-model.number="levelForm.bonusPercent" type="number" class="form-input" placeholder="0" /></div>
            <div class="form-group"><label class="form-label">排序</label><input v-model.number="levelForm.sort" type="number" class="form-input" placeholder="1" /></div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="addLevelVisible=false">取消</button>
          <button class="btn btn-primary" @click="submitAddLevel">✅ 确认添加</button>
        </div>
      </div>
    </div>

    <!-- 编辑等级弹窗 -->
    <div class="modal-overlay" :class="{ show: editLevelVisible }" @click.self="editLevelVisible=false">
      <div class="modal">
        <div class="modal-header"><h3 class="modal-title">✏️ 编辑等级 — {{ editingLevelName }}</h3><button class="modal-close" @click="editLevelVisible=false">×</button></div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group"><label class="form-label">等级名称</label><input :value="editingLevelName" class="form-input" readonly style="background:#f5f7fa;cursor:not-allowed;" /></div>
            <div class="form-group"><label class="form-label">等级图标</label><input v-model="editLevelForm.icon" class="form-input" /></div>
          </div>
          <div class="form-row">
            <div class="form-group"><label class="form-label">升级条件（累计消费）</label><input v-model.number="editLevelForm.minAmount" type="number" class="form-input" /></div>
            <div class="form-group"><label class="form-label">享受折扣</label><input v-model.number="editLevelForm.discount" type="number" class="form-input" step="0.5" /></div>
          </div>
          <div class="form-group"><label class="form-label">充值赠送比例(%)</label><input v-model.number="editLevelForm.bonusPercent" type="number" class="form-input" /></div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="editLevelVisible=false">取消</button>
          <button class="btn btn-primary" @click="submitEditLevel">💾 保存</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { memberApi } from '@/api'

const members = ref<any[]>([])

const levelConfig = reactive<Record<string, any>>({
  '钻石会员': { color: '#f56c6c', bg: '#fef0f0', icon: '💎', key: 'diamond', minAmount: 100000, discount: 8.5, bonusPercent: 6 },
  '金牌会员': { color: '#e6a23c', bg: '#fdf6ec', icon: '🌟', key: 'gold', minAmount: 50000, discount: 9.0, bonusPercent: 4 },
  '银牌会员': { color: '#409eff', bg: '#ecf5ff', icon: '⭐', key: 'silver', minAmount: 20000, discount: 9.5, bonusPercent: 2 },
  '普通会员': { color: '#909399', bg: '#f5f7fa', icon: '🥉', key: 'normal', minAmount: 0, discount: 10.0, bonusPercent: 0 },
})

function getMemberCount(levelKey: string) {
  return members.value.filter((m: any) => m.level === levelKey).length
}

function getLevelBalance(levelKey: string) {
  const total = members.value.filter((m: any) => m.level === levelKey).reduce((s: number, m: any) => s + Number(m.balance || 0), 0)
  return total.toLocaleString('zh', { minimumFractionDigits: 2 })
}

const addLevelVisible = ref(false)
const editLevelVisible = ref(false)
const editingLevelName = ref('')
const levelForm = reactive({ name: '', icon: '🏅', minAmount: 0, discount: 10, bonusPercent: 0, sort: 1 })
const editLevelForm = reactive({ icon: '', minAmount: 0, discount: 10, bonusPercent: 0 })

function openAddLevel() { Object.assign(levelForm, { name: '', icon: '🏅', minAmount: 0, discount: 10, bonusPercent: 0, sort: 1 }); addLevelVisible.value = true }
function openEditLevel(name: string) {
  editingLevelName.value = name
  const cfg = levelConfig[name]
  if (cfg) Object.assign(editLevelForm, { icon: cfg.icon, minAmount: cfg.minAmount, discount: cfg.discount, bonusPercent: cfg.bonusPercent })
  editLevelVisible.value = true
}

async function submitAddLevel() {
  if (!levelForm.name.trim()) { ElMessage.warning('请输入等级名称！'); return }
  ElMessage.success('等级已添加！')
  levelConfig[levelForm.name] = { color: '#409eff', bg: '#ecf5ff', icon: levelForm.icon || '🏅', key: '', minAmount: levelForm.minAmount, discount: levelForm.discount, bonusPercent: levelForm.bonusPercent }
  addLevelVisible.value = false
}

async function submitEditLevel() {
  const cfg = levelConfig[editingLevelName.value]
  if (cfg) {
    cfg.icon = editLevelForm.icon
    cfg.minAmount = editLevelForm.minAmount
    cfg.discount = editLevelForm.discount
    cfg.bonusPercent = editLevelForm.bonusPercent
  }
  ElMessage.success('等级配置已更新！')
  editLevelVisible.value = false
}

async function loadMembers() {
  try { const r = await memberApi.getList({ size: 999 }); members.value = r.data?.records || r.data?.list || [] } catch { members.value = [] }
}

onMounted(loadMembers)
</script>
<style scoped lang="scss">
.level-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}
.level-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  transition: all 0.3s;
}
.level-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.1);
}
.level-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
}
.level-card-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  font-size: 13px;
}
.level-stat-item {
  padding: 10px;
  border-radius: 6px;
}
.level-stat-label {
  font-size: 11px;
  margin-bottom: 3px;
}
.level-stat-value {
  font-weight: 600;
}
.rules-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
  font-size: 13px;
}
.rule-card {
  padding: 14px;
  border-radius: 8px;
}
@media (max-width: 768px) {
  .rules-grid { grid-template-columns: 1fr; }
  .level-card-body { grid-template-columns: 1fr; }
}
</style>
