import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { client } from '@/shared/api/generated/client.gen';
import { server } from '@/test/msw/server';
import { API, resetWallets } from '@/test/msw/walletApi';

// Node's fetch needs absolute URLs; in the browser the relative '/api/v1' is enough.
client.setConfig({ baseUrl: API });

beforeAll(() => {
  server.listen({ onUnhandledFrame: 'error' });
});

afterEach(() => {
  cleanup();
  server.resetHandlers();
  resetWallets();
});

afterAll(() => {
  server.close();
});
