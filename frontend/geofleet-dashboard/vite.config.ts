import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";
import { componentTagger } from "lovable-tagger";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  server: {
    host: "::",        // Listen on all interfaces (good for Docker)
    port: 8081,
    proxy: {
      // Proxy all /api requests to the backend
      "/api": {
        target: "http://backend:8080",  // ← Change to this (service name from docker-compose)
        changeOrigin: true,
        secure: false,
        // Optional: uncomment if you need to strip /api (but you DON'T in this case, as backend base-path is /api)
        // rewrite: (path) => path.replace(/^\/api/, ""),
      },
    },
  },
  plugins: [react(), mode === "development" && componentTagger()].filter(Boolean),
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
}));
