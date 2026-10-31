import { useQueryClient } from '@tanstack/react-query';
import { useLocation, useNavigate } from 'react-router';
import { z } from 'zod';
import type { User } from '@/shared/api/generated';
import { getCurrentUserQueryKey } from '@/shared/api/generated/@tanstack/react-query.gen';
import { paths } from '@/shared/config/paths';

const returnTo = z.object({ from: z.string().startsWith('/') });

/**
 * Where to go after signing in: the page RequireAuth was protecting when it sent the user to sign
 * in (kept in the navigation state), or the overview.
 */
export function returnPath(state: unknown): string {
  const target = returnTo.safeParse(state);
  return target.success ? target.data.from : paths.overview;
}

/**
 * What happens once the backend has started a session (sign-in or sign-up): anything cached for
 * a previous user is dropped, the new user is put in the cache, and the user goes to the page they
 * originally asked for, or the overview.
 */
export function useStartSession(): (user: User) => void {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const location = useLocation();

  return (user) => {
    queryClient.removeQueries();
    queryClient.setQueryData(getCurrentUserQueryKey(), user);
    void navigate(returnPath(location.state), { replace: true });
  };
}
