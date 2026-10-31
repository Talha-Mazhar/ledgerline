import { zProblem } from '@/shared/api/generated/zod.gen';

/**
 * The message to show for a failed request. API errors are RFC 9457 problem details, and their
 * `detail` is written for users ("You already have a USD wallet."), so it is shown as is. Anything
 * else (the network is down, the server crashed) gets the caller's fallback.
 */
export function problemMessage(error: unknown, fallback: string): string {
  const problem = zProblem.safeParse(error);
  return problem.success && problem.data.detail !== undefined ? problem.data.detail : fallback;
}

/** The HTTP status of a problem-details error, or undefined for any other failure. */
export function problemStatus(error: unknown): number | undefined {
  const problem = zProblem.safeParse(error);
  return problem.success ? problem.data.status : undefined;
}
