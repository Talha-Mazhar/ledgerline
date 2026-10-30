import type { RouteObject } from 'react-router';
import { AppShell } from '@/app/layout/AppShell';
import { NotFoundPage } from '@/app/errors/NotFoundPage';
import { RouteErrorPage } from '@/app/errors/RouteErrorPage';
import { OverviewPage } from '@/features/overview/OverviewPage';
import { paths } from '@/shared/config/paths';

/**
 * The route tree. Kept separate from the router instance so tests can mount the same routes in a
 * memory router.
 */
export const routes: RouteObject[] = [
  {
    path: paths.overview,
    element: <AppShell />,
    errorElement: <RouteErrorPage />,
    children: [
      { index: true, element: <OverviewPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
