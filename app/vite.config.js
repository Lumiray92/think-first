import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 本地开发固定使用 /ThinkFirst/ 作为 base，
  // 因此访问地址是 http://localhost:5173/ThinkFirst/login
  base: '/ThinkFirst/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: true,
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_PROXY || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
