import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// App para mecánicos y operadores de traslado. Usa el mismo backend que la app de oficina.
export default defineConfig({
  plugins: [react()],
  server: {
    // 5174 para no utilizar el de la app de oficina (5173)
    port: 5174,
    // Permite abrirla desde el celular en la misma red: http://IP-DE-LA-PC:5174
    host: true,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})