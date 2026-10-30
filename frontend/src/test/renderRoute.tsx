import { render, type RenderResult } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { AppProviders } from '@/app/AppProviders';
import { createQueryClient } from '@/app/queryClient';
import { routes } from '@/app/routes';

/**
 * Renders the real app (providers and routes) at a given URL, the way a user would arrive there.
 * Each call gets a fresh query cache, so tests never share data, and failed requests are not
 * retried, so a test sees an error at once instead of after the retry delay.
 */
export function renderRoute(path: string): RenderResult {
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  const queryClient = createQueryClient();
  queryClient.setDefaultOptions({
    queries: { ...queryClient.getDefaultOptions().queries, retry: false },
  });
  return render(
    <AppProviders queryClient={queryClient}>
      <RouterProvider router={router} />
    </AppProviders>,
  );
}
