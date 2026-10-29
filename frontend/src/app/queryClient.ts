import { QueryClient } from '@tanstack/react-query';

/**
 * One place for server-state defaults. Data counts as fresh for 30 seconds, so moving between pages
 * doesn't refetch everything; failed requests are retried once. A new client per call keeps tests
 * isolated from each other's cache.
 */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        retry: 1,
        refetchOnWindowFocus: false,
      },
      mutations: {
        retry: false,
      },
    },
  });
}
