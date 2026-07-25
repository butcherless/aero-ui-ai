import { defineConfig } from "vite";

export default defineConfig({
  publicDir: "public",
  server: {
    port: 5173,
    proxy: {
      // The backend has no CORS support, so route API calls through this
      // same-origin dev-server proxy instead of fetching localhost:8080 directly.
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  }
});
