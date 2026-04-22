import { reactRouter } from "@react-router/dev/vite";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "vite";
import tsconfigPaths from "vite-tsconfig-paths";

export default defineConfig({
  plugins: [tailwindcss(), reactRouter(), tsconfigPaths()],
  server: {
    proxy: {
      "/api": {
        // In Docker Compose, the backend is reachable by service name.
        // Locally (no Docker), this falls back to your normal localhost setup.
        target: process.env.VITE_API_PROXY_TARGET ?? "http://localhost:8081",
        changeOrigin: true,
      },
    },
  },
});
