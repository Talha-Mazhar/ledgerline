/**
 * The accounts the backend's V5 migration creates, so the app can be tried without signing up.
 * They exist only in this demo; a real deployment would leave them out.
 */
export const demoAccount = {
  email: 'demo@ledgerline.dev',
  password: 'ledgerline-demo',
} as const;

/** The second demo account, someone to send money to. */
export const demoRecipient = 'jana@ledgerline.dev';
