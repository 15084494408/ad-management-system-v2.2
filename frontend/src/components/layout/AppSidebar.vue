<template>
  <aside class="sidebar" :class="{ collapsed }">
    <nav class="menu">
      <div v-for="group in filteredMenuGroups" :key="group.title" class="menu-group">
        <div class="menu-group-title" v-if="!collapsed && group.title">{{ group.title }}</div>
        <template v-for="item in group.items" :key="item.path">
          <!-- 有子菜单 -->
          <div v-if="item.children && item.children.length" class="menu-item" :class="{ open: openMenus.includes(item.name) }">
            <div class="menu-link" @click="toggleMenu(item.name)">
              <span class="menu-icon">{{ item.icon }}</span>
              <span class="menu-text" v-if="!collapsed">{{ item.label }}</span>
              <el-icon v-if="!collapsed" class="menu-arrow"><ArrowRight /></el-icon>
            </div>
            <div class="submenu" v-if="!collapsed">
              <router-link
                v-for="sub in item.children" :key="sub.path"
                :to="sub.path"
                class="submenu-item"
                :class="{ active: currentPath === sub.path }"
              >
                {{ sub.label }}
                <span v-if="sub.badge" class="v2-badge">{{ sub.badge }}</span>
              </router-link>
            </div>
          </div>
          <!-- 无子菜单 -->
          <router-link v-else :to="item.path" class="menu-link solo" :class="{ active: currentPath.startsWith(item.path) }">
            <el-tooltip :content="item.label" placement="right" :disabled="!collapsed">
              <span class="menu-icon">{{ item.icon }}</span>
            </el-tooltip>
            <span class="menu-text" v-if="!collapsed">{{ item.label }}</span>
          </router-link>
        </template>
      </div>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

defineProps<{ collapsed?: boolean }>()

const route = useRoute()
const authStore = useAuthStore()
const currentPath = computed(() => route.path)
const openMenus = ref<string[]>([])

function toggleMenu(name: string) {
  const idx = openMenus.value.indexOf(name)
  if (idx >= 0) openMenus.value.splice(idx, 1)
  else openMenus.value.push(name)
}

// 权限码 → 菜单映射
const menuGroups = [
  {
    title: '',  // 空标题，不显示分组
    items: [
      // 1. 工作台
      { name: 'dashboard', path: '/dashboard', icon: '📊', label: '工作台', perm: 'dashboard:view' },
      // 2. 待办工作台
      { path: '/todo', label: '📋 待办工作台', perm: 'order:list', badge: 'NEW' },
      // 3. 订单管理
      {
        name: 'orders', icon: '📋', label: '订单管理', perm: 'order:list',
        children: [
          { path: '/orders', label: '订单列表', perm: 'order:list' },
          { path: '/orders/create', label: '创建订单', perm: 'order:create' },
          { path: '/orders/statistics', label: '订单统计', perm: 'order:list' },
          { path: '/finance/quote', label: '订单报价', perm: 'finance:view' },
        ],
      },
      // 4. 客户管理
      {
        name: 'customers', icon: '👥', label: '客户管理', perm: 'customer:list',
        children: [
          { path: '/customers', label: '客户列表', perm: 'customer:list' },
          { path: '/customers/levels', label: '客户等级', perm: 'customer:list' },
          { path: '/customer-bills', label: '客户账单', perm: 'factory:list' },
          { path: '/factory-bills', label: '工厂账单', perm: 'factory:list', badge: 'V2.1' },
        ],
      },
      // 5. 会员管理
      {
        name: 'members', icon: '👑', label: '会员管理', perm: 'member:list',
        children: [
          { path: '/members', label: '会员列表', perm: 'member:list' },
          { path: '/members/recharge', label: '充值管理', perm: 'member:recharge' },
          { path: '/members/consume', label: '消费记录', perm: 'member:list' },
          { path: '/members/levels', label: '会员等级', perm: 'member:list' },
        ],
      },
      // 6. 财务结算
      {
        name: 'finance', icon: '💰', label: '财务结算', perm: 'finance:view',
        children: [
          { path: '/finance', label: '📊 财务概览', perm: 'finance:view' },
          { path: '/finance/flow', label: '💵 财务流水', perm: 'finance:view', badge: '统一' },
          { path: '/finance/arap', label: '📋 应收应付', perm: 'finance:view' },
          { path: '/finance/invoice', label: '🧾 发票管理', perm: 'finance:view' },
          { path: '/finance/report', label: '📈 财务报表', perm: 'finance:view' },
          { path: '/finance/designer-commission', label: '👔 提成管理', perm: 'finance:view' },
        ],
      },
      // 7. 统计分析
      {
        name: 'statistics', icon: '📈', label: '统计分析', perm: 'statistics:view',
        children: [
          { path: '/statistics/order', label: '订单统计', perm: 'statistics:view' },
          { path: '/statistics/revenue', label: '营收统计', perm: 'statistics:view' },
          { path: '/statistics/customer', label: '客户统计', perm: 'statistics:view' },
          { path: '/statistics/material', label: '物料统计', perm: 'statistics:view' },
          { path: '/statistics/flow', label: '流水统计', perm: 'statistics:view', badge: 'V2.2' },
        ],
      },
      // 8. 设计文件
      {
        name: 'design', icon: '🎨', label: '设计文件', perm: 'design:file',
        children: [
          { path: '/design/files', label: '文件管理', perm: 'design:file' },
          { path: '/design/upload', label: '文件上传', perm: 'design:file' },
          { path: '/design/versions', label: '版本管理', perm: 'design:file' },
        ],
      },
      // 9. 物料管理
      {
        name: 'material', icon: '📦', label: '物料管理', perm: 'material:view',
        children: [
          { path: '/materials', label: '物料库存', perm: 'material:view' },
          { path: '/materials/types', label: '物料分类', perm: 'material:view' },
          { path: '/materials/purchase', label: '采购管理', perm: 'material:view' },
          { path: '/materials/warning', label: '库存预警', perm: 'material:view' },
          { path: '/materials/overview', label: '📦 物料总览', perm: 'material:view' },
        ],
      },
      // 10. 设计广场
      {
        name: 'square', icon: '🏪', label: '设计广场', perm: 'square:manage',
        children: [
          { path: '/square', label: '需求广场', perm: 'square:manage' },
          { path: '/square/publish', label: '发布需求', perm: 'square:manage' },
          { path: '/square/application', label: '我的接单', perm: 'square:manage' },
          { path: '/square/income', label: '收入管理', perm: 'square:manage' },
        ],
      },
      // 11. 系统管理
      {
        name: 'system', icon: '⚙️', label: '系统管理', perm: 'system:user',
        children: [
          { path: '/system/users', label: '用户管理', perm: 'system:user' },
          { path: '/system/roles', label: '角色权限', perm: 'system:role' },
          { path: '/system/config', label: '系统配置', perm: 'system:user' },
          { path: '/system/logs', label: '操作日志', perm: 'system:log' },
          { path: '/system/dict', label: '数据字典', perm: 'system:dict' },
          { path: '/system/backup', label: '数据备份', perm: 'system:backup' },
          { path: '/system/buttons', label: '🎨 按钮管理', perm: 'system:user' },
          { path: '/system/companies', label: '🏢 公司管理', perm: 'system:config' },
        ],
      },
    ],
  },
]

// 根据权限过滤菜单
const filteredMenuGroups = computed(() => {
  return menuGroups.map(group => ({
    ...group,
    items: group.items.map(item => {
      if (!item.children) {
        // 无子菜单：检查权限
        return item.perm && !authStore.hasPermission(item.perm) ? null : item
      }
      // 有子菜单：过滤子项，保留有权限的
      const filteredChildren = item.children.filter(c =>
        !c.perm || authStore.hasPermission(c.perm)
      )
      return filteredChildren.length > 0 ? { ...item, children: filteredChildren } : null
    }).filter(Boolean) as any[],
  })).filter(group => group.items.length > 0)
})

// 根据当前路由自动展开对应的父菜单
function autoOpenCurrentMenu() {
  const path = route.path
  for (const group of filteredMenuGroups.value) {
    for (const item of group.items) {
      if (item.children) {
        const match = item.children.some(c => path.startsWith(c.path))
        if (match && !openMenus.value.includes(item.name)) {
          openMenus.value.push(item.name)
        } else if (!match) {
          const idx = openMenus.value.indexOf(item.name)
          if (idx >= 0) openMenus.value.splice(idx, 1)
        }
      }
    }
  }
}
watch(currentPath, autoOpenCurrentMenu, { immediate: true })
</script>

<style scoped lang="scss">
.sidebar {
  width: 220px; position: fixed; left: 0; top: 60px; bottom: 0;
  background: $sidebar-bg; overflow-y: auto; overflow-x: hidden;
  box-shadow: 2px 0 8px rgba(0,0,0,0.08);
  transition: width 0.3s ease; z-index: 999;
  &.collapsed { width: 64px; }
  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.2); border-radius: 3px; }
}
.menu { padding: 15px 0; }
.menu-group { padding: 15px 0; }
.menu-group-title {
  padding: 8px 20px; font-size: 11px;
  color: rgba(255,255,255,0.4); text-transform: uppercase; letter-spacing: 1px;
}
.menu-item { position: relative; }
.menu-link {
  display: flex; align-items: center; padding: 12px 20px;
  color: rgba(255,255,255,0.7); cursor: pointer; gap: 10px;
  transition: all 0.3s; text-decoration: none;
  &:hover { background: #263445; color: #fff; }
  &.active, &.router-link-active { 
    background: $primary; color: #fff; 
  }
  &.solo.router-link-active { 
    background: $primary; color: #fff; 
  }
}
.menu-item.active::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0;
  width: 4px; background: #fff;
}
.menu-icon { width: 20px; text-align: center; font-size: 16px; flex-shrink: 0; }
.menu-text { flex: 1; font-size: 14px; white-space: nowrap; }
.menu-arrow { font-size: 12px; transition: transform 0.3s; }
.menu-item.open > .menu-link .menu-arrow { transform: rotate(90deg); }
.submenu { display: none; background: rgba(0,0,0,0.1); }
.menu-item.open > .submenu { display: block; }
.submenu-item {
  display: block; padding: 10px 20px 10px 50px;
  color: rgba(255,255,255,0.6); cursor: pointer; font-size: 13px;
  transition: all 0.3s; text-decoration: none;
  display: flex; align-items: center; gap: 8px;
  &:hover { background: #263445; color: #fff; }
  &.active, &.router-link-active { 
    color: $primary; border-right: 3px solid $primary; 
  }
}
.v2-badge {
  background: $success; color: #fff; font-size: 9px;
  padding: 2px 5px; border-radius: 3px; font-weight: normal;
}
</style>
