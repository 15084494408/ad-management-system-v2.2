import { defineStore } from 'pinia'
import { ref } from 'vue'
import { orderApi } from '@/api/modules/order'

export const useOrderStore = defineStore('order', () => {
  const list = ref<any[]>([])
  const total = ref(0)
  const detail = ref<any>(null)
  const loading = ref(false)

  async function fetchList(params: Record<string, any> = {}) {
    loading.value = true
    try {
      const res = await orderApi.getList(params)
      const data = res.data
      list.value = data?.records || data?.list || []
      total.value = data?.total || 0
    } finally {
      loading.value = false
    }
  }

  async function fetchDetail(id: number) {
    loading.value = true
    try {
      const res = await orderApi.getDetail(id)
      detail.value = res.data || null
    } finally {
      loading.value = false
    }
  }

  return { list, total, detail, loading, fetchList, fetchDetail }
})
