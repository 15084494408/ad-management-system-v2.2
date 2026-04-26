<template>
  <div class="login-wrap">
    <div class="login-left">
      <div class="brand-logo">广</div>
      <h1 class="brand-name">企业广告管理系统</h1>
      <p class="brand-desc">专业的广告公司业务管理平台<br />订单·客户·财务·会员 一体化管理</p>
      <div class="feature-list">
        <div class="f-item">📋 订单全流程跟踪</div>
        <div class="f-item">👥 客户关系管理</div>
        <div class="f-item">💰 财务收支管控</div>
        <div class="f-item">👑 会员积分体系</div>
      </div>
    </div>
    <div class="login-right">
      <div class="login-box">
        <div class="login-title">欢迎登录</div>
        <div class="login-sub">Enterprise Ad Management System</div>
        <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large"
              prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
          </el-form-item>
          <el-button type="primary" size="large" style="width:100%;" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>
        <div class="login-tip">演示账号：admin / 123456</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: 'admin', password: '123456' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect as string || '/'
    router.push(redirect)
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-wrap { min-height: 100vh; display: flex; }
.login-left {
  flex: 1; background: linear-gradient(135deg, #304156, #409eff);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 60px 40px; color: #fff;
}
.brand-logo {
  width: 80px; height: 80px; border-radius: 20px;
  background: rgba(255,255,255,0.15); font-size: 36px; font-weight: 900;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 20px; box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.brand-name { font-size: 28px; font-weight: 700; margin-bottom: 12px; }
.brand-desc { font-size: 14px; opacity: 0.8; line-height: 2; text-align: center; margin-bottom: 40px; }
.feature-list { display: flex; flex-direction: column; gap: 12px; }
.f-item { font-size: 14px; opacity: 0.85; background: rgba(255,255,255,0.1); padding: 8px 20px; border-radius: 20px; }
.login-right { width: 440px; display: flex; align-items: center; justify-content: center; background: $bg-base; }
.login-box { width: 360px; }
.login-title { font-size: 26px; font-weight: 700; color: $text-primary; margin-bottom: 6px; }
.login-sub { font-size: 13px; color: $text-secondary; margin-bottom: 32px; }
.login-tip { text-align: center; font-size: 12px; color: $text-secondary; margin-top: 16px; }
</style>
