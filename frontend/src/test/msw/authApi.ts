import { http, HttpResponse } from 'msw';
import type { User } from '@/shared/api/generated';
import { zSignInRequest, zSignUpRequest } from '@/shared/api/generated/zod.gen';
import { API, post, problem } from '@/test/msw/http';

/** A fake auth API for tests. By default the demo user is signed in. */

interface Account {
  readonly user: User;
  readonly password: string;
}

export const DEMO_USER: User = {
  id: '00000000-0000-4000-8000-000000000001',
  email: 'demo@ledgerline.dev',
  displayName: 'Demo user',
  role: 'CUSTOMER',
};

export const JANA: User = {
  id: '00000000-0000-4000-8000-000000000002',
  email: 'jana@ledgerline.dev',
  displayName: 'Jana Nováková',
  role: 'CUSTOMER',
};

export const SUPPORT_USER: User = {
  id: '00000000-0000-4000-8000-000000000003',
  email: 'support@ledgerline.dev',
  displayName: 'Sam Support',
  role: 'SUPPORT',
};

export const ADMIN_USER: User = {
  id: '00000000-0000-4000-8000-000000000004',
  email: 'admin@ledgerline.dev',
  displayName: 'Alex Admin',
  role: 'ADMIN',
};

let accounts: Account[] = [];
let signedIn: User | null = null;

export function resetAuth(): void {
  accounts = [
    { user: DEMO_USER, password: 'ledgerline-demo' },
    { user: JANA, password: 'ledgerline-demo' },
    { user: SUPPORT_USER, password: 'ledgerline-demo' },
    { user: ADMIN_USER, password: 'ledgerline-demo' },
  ];
  signedIn = DEMO_USER;
}

/** Starts the test with this user signed in, e.g. a member of staff. */
export function givenSignedInAs(user: User): void {
  signedIn = user;
}

/** Every account, for the back office's user list. */
export function allUsers(): User[] {
  return accounts.map((account) => account.user);
}

/** Changes a user's role, as an admin would. */
export function setRole(userId: string, role: User['role']): User | undefined {
  const account = accounts.find((candidate) => candidate.user.id === userId);
  if (account === undefined) {
    return undefined;
  }
  const changed = { ...account.user, role };
  accounts = accounts.map((candidate) =>
    candidate === account ? { ...candidate, user: changed } : candidate,
  );
  return changed;
}

/** Starts the test without a session, like a first visit. */
export function givenSignedOut(): void {
  signedIn = null;
}

/** The user found by email, ignoring case, as the backend does. */
export function findUser(email: string): User | undefined {
  return accounts.find((account) => account.user.email === email.trim().toLowerCase())?.user;
}

export function currentUser(): User | null {
  return signedIn;
}

resetAuth();

export const authHandlers = [
  http.get(`${API}/auth/me`, () =>
    signedIn === null
      ? problem(401, 'Sign-in required', 'Sign in to continue.')
      : HttpResponse.json<User>(signedIn),
  ),

  post('/auth/sign-in', async ({ request }) => {
    const { email, password } = zSignInRequest.parse(await request.json());
    const account = accounts.find(
      (candidate) => candidate.user.email === email.trim().toLowerCase(),
    );
    if (account?.password !== password) {
      return problem(401, 'Sign-in failed', 'Wrong email or password.');
    }
    signedIn = account.user;
    return HttpResponse.json<User>(account.user);
  }),

  post('/auth/sign-up', async ({ request }) => {
    const body = zSignUpRequest.parse(await request.json());
    if (findUser(body.email) !== undefined) {
      return problem(
        409,
        'Email already registered',
        'An account with this email already exists. Sign in instead.',
      );
    }
    const user: User = {
      id: crypto.randomUUID(),
      email: body.email.trim().toLowerCase(),
      displayName: body.displayName.trim(),
      role: 'CUSTOMER',
    };
    accounts.push({ user, password: body.password });
    signedIn = user;
    return HttpResponse.json<User>(user, { status: 201 });
  }),

  post('/auth/sign-out', () => {
    signedIn = null;
    return new HttpResponse(null, { status: 204 });
  }),
];
