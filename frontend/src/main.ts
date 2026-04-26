import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import 'element-plus/dist/index.css'
import '@/styles/global.scss'

import App from './App.vue'
import router from './router'

// NProgress 配置
NProgress.configure({ showSpinner: false })

const app = createApp(App)

// Pinia 持久化插件
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.mount('#app')
