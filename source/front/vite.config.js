import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  // 本地开发保持根路径 base='/'；仅门户部署构建时用 VITE_BASE=/mig/ 注入
  base: process.env.VITE_BASE || '/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(import.meta.dirname, 'src'),
    },
  },
  server: {
    port: 5174,
    proxy: {
      // 本地默认接口前缀 /mig；门户部署时前端走 /mig-api，再改写到 context-path /mig
      '/mig': { target: 'http://localhost:8091', changeOrigin: true },
      '/mig-api': {
        target: 'http://localhost:8091',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/mig-api/, '/mig'),
      }
    }
  }
})
