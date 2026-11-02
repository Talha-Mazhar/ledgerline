import { http, HttpResponse, type HttpResponseResolver, type PathParams } from 'msw';
import type { Problem } from '@/shared/api/generated';

/** The base URL of the fake API. */
export const API = 'http://localhost/api/v1';

/** The CSRF token the tests' "browser" holds, in the cookie the app reads it from. */
export const CSRF_TOKEN = 'test-csrf-token';

/** A problem-details answer, as the backend sends for every error. */
export function problem(status: number, title: string, detail: string) {
  const body: Problem = { type: 'about:blank', title, status, detail };
  return HttpResponse.json(body, {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  });
}

/**
 * A POST handler that, like the backend, refuses the request without the CSRF token in the
 * X-XSRF-TOKEN header. So every test that posts also checks the app sends the token.
 */
export function post<Params extends PathParams>(
  path: string,
  resolver: HttpResponseResolver<Params>,
) {
  return http.post<Params>(`${API}${path}`, (info) =>
    info.request.headers.get('X-XSRF-TOKEN') === CSRF_TOKEN ? resolver(info) : refused(),
  );
}

/** Like {@link post}, for PUT. */
export function put<Params extends PathParams>(
  path: string,
  resolver: HttpResponseResolver<Params>,
) {
  return http.put<Params>(`${API}${path}`, (info) =>
    info.request.headers.get('X-XSRF-TOKEN') === CSRF_TOKEN ? resolver(info) : refused(),
  );
}

function refused() {
  return problem(403, 'Request refused', 'The request was refused. Reload the page and try again.');
}
