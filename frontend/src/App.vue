<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

// 应用启动时：如果有 token，立即验证有效性（后端重启后 token 会失效）
onMounted(async () => {
  if (authStore.token) {
    try {
      await authStore.fetchUserInfo()
    } catch {
      // token 无效 → 清除并跳转登录（防止用户看到空白/无权限页面）
      authStore.clearToken()
      router.replace('/login')
    }
  }
})
</script>
