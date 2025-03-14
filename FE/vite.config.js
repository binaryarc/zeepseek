import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig(({ mode }) => ({
  id: "/",
  plugins: [
    react(),
    VitePWA({
      registerType: "autoUpdate",
      devOptions: {
        enabled: mode !== "development", // 개발 환경에서는 PWA 비활성화, 배포 시 활성화
        debug: false, 
      },
      workbox: {
        clientsClaim: mode !== "development", // 개발 환경에서는 서비스 워커가 자동 적용되지 않도록 설정
        skipWaiting: mode !== "development", // 개발 환경에서는 캐싱 없이 최신 코드 사용
        disableDevLogs: true, // Workbox의 개발 로그 최소화
      },
      manifest: {
        name: "My PWA App",
        short_name: "PWA App",
        start_url: "/",
        display: "standalone",
        background_color: "#ffffff",
        theme_color: "#000000",
        icons: [
          {
            src: "/192image.png",
            sizes: "192x192",
            type: "image/png",
          },
          {
            src: "/512image.png",
            sizes: "512x512",
            type: "image/png",
          },
        ],
      },
    }),
  ],
  server: {
    host: "0.0.0.0",
  },
}));
