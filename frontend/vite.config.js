import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // During `npm run dev` the frontend runs on :5173 and the backend on :8080.
  // axios uses a relative baseURL ("/"), so API calls hit :5173 and this proxy
  // forwards /api/* to the backend. Not used in the bundled JAR.
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
