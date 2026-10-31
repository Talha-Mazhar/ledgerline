import { client } from '@/shared/api/generated/client.gen';

/** Methods that only read; every other request must prove it comes from Ledgerline's own pages. */
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);

/** The cookie the backend puts the CSRF token in, and the header it expects the token back in. */
export const CSRF_COOKIE = 'XSRF-TOKEN';
export const CSRF_HEADER = 'X-XSRF-TOKEN';

/** The value of a cookie this page can read, or undefined. */
export function readCookie(name: string): string | undefined {
  const prefix = `${name}=`;
  const cookie = document.cookie.split('; ').find((entry) => entry.startsWith(prefix));
  return cookie === undefined ? undefined : decodeURIComponent(cookie.slice(prefix.length));
}

/**
 * Makes every request that changes something carry the CSRF token.
 *
 * Why: the session lives in a cookie, and the browser attaches cookies to requests no matter which
 * site started them. A malicious page could make the browser POST to Ledgerline as the signed-in
 * user. It can't read Ledgerline's cookies, though, so it can't copy the token into the header;
 * the backend refuses any change without it.
 *
 * The token is read from the cookie on each request, not once at start-up: the backend replaces it
 * at sign-in.
 */
export function sendCsrfToken(): void {
  client.interceptors.request.use((request) => {
    if (!SAFE_METHODS.has(request.method)) {
      const token = readCookie(CSRF_COOKIE);
      if (token !== undefined) {
        request.headers.set(CSRF_HEADER, token);
      }
    }
    return request;
  });
}
