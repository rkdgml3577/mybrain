import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// /api 요청은 개발 시 API Gateway(8080)로 프록시한다.
// Gateway(profile=infra)를 띄우지 않았다면 아래 target 을 개별 서비스
// 포트(예: http://localhost:8082)로 바꿔 직접 호출할 수 있다.
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') }
  },
  server: {
    host: 'localhost',
    port: 3000,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
