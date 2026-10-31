import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: '../api/openapi.yaml',
  output: 'src/shared/api/generated',
  plugins: [
    '@hey-api/client-fetch',
    '@hey-api/typescript',
    // Every response is checked against the contract's zod schemas at runtime, so a backend that
    // drifts from openapi.yaml fails loudly instead of rendering wrong numbers. Requests aren't:
    // the generated request check can't read headers, and forms already validate what users type.
    { name: '@hey-api/sdk', validator: { request: false, response: true } },
    'zod',
    '@tanstack/react-query',
  ],
});
