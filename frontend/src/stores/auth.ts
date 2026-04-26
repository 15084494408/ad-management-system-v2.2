import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/modules/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<any>(null)
  const permissions = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed(() => userInfo.value?.roles?.[0])
  const isSuperAdmin = computed(() => (userInfo.value?.roles || []).includes('SUPER_ADMIN'))

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('token', t)
  }

  function clearToken() {
    token.value = ''
    localStorage.removeItem('token')
    userInfo.value = null
    permissions.value = []
  }

  function hasPermission(code: string): boolean {
    if (isSuperAdmin.value) return true
    return permissions.value.includes(code)
  }

  function hasAnyPermission(codes: string[]): boolean {
    if (isSuperAdmin.value) return true
    return codes.some(c => permissions.value.includes(c))
  }

  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    setToken(res.data.token)
    userInfo.value = res.data.userInfo
    permissions.value = res.data.userInfo?.permissions || []
    return res
  }

  async function logout() {
    try { await authApi.logout() } catch {}
    clearToken()
  }

  async function fetchUserInfo() {
    const res = await authApi.getUserInfo()
    userInfo.value = res.data
    permissions.value = res.data?.permissions || []
  }

  return {
    token, userInfo, permissions,
    isLoggedIn, userRole, isSuperAdmin,
    login, logout, fetchUserInfo,
    hasPermission, hasAnyPermission,
    setToken, clearToken,
  }
}, {
  persist: { paths: ['token'] },
})
