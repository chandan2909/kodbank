import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const apiProxy = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
}

export default defineConfig({
  plugins: [react()],
  preview: {
    port: 4173,
    proxy: apiProxy,
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: apiProxy,
  },
})
