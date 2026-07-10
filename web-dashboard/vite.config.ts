import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// Proxy em dev: single origin no browser, zero CORS nos servicos Java.
// Em demo/producao o nginx do docker-compose faz o mesmo mapeamento.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api/inscricoes": {
        target: "http://localhost:8081",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/inscricoes/, ""),
      },
      "/api/partidas": {
        target: "http://localhost:8082",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/partidas/, ""),
      },
      "/api/ranking": {
        target: "http://localhost:8083",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/ranking/, ""),
      },
    },
  },
});
