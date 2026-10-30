/**
 * Every route path in one place. Links and routes use these constants, so a typo is a type error
 * and renaming a path is a one-line change.
 */
export const paths = {
  overview: '/',
  wallet: '/wallets/:walletId',
} as const;

export type AppPath = (typeof paths)[keyof typeof paths];

/** The URL of one wallet's page. */
export function walletPath(walletId: string): string {
  return paths.wallet.replace(':walletId', encodeURIComponent(walletId));
}
