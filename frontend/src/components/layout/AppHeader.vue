<template>
  <header class="app-header">
    <!-- 左侧：Logo -->
    <div class="header-left">
      <div class="logo">
        <div class="logo-icon">企</div>
        <span class="logo-text">企业版图文广告管理系统 V2.2</span>
      </div>
    </div>

    <!-- 右侧：快速记账 / 通知 / 用户 -->
    <div class="header-right">
      <button class="quick-btn" @click="showQuickAccount = true">
        <span>+</span> 快速记账
      </button>

      <div class="notif-wrapper">
        <button class="notif-btn" @click="showNotif = !showNotif">
          <span class="notif-icon">🔔</span>
          <span class="notif-badge">3</span>
        </button>
      </div>

      <el-dropdown @command="handleUserCmd">
        <div class="user-info">
          <div class="user-avatar">{{ avatarText }}</div>
          <div class="user-meta" v-if="!collapsed">
            <div class="user-name">{{ authStore.userInfo?.realName || authStore.userInfo?.username || '系统管理员' }}</div>
            <div class="user-role">{{ roleDisplay }}</div>
          </div>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">👤 个人信息</el-dropdown-item>
            <el-dropdown-item command="settings">⚙️ 系统设置</el-dropdown-item>
            <el-dropdown-item divided command="logout">🚪 退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>

  <!-- 快速记账弹窗（样式由 prototype-ui.scss 全局提供） -->
  <Teleport to="body">
    <div class="modal-overlay" :class="{ show: showQuickAccount }" @click.self="showQuickAccount = false">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">⚡ 快速记账</h3>
          <button class="modal-close" @click="showQuickAccount = false">×</button>
        </div>
        <div class="modal-body">
          <div class="quick-account-form">
            <input
              v-model="quickForm.amount"
              type="number"
              class="quick-amount"
              placeholder="¥ 0.00"
              step="0.01"
              @keyup.enter="submitQuickAccount"
              ref="quickAmountRef"
            >
            <div class="company-selector-wrapper">
              <select class="quick-select" v-model="quickForm.company" @change="onCompanyChange">
                <option value="" disabled>选择公司/客户...</option>
                <option v-for="c in companyList" :key="c.id" :value="c.name">
                  {{ c.customerType === 2 ? '🏭' : '🏢' }} {{ c.name }}
                </option>
                <option value="new">➕ 新建公司...</option>
              </select>
              <input
                v-if="showNewCompany"
                v-model="quickForm.newCompany"
                type="text"
                class="form-input"
                placeholder="请输入新公司名称"
              >
            </div>
            <select class="quick-select" v-model="quickForm.category">
              <option value="图文类">📄 图文类</option>
              <option value="广告类">🎨 广告类</option>
              <option value="印刷类">🖨️ 印刷类</option>
              <option value="设计费">🎨 设计费</option>
              <option value="其他">📝 其他</option>
            </select>
            <select class="quick-select" v-model="quickForm.paymentMethod">
              <option value="cash">💵 现金</option>
              <option value="wechat">💚 微信</option>
              <option value="alipay">💙 支付宝</option>
              <option value="transfer">💳 银行转账</option>
            </select>
            <input v-model="quickForm.remark" type="text" class="form-input" placeholder="备注说明（可选）">
            <button class="quick-submit" @click="submitQuickAccount" :disabled="submitting">
              {{ submitting ? '保存中...' : '💾 保存记账' }}
            </button>
            <p class="quick-hint">💡 按 Enter 键可快速保存</p>
          </div>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- 通知面板（样式由 prototype-ui.scss 全局提供） -->
  <Teleport to="body">
    <div v-if="showNotif" class="notif-overlay" @click.self="showNotif = false">
      <div class="notif-panel">
        <div class="notif-header">
          <span>系统通知</span>
          <span class="notif-clear" @click="showNotif = false">关闭</span>
        </div>
        <div class="notif-list">
          <div class="notif-item">
            <div class="notif-item-title">新订单待处理</div>
            <div class="notif-item-time">刚刚</div>
          </div>
          <div class="notif-item">
            <div class="notif-item-title">工厂账单已生成</div>
            <div class="notif-item-time">10分钟前</div>
          </div>
          <div class="notif-item">
            <div class="notif-item-title">会员充值成功 ¥500</div>
            <div class="notif-item-time">30分钟前</div>
          </div>
        </div>
        <div class="notif-footer">查看全部通知</div>
      </div>
    </div>
  </Teleport>

  <!-- 个人信息弹窗（样式由 prototype-ui.scss 全局提供） -->
  <Teleport to="body">
    <div class="modal-overlay" :class="{ show: showUserProfile }" @click.self="showUserProfile = false">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">👤 个人信息</h3>
          <button class="modal-close" @click="showUserProfile = false">×</button>
        </div>
        <div class="modal-body">
          <div class="profile-info">
            <div class="profile-avatar">{{ avatarText }}</div>
            <div class="profile-name">{{ authStore.userInfo?.realName || authStore.userInfo?.username || '未知用户' }}</div>
            <div class="profile-role">{{ roleDisplay }}</div>
          </div>
          <div class="profile-details">
            <div class="profile-row">
              <span class="profile-label">用户名</span>
              <span class="profile-value">{{ authStore.userInfo?.username || '-' }}</span>
            </div>
            <div class="profile-row">
              <span class="profile-label">真实姓名</span>
              <span class="profile-value">{{ authStore.userInfo?.realName || '-' }}</span>
            </div>
            <div class="profile-row">
              <span class="profile-label">角色</span>
              <span class="profile-value">{{ roleDisplay }}</span>
            </div>
            <div class="profile-row">
              <span class="profile-label">手机号</span>
              <span class="profile-value">{{ authStore.userInfo?.phone || '-' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useFinanceStore } from '@/stores/finance'
import { financeApi, customerApi } from '@/api'

defineEmits(['toggle-sidebar'])
const props = defineProps<{ collapsed?: boolean }>()

const router = useRouter()
const authStore = useAuthStore()
const financeStore = useFinanceStore()
const showNotif = ref(false)

const roleDisplay = computed(() => {
  const roles = authStore.userInfo?.roles
  if (!roles || roles.length === 0) return '普通用户'
  const roleMap: Record<string, string> = {
    'SUPER_ADMIN': '超级管理员',
    'ADMIN': '管理员',
    'FINANCE': '财务',
    'OPERATOR': '操作员'
  }
  return roles.map(r => roleMap[r] || r).join('/')
})

// 快速记账弹窗状态
const showQuickAccount = ref(false)
const submitting = ref(false)
const quickAmountRef = ref<HTMLInputElement>()
const showNewCompany = ref(false)
const companyList = ref<any[]>([])
const quickForm = ref({
  amount: '',
  company: '',
  newCompany: '',
  category: '图文类',
  paymentMethod: 'cash',
  remark: ''
})

const avatarText = computed(() => {
  const name = authStore.userInfo?.realName || authStore.userInfo?.username || '管'
  return name.slice(-1)
})

// 用户信息弹窗
const showUserProfile = ref(false)

function onCompanyChange() {
  showNewCompany.value = quickForm.value.company === 'new'
  if (showNewCompany.value) {
    quickForm.value.newCompany = ''
  }
}

function handleUserCmd(cmd: string) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        await authStore.logout()
        router.push('/login')
      })
      .catch(() => {})
  } else if (cmd === 'profile') {
    showUserProfile.value = true
  } else if (cmd === 'settings') {
    router.push('/settings')
  }
}

// 提交快速记账
async function submitQuickAccount() {
  if (!quickForm.value.amount || Number(quickForm.value.amount) <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }
  const company = quickForm.value.company === 'new' ? quickForm.value.newCompany : quickForm.value.company
  if (!company) {
    ElMessage.warning('请选择或输入公司/客户名称')
    return
  }
  submitting.value = true
  try {
    await financeApi.createQuickRecord({
      type: 'income',
      amount: Number(quickForm.value.amount),
      category: quickForm.value.category,
      relatedName: company,
      paymentMethod: quickForm.value.paymentMethod,
      remark: quickForm.value.remark
    })
    ElMessage.success('记账成功')
    financeStore.triggerRefresh()
    quickForm.value = { amount: '', company: '', newCompany: '', category: '图文类', paymentMethod: 'cash', remark: '' }
    showNewCompany.value = false
    showQuickAccount.value = false
  } catch (e: any) {
    ElMessage.error(e.message || '记账失败')
  } finally {
    submitting.value = false
  }
}

// 加载客户列表
async function loadCompanyList() {
  try {
    const r = await customerApi.getList({ current: 1, size: 200 })
    companyList.value = r.data?.records || []
  } catch {
    companyList.value = []
  }
}

// 弹窗打开后自动聚焦金额输入框 + 刷新客户列表
watch(showQuickAccount, (val) => {
  if (val) {
    loadCompanyList()
    nextTick(() => quickAmountRef.value?.focus())
  }
})

// 监听全局快速记账事件（Dashboard 等页面触发）
function onShowQuickAccount() {
  showQuickAccount.value = true
}
onMounted(() => {
  window.addEventListener('show-quick-account', onShowQuickAccount)
  loadCompanyList()
})
onUnmounted(() => {
  window.removeEventListener('show-quick-account', onShowQuickAccount)
})
</script>

<style scoped lang="scss">
.app-header {
  position: fixed; top: 0; left: 0; right: 0; z-index: 1000;
  height: 60px;
  background: $bg-white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  border-bottom: 1px solid #f0f0f0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px;
}
.header-left { display: flex; align-items: center; gap: 10px; }
.logo {
  display: flex; align-items: center; gap: 10px;
  font-size: 17px; font-weight: 700; color: $primary;
  letter-spacing: 0.3px;
}
.logo-icon {
  width: 34px; height: 34px; border-radius: 9px;
  background: linear-gradient(135deg, $primary, #66b1ff);
  color: #fff; font-weight: bold; font-size: 15px;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 3px 8px rgba(64,158,255,0.35);
}
.header-right { display: flex; align-items: center; gap: 12px; }
.quick-btn {
  background: linear-gradient(135deg, $success, #85ce61);
  color: #fff; border: none; padding: 8px 18px;
  border-radius: 20px; cursor: pointer; font-size: 13px;
  font-weight: 500; transition: all 0.3s;
  display: flex; align-items: center; gap: 6px;
  box-shadow: 0 2px 8px rgba(103,194,58,0.3);
  &:hover {
    transform: scale(1.05);
    box-shadow: 0 5px 15px rgba(103, 194, 58, 0.45);
  }
}
.notif-wrapper { position: relative; }
.notif-btn {
  position: relative; width: 38px; height: 38px;
  border-radius: 50%; background: #f5f7fa;
  border: 1px solid #eee; cursor: pointer;
  font-size: 17px; display: flex;
  align-items: center; justify-content: center;
  transition: all 0.2s;
  &:hover { background: #ecf5ff; border-color: #409eff; }
}
.notif-badge {
  position: absolute; top: -3px; right: -3px;
  min-width: 18px; height: 18px; background: #f56c6c;
  color: #fff; border-radius: 9px; font-size: 10px;
  font-weight: 600; display: flex; align-items: center;
  justify-content: center; padding: 0 4px;
  border: 2px solid #fff;
  animation: badgePulse 2s infinite;
}
@keyframes badgePulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(245,108,108,0.4); }
  50% { box-shadow: 0 0 0 4px rgba(245,108,108,0); }
}
.user-info {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 12px; border-radius: 22px; cursor: pointer;
  border: 1px solid transparent; transition: all 0.2s;
  &:hover { background: $bg-base; border-color: #eee; }
}
.user-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: linear-gradient(135deg, $primary, #66b1ff);
  color: #fff; font-size: 14px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 6px rgba(64,158,255,0.3);
}
.user-meta { display: flex; flex-direction: column; }
.user-name { font-size: 13px; color: $text-primary; }
.user-role { font-size: 11px; color: $text-secondary; }
</style>
