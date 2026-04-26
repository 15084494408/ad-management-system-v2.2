<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">⚙️ 通知设置</span>
    </div>

    <div class="card">
      <div class="card-header-row"><span class="card-title">🔔 通知方式</span></div>
      <el-form label-width="160px" style="padding: 20px;">
        <el-form-item label="站内消息">
          <el-switch v-model="settings.inApp" />
        </el-form-item>
        <el-form-item label="邮件通知">
          <el-switch v-model="settings.email" />
          <el-input v-if="settings.email" v-model="settings.emailAddress" placeholder="请输入邮箱地址" style="width:250px;margin-left:15px;" />
        </el-form-item>
        <el-form-item label="微信通知">
          <el-switch v-model="settings.wechat" />
          <span v-if="settings.wechat" style="margin-left:15px;color:#909399;font-size:12px;">已绑定微信: {{ settings.wechatName || '未绑定' }}</span>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="card-header-row"><span class="card-title">📋 通知规则</span></div>
      <el-form label-width="200px" style="padding: 20px;">
        <el-divider content-position="left">订单通知</el-divider>
        <el-form-item label="新订单提醒">
          <el-switch v-model="settings.newOrder" />
        </el-form-item>
        <el-form-item label="订单状态变更">
          <el-switch v-model="settings.statusChange" />
        </el-form-item>
        <el-form-item label="订单交付提醒">
          <el-switch v-model="settings.deliveryReminder" />
          <span style="margin-left:10px;color:#909399;font-size:12px;">提前</span>
          <el-input-number v-model="settings.remindDays" :min="1" :max="7" size="small" style="width:80px;margin:0 5px;" />
          <span style="color:#909399;font-size:12px;">天提醒</span>
        </el-form-item>
        <el-divider content-position="left">财务通知</el-divider>
        <el-form-item label="收款提醒">
          <el-switch v-model="settings.receiveNotify" />
        </el-form-item>
        <el-form-item label="逾期提醒">
          <el-switch v-model="settings.overdueNotify" />
        </el-form-item>
        <el-divider content-position="left">物料通知</el-divider>
        <el-form-item label="库存预警">
          <el-switch v-model="settings.stockWarning" />
        </el-form-item>
        <el-divider content-position="left">系统通知</el-divider>
        <el-form-item label="系统公告">
          <el-switch v-model="settings.systemAnnounce" />
        </el-form-item>
        <el-form-item label="系统维护">
          <el-switch v-model="settings.maintenanceNotify" />
        </el-form-item>
      </el-form>
    </div>

    <div style="padding: 20px 0;">
      <el-button type="primary" :loading="saving" @click="saveSettings">保存设置</el-button>
      <el-button @click="resetSettings">恢复默认</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const saving = ref(false)
const settings = reactive({
  inApp: true, email: false, emailAddress: '', wechat: false, wechatName: '',
  newOrder: true, statusChange: true, deliveryReminder: true, remindDays: 1,
  receiveNotify: true, overdueNotify: true, stockWarning: true,
  systemAnnounce: true, maintenanceNotify: true,
})

async function loadSettings() {
  try {
    const res = await request.get('/notice/setting')
    Object.assign(settings, res.data || {})
  } catch {}
}

async function saveSettings() {
  saving.value = true
  try { await request.put('/notice/setting', settings); ElMessage.success('保存成功') } catch { ElMessage.error('保存失败') }
  saving.value = false
}

function resetSettings() {
  Object.assign(settings, {
    inApp: true, email: false, emailAddress: '', wechat: false, wechatName: '',
    newOrder: true, statusChange: true, deliveryReminder: true, remindDays: 1,
    receiveNotify: true, overdueNotify: true, stockWarning: true,
    systemAnnounce: true, maintenanceNotify: true,
  })
  ElMessage.info('已恢复默认设置')
}

onMounted(loadSettings)
</script>
