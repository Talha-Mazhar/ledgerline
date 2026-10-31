import { setupServer } from 'msw/node';
import { authHandlers } from '@/test/msw/authApi';
import { walletHandlers } from '@/test/msw/walletApi';

/** Intercepts the app's HTTP requests in tests. A request with no handler fails the test. */
export const server = setupServer(...authHandlers, ...walletHandlers);
