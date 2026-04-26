<template>
  <div class="page-container" style="text-align:center;padding:60px 20px;">
    <p style="color:#909399;">正在跳转至订单列表...</p>
  </div>
</template>
<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()
onMounted(() => {
  router.replace('/orders')
})
</script>
