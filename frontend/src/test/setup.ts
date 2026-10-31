import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { CSRF_COOKIE, sendCsrfToken } from '@/shared/api/csrf';
import { client } from '@/shared/api/generated/client.gen';
import { resetAuth } from '@/test/msw/authApi';
import { API, CSRF_TOKEN } from '@/test/msw/http';
import { server } from '@/test/msw/server';
import { resetWallets } from '@/test/msw/walletApi';

// Node's fetch needs absolute URLs; in the browser the relative '/api/v1' is enough.
client.setConfig({ baseUrl: API });
// As in the browser: the backend has set the CSRF cookie, and the app copies it into a header.
document.cookie = `${CSRF_COOKIE}=${CSRF_TOKEN}; path=/`;
sendCsrfToken();

beforeAll(() => {
  server.listen({ onUnhandledFrame: 'error' });
});

afterEach(() => {
  cleanup();
  server.resetHandlers();
  resetWallets();
  resetAuth();
});

afterAll(() => {
  server.close();
});
