import type { RouteObject } from 'react-router';
import { AppShell } from '@/app/layout/AppShell';
import { NotFoundPage } from '@/app/errors/NotFoundPage';
import { RouteErrorPage } from '@/app/errors/RouteErrorPage';
import { RequireAuth } from '@/features/auth/RequireAuth';
import { SignInPage } from '@/features/auth/SignInPage';
import { SignUpPage } from '@/features/auth/SignUpPage';
import { OverviewPage } from '@/features/overview/OverviewPage';
import { WalletDetailPage } from '@/features/wallets/WalletDetailPage';
import { paths } from '@/shared/config/paths';

/**
 * The route tree. Kept separate from the router instance so tests can mount the same routes in a
 * memory router.
 *
 * Sign-in and sign-up are open to everyone; everything else sits behind RequireAuth, which sends
 * visitors without a session to the sign-in page.
 */
export const routes: RouteObject[] = [
  { path: paths.signIn, element: <SignInPage />, errorElement: <RouteErrorPage /> },
  { path: paths.signUp, element: <SignUpPage />, errorElement: <RouteErrorPage /> },
  {
    element: <RequireAuth />,
    errorElement: <RouteErrorPage />,
    children: [
      {
        path: paths.overview,
        element: <AppShell />,
        children: [
          { index: true, element: <OverviewPage /> },
          { path: paths.wallet, element: <WalletDetailPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
];
