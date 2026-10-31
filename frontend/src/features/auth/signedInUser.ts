import { createContext, useContext } from 'react';
import type { User } from '@/shared/api/generated';

/** The signed-in user, provided by RequireAuth to every page behind sign-in. */
export const SignedInUserContext = createContext<User | null>(null);

/** The signed-in user. Only for components rendered inside RequireAuth. */
export function useSignedInUser(): User {
  const user = useContext(SignedInUserContext);
  if (user === null) {
    throw new Error('useSignedInUser is used outside RequireAuth');
  }
  return user;
}
