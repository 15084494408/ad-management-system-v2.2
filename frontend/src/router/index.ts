import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '工作台', icon: '📊' },
      },
      // ── 待办工作台（放在工作台下） ──
      {
        path: 'todo',
        name: 'TodoWorkbench',
        component: () => import('@/views/todo/TodoWorkbenchView.vue'),
        meta: { title: '待办工作台', icon: '📋' },
      },
      // ── 订单管理 ──
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/orders/OrderListView.vue'),
        meta: { title: '订单列表', icon: '📋' },
      },
      {
        path: 'orders/create',
        name: 'OrderCreate',
        component: () => import('@/views/orders/OrderCreateView.vue'),
        meta: { title: '创建订单' },
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/orders/OrderDetailView.vue'),
        meta: { title: '订单详情' },
      },
      // ── 客户管理 ──
      {
        path: 'customers',
        name: 'Customers',
        component: () => import('@/views/customers/CustomerListView.vue'),
        meta: { title: '客户列表', icon: '👥' },
      },
      {
        path: 'customers/levels',
        name: 'CustomerLevels',
        component: () => import('@/views/customers/CustomerLevelView.vue'),
        meta: { title: '客户等级' },
      },
      {
        path: 'customer-bills',
        name: 'CustomerBills',
        component: () => import('@/views/customers/CustomerBillView.vue'),
        meta: { title: '客户账单', icon: '📋' },
      },
      {
        path: 'factory-bills',
        name: 'FactoryBills',
        component: () => import('@/views/factory/FactoryBillView.vue'),
        meta: { title: '工厂账单', icon: '🏭' },
      },
      // ── 会员管理 ──
      {
        path: 'members',
        name: 'Members',
        component: () => import('@/views/members/MemberListView.vue'),
        meta: { title: '会员列表', icon: '👑' },
      },
      {
        path: 'members/recharge',
        name: 'MemberRecharge',
        component: () => import('@/views/members/MemberRechargeView.vue'),
        meta: { title: '充值管理' },
      },
      {
        path: 'members/consume',
        name: 'MemberConsume',
        component: () => import('@/views/members/MemberConsumeView.vue'),
        meta: { title: '消费记录' },
      },
      {
        path: 'members/levels',
        name: 'MemberLevels',
        component: () => import('@/views/members/MemberLevelView.vue'),
        meta: { title: '会员等级' },
      },
      // ── 财务管理 ──
      {
        path: 'finance',
        name: 'Finance',
        component: () => import('@/views/finance/FinanceView.vue'),
        meta: { title: '财务概览', icon: '💰' },
      },
      {
        path: 'finance/quick-record',
        name: 'QuickRecord',
        component: () => import('@/views/finance/QuickRecordView.vue'),
        meta: { title: '快速记账' },
      },
      {
        path: 'finance/receive',
        name: 'FinanceReceive',
        component: () => import('@/views/finance/FinanceReceiveView.vue'),
        meta: { title: '收款管理' },
      },
      {
        path: 'finance/arap',
        name: 'FinanceArap',
        component: () => import('@/views/finance/FinanceArapView.vue'),
        meta: { title: '应收应付' },
      },
      {
        path: 'finance/invoice',
        name: 'FinanceInvoice',
        component: () => import('@/views/finance/FinanceInvoiceView.vue'),
        meta: { title: '发票管理' },
      },
      {
        path: 'finance/quote',
        name: 'FinanceQuote',
        component: () => import('@/views/finance/FinanceQuoteView.vue'),
        meta: { title: '报价管理' },
      },
      {
        path: 'finance/quote/create',
        name: 'QuoteCreate',
        component: () => import('@/views/finance/QuoteCreateView.vue'),
        meta: { title: '新建报价' },
      },
      {
        path: 'finance/print-quote',
        name: 'PrintQuote',
        component: () => import('@/views/finance/PrintQuoteView.vue'),
        meta: { title: '印刷报价计算器' },
      },
      {
        path: 'finance/report',
        name: 'FinanceReport',
        component: () => import('@/views/finance/FinanceReportView.vue'),
        meta: { title: '财务报表' },
      },
      {
        path: 'finance/flow',
        name: 'FinanceFlow',
        component: () => import('@/views/finance/FlowRecordView.vue'),
        meta: { title: '财务流水' },
      },
      {
        path: 'finance/designer-commission',
        name: 'FinanceDesignerCommission',
        component: () => import('@/views/finance/DesignerCommissionView.vue'),
        meta: { title: '设计师提成' },
      },
      {
        path: 'finance/salary',
        name: 'FinanceSalary',
        component: () => import('@/views/finance/SalaryView.vue'),
        meta: { title: '工资管理' },
      },

      // ── 系统管理 ──
      {
        path: 'system/users',
        name: 'SystemUsers',
        component: () => import('@/views/system/SystemUserView.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'system/roles',
        name: 'SystemRoles',
        component: () => import('@/views/system/SystemRoleView.vue'),
        meta: { title: '角色权限' },
      },
      {
        path: 'system/logs',
        name: 'SystemLogs',
        component: () => import('@/views/system/SystemLogView.vue'),
        meta: { title: '操作日志' },
      },
      {
        path: 'system/dict',
        name: 'SystemDict',
        component: () => import('@/views/system/SystemDictView.vue'),
        meta: { title: '数据字典' },
      },
      {
        path: 'system/config',
        name: 'SystemConfig',
        component: () => import('@/views/system/SystemConfigView.vue'),
        meta: { title: '系统配置' },
      },
      {
        path: 'system/backup',
        name: 'DataBackup',
        component: () => import('@/views/system/DataBackupView.vue'),
        meta: { title: '数据备份' },
      },
      {
        path: 'system/buttons',
        name: 'ButtonManage',
        component: () => import('@/views/system/ButtonManageView.vue'),
        meta: { title: '按钮管理' },
      },
      {
        path: 'system/companies',
        name: 'CompanyManage',
        component: () => import('@/views/system/SystemCompanyView.vue'),
        meta: { title: '公司管理' },
      },
      {
        path: 'finance/commission-config',
        name: 'CommissionConfig',
        component: () => import('@/views/system/DesignerCommissionView.vue'),
        meta: { title: '提成配置', icon: '👔' },
      },
      // ── 物料管理 ──
      {
        path: 'materials',
        name: 'Materials',
        component: () => import('@/views/material/MaterialStockView.vue'),
        meta: { title: '物料库存', icon: '📦' },
      },
      {
        path: 'materials/types',
        name: 'MaterialTypes',
        component: () => import('@/views/material/MaterialTypeView.vue'),
        meta: { title: '物料分类' },
      },
      {
        path: 'materials/purchase',
        name: 'MaterialPurchase',
        component: () => import('@/views/material/MaterialPurchaseView.vue'),
        meta: { title: '采购管理' },
      },
      {
        path: 'materials/warning',
        name: 'MaterialWarning',
        component: () => import('@/views/material/MaterialWarningView.vue'),
        meta: { title: '库存预警' },
      },
      {
        path: 'materials/overview',
        name: 'MaterialOverview',
        component: () => import('@/views/material/MaterialOverviewView.vue'),
        meta: { title: '物料总览' },
      },
      // ── 设计文件 ──
      {
        path: 'design/files',
        name: 'DesignFiles',
        component: () => import('@/views/design/DesignFileView.vue'),
        meta: { title: '设计文件', icon: '🎨' },
      },
      {
        path: 'design/upload',
        name: 'DesignUpload',
        component: () => import('@/views/design/DesignUploadView.vue'),
        meta: { title: '设计上传' },
      },
      {
        path: 'design/versions',
        name: 'DesignVersions',
        component: () => import('@/views/design/DesignVersionView.vue'),
        meta: { title: '版本控制' },
      },
      // ── 设计广场 ──
      {
        path: 'square',
        name: 'Square',
        component: () => import('@/views/square/SquareView.vue'),
        meta: { title: '需求广场', icon: '🏪' },
      },
      {
        path: 'square/publish',
        name: 'SquarePublish',
        component: () => import('@/views/square/SquarePublishView.vue'),
        meta: { title: '需求发布' },
      },
      {
        path: 'square/application',
        name: 'SquareApplication',
        component: () => import('@/views/square/SquareApplicationView.vue'),
        meta: { title: '需求申请' },
      },
      {
        path: 'square/income',
        name: 'SquareIncome',
        component: () => import('@/views/square/SquareIncomeView.vue'),
        meta: { title: '收益统计' },
      },
      // ── 统计分析 ──
      {
        path: 'statistics/order',
        name: 'StatOrder',
        component: () => import('@/views/statistic/StatOrderView.vue'),
        meta: { title: '订单统计', icon: '📈' },
      },
      {
        path: 'statistics/revenue',
        name: 'StatRevenue',
        component: () => import('@/views/statistic/StatRevenueView.vue'),
        meta: { title: '营收统计' },
      },
      {
        path: 'statistics/customer',
        name: 'StatCustomer',
        component: () => import('@/views/statistic/StatCustomerView.vue'),
        meta: { title: '客户统计' },
      },
      {
        path: 'statistics/material',
        name: 'StatMaterial',
        component: () => import('@/views/statistic/StatMaterialView.vue'),
        meta: { title: '物料统计' },
      },
      {
        path: 'statistics/flow',
        name: 'StatFlow',
        component: () => import('@/views/statistic/StatFlowView.vue'),
        meta: { title: '流水统计' },
      },
      {
        path: 'orders/statistics',
        name: 'OrderStatistics',
        component: () => import('@/views/orders/OrderStatisticsView.vue'),
        meta: { title: '订单报表' },
      },
      // ── 消息通知 ──
      {
        path: 'notices',
        name: 'Notices',
        component: () => import('@/views/notice/NoticeView.vue'),
        meta: { title: '消息通知', icon: '🔔' },
      },
      {
        path: 'notices/system',
        name: 'NotifySystem',
        component: () => import('@/views/notice/NotifySystemView.vue'),
        meta: { title: '系统通知' },
      },
      {
        path: 'notices/order',
        name: 'NotifyOrder',
        component: () => import('@/views/notice/NotifyOrderView.vue'),
        meta: { title: '订单通知' },
      },
      {
        path: 'notices/settings',
        name: 'NotifySetting',
        component: () => import('@/views/notice/NotifySettingView.vue'),
        meta: { title: '通知设置' },
      },
      // ── 系统设置 ──
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/SettingsView.vue'),
        meta: { title: '系统设置', icon: '⚙️' },
      },
    ],
  },
  // 404
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/error/NotFoundView.vue') },
]

const router = createRouter({
  history: createWebHistory('/'),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

// ★ 修复 P0-4: 用户信息缓存策略，避免每次路由切换都请求后端
let userInfoLastFetch = 0
const USER_INFO_CACHE_DURATION = 5 * 60 * 1000 // 5 分钟缓存
let userInfoFetchPromise: Promise<any> | null = null

// 全局路由守卫
router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  document.title = `${to.meta.title || '管理系统'} - 企业广告管理`
  const authStore = useAuthStore()

  // 登录页：有 token 就验证，无效则清掉留在登录页
  if (to.path === '/login') {
    if (authStore.token) {
      try {
        await authStore.fetchUserInfo()
        next('/')
        return
      } catch {
        authStore.clearToken()
      }
    }
    next()
    return
  }

  // 不需要认证的页面直接放行
  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  // 没有 token → 跳登录
  if (!authStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // ★ 修复 P0-4: 缓存 + 过期刷新策略
  // 有 userInfo 缓存且在有效期内 → 直接放行（不请求后端）
  const now = Date.now()
  if (authStore.userInfo && (now - userInfoLastFetch < USER_INFO_CACHE_DURATION)) {
    next()
    return
  }

  // 缓存过期或首次加载 → 请求后端验证 token 有效性
  // 使用 Promise 去重，防止并发路由切换导致多次请求
  try {
    if (!userInfoFetchPromise) {
      userInfoFetchPromise = authStore.fetchUserInfo().finally(() => {
        userInfoFetchPromise = null
      })
    }
    await userInfoFetchPromise
    userInfoLastFetch = Date.now()
    next()
  } catch {
    // 401/403 等认证失败 → 清除 token 并跳转登录
    authStore.clearToken()
    userInfoLastFetch = 0
    next({ path: '/login', query: { redirect: to.fullPath } })
  }
})

router.afterEach(() => NProgress.done())

export default router
