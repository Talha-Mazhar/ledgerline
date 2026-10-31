import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import { useQuery } from '@tanstack/react-query';
import { Navigate, Outlet, useLocation } from 'react-router';
import { SignedInUserContext } from '@/features/auth/signedInUser';
import { getCurrentUserOptions } from '@/shared/api/generated/@tanstack/react-query.gen';
import { problemStatus } from '@/shared/api/problem';
import { paths } from '@/shared/config/paths';
import { ErrorState } from '@/shared/ui/ErrorState';

/**
 * Guards every page behind sign-in. It asks the backend who is signed in (`GET /auth/me`):
 *
 * - a user → the page renders, with the user available through `useSignedInUser`;
 * - 401 → off to the sign-in page, remembering where the user wanted to go;
 * - anything else (the server is down) → an error with a retry, not a sign-in page that wouldn't
 *   work either.
 *
 * The answer is cached for good (`staleTime: Infinity`); it is checked again only when another
 * request gets a 401 (see `createQueryClient`).
 */
export function RequireAuth() {
  const location = useLocation();
  const session = useQuery({ ...getCurrentUserOptions(), retry: false, staleTime: Infinity });

  if (session.isError) {
    if (problemStatus(session.error) === 401) {
      return (
        <Navigate
          to={paths.signIn}
          replace
          state={{ from: `${location.pathname}${location.search}` }}
        />
      );
    }
    return (
      <Box sx={{ maxWidth: 560, mx: 'auto', px: 2, py: 8 }}>
        <ErrorState
          title="Ledgerline can't be reached"
          description="The server didn't answer. Check your connection and try again."
          onRetry={() => void session.refetch()}
        />
      </Box>
    );
  }

  if (session.data === undefined) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', minHeight: '100vh' }}>
        <CircularProgress aria-label="Loading your account" />
      </Box>
    );
  }

  return (
    <SignedInUserContext value={session.data}>
      <Outlet />
    </SignedInUserContext>
  );
}
