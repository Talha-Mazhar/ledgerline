import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: '../api/openapi.yaml',
  output: 'src/shared/api/generated',
  plugins: [
    '@hey-api/client-fetch',
    '@hey-api/typescript',
    '@hey-api/sdk',
    'zod',
    '@tanstack/react-query',
  ],
});
