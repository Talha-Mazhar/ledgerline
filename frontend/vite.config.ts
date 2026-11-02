import react from '@vitejs/plugin-react';
import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vitest/config';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    rolldownOptions: {
      output: {
        // Libraries in their own long-cached files: an app release doesn't make browsers download
        // React or MUI again.
        codeSplitting: {
          groups: [
            {
              name: 'react',
              test: /node_modules[\\/](react|react-dom|react-router|scheduler)[\\/]/,
            },
            { name: 'mui', test: /node_modules[\\/](@mui|@emotion)[\\/]/ },
            {
              name: 'data',
              test: /node_modules[\\/](@tanstack|zod|react-hook-form|@hookform)[\\/]/,
            },
          ],
        },
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: true,
    passWithNoTests: true,
    coverage: {
      provider: 'v8',
      include: ['src/**'],
      exclude: ['src/shared/api/generated/**'],
    },
  },
});
