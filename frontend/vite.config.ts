import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Configura proxy para redirigir las solicitudes a la API del backend
      '/api': 'http://localhost:8080',
    },
  },
})
