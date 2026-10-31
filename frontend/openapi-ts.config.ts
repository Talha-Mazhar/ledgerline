import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: '../api/openapi.yaml',
  output: 'src/shared/api/generated',
  plugins: [
    '@hey-api/client-fetch',
    '@hey-api/typescript',
    // validator: every response is checked against the contract's zod schemas at runtime, so a
    // backend that drifts from openapi.yaml fails loudly instead of rendering wrong numbers.
    { name: '@hey-api/sdk', validator: true },
    'zod',
    '@tanstack/react-query',
  ],
});
