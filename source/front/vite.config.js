import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      '/mip': { target: 'http://localhost:8091', changeOrigin: true }
    }
  }
})
