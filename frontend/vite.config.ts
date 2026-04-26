import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 自动按需引入
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/variables.scss" as *;`,
      },
    },
  },
  server: {
    port: 5173,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // ★ 修复：后端有 context-path: /api，所以不需要去掉前缀
        // rewrite: (path) => path.replace(/^\/api/, ''),  // 注释掉rewrite
      },
      '/uploads': {
        target: 'http://localhost:8080',  // ★ 修复：也指向后端 8080
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 2000,
    rollupOptions: {
      output: {
        // 分包策略：核心库单独打包
        manualChunks(id) {
          if (id.includes('element-plus')) return 'element-plus'
          if (id.includes('vue') || id.includes('pinia')) return 'vue-vendor'
          if (id.includes('axios') || id.includes('@vueuse') || id.includes('nprogress')) return 'utils'
        },
      },
    },
  },
})
