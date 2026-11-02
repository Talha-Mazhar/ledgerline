import type { RouteObject } from 'react-router';
import { AppShell } from '@/app/layout/AppShell';
import { NotFoundPage } from '@/app/errors/NotFoundPage';
import { RouteErrorPage } from '@/app/errors/RouteErrorPage';
import { RequireAuth } from '@/features/auth/RequireAuth';
import { RequireCustomer, RequireStaff } from '@/features/auth/RequireRole';
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
 * visitors without a session to the sign-in page. Customer pages and back-office pages are split
 * by role. Pages beyond the overview and wallets load on first visit, so a customer never downloads
 * the back office.
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
          {
            element: <RequireCustomer />,
            children: [
              { index: true, element: <OverviewPage /> },
              { path: paths.wallet, element: <WalletDetailPage /> },
              {
                path: paths.payments,
                lazy: async () => ({
                  Component: (await import('@/features/payments/PaymentsPage')).PaymentsPage,
                }),
              },
              {
                path: paths.verification,
                lazy: async () => ({
                  Component: (await import('@/features/verification/VerificationPage'))
                    .VerificationPage,
                }),
              },
              {
                path: paths.assistant,
                lazy: async () => ({
                  Component: (await import('@/features/assistant/AssistantPage')).AssistantPage,
                }),
              },
            ],
          },
          {
            element: <RequireStaff />,
            children: [
              {
                path: paths.approvals,
                lazy: async () => ({
                  Component: (await import('@/features/back-office/ApprovalsPage')).ApprovalsPage,
                }),
              },
              {
                path: paths.reviews,
                lazy: async () => ({
                  Component: (await import('@/features/back-office/VerificationsPage'))
                    .VerificationsPage,
                }),
              },
              {
                path: paths.users,
                lazy: async () => ({
                  Component: (await import('@/features/back-office/UsersPage')).UsersPage,
                }),
              },
              {
                path: paths.audit,
                lazy: async () => ({
                  Component: (await import('@/features/back-office/AuditPage')).AuditPage,
                }),
              },
            ],
          },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
];
