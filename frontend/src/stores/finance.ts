import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useFinanceStore = defineStore('finance', () => {
  const lastFinanceUpdate = ref(Date.now())

  function triggerRefresh() {
    lastFinanceUpdate.value = Date.now()
  }

  return { lastFinanceUpdate, triggerRefresh }
})
