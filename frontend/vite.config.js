import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // During `npm run dev` the frontend runs on :5173 and the backend on :8080.
  // Because axios now uses a relative baseURL ("/"), API calls go to :5173 —
  // this proxy forwards anything starting with /api to the backend on :8080.
  // In production (bundled JAR) there is no dev server, so this is dev-only.
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
