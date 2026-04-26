<template>
  <div class="layout">
    <!-- 顶部Header -->
    <AppHeader @toggle-sidebar="sidebarCollapsed = !sidebarCollapsed" />

    <div class="layout-body">
      <!-- 侧边栏 -->
      <AppSidebar :collapsed="sidebarCollapsed" />

      <!-- 主内容区 -->
      <main class="main-content" :class="{ collapsed: sidebarCollapsed }">
        <router-view v-slot="{ Component, route }">
          <transition name="fade" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'

const sidebarCollapsed = ref(false)
</script>

<style scoped lang="scss">
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.layout-body {
  display: flex;
  flex: 1;
  margin-top: 60px;
}
.main-content {
  flex: 1;
  margin-left: 220px;
  min-height: calc(100vh - 60px);
  background: $bg-base;
  transition: margin-left 0.3s ease;
  &.collapsed { margin-left: 64px; }
}
</style>
