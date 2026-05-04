<template>
  <div style="min-height:calc(100vh - 60px);display:flex;align-items:center;justify-content:center;flex-direction:column;gap:16px;">
    <div style="font-size:80px;">🔍</div>
    <div style="font-size:24px;font-weight:700;color:#303133;">404 - 页面不存在</div>
    <div style="color:#909399;">你访问的页面已被外星人带走了...</div>
    <el-button type="primary" @click="router.push('/')">回到首页</el-button>
  </div>
</template>
<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>
