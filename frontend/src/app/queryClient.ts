import { hashKey, MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';
import { getCurrentUserQueryKey } from '@/shared/api/generated/@tanstack/react-query.gen';
import { problemStatus } from '@/shared/api/problem';

/**
 * One place for server-state defaults. Data counts as fresh for 30 seconds, so moving between pages
 * doesn't refetch everything. A new client per call keeps tests isolated from each other's cache.
 *
 * Failed requests are retried once, but only when trying again could help: a network error or a
 * server error (5xx). A 4xx answer (not signed in, not found, invalid) would be the same again.
 *
 * Any request answered 401 means the session has ended (it expired, or the user signed out in
 * another tab). Then the "who am I" query is checked again; it fails too, and the sign-in guard
 * sends the user to the sign-in page.
 */
export function createQueryClient(): QueryClient {
  const currentUserHash = hashKey(getCurrentUserQueryKey());

  const queryClient: QueryClient = new QueryClient({
    queryCache: new QueryCache({
      onError: (error, query) => {
        if (problemStatus(error) === 401 && query.queryHash !== currentUserHash) {
          sessionEnded();
        }
      },
    }),
    mutationCache: new MutationCache({
      onError: (error) => {
        if (problemStatus(error) === 401) {
          sessionEnded();
        }
      },
    }),
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        retry: (failureCount, error) => failureCount < 1 && worthRetrying(error),
        refetchOnWindowFocus: false,
      },
      mutations: {
        retry: false,
      },
    },
  });

  function sessionEnded() {
    void queryClient.invalidateQueries({ queryKey: getCurrentUserQueryKey() });
  }

  return queryClient;
}

function worthRetrying(error: unknown): boolean {
  const status = problemStatus(error);
  return status === undefined || status >= 500;
}
