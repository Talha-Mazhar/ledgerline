import type { CurrencyCode } from '@/shared/api/generated';

export interface CurrencyInfo {
  readonly code: CurrencyCode;
  /** Name in a sentence: "Euro", "Czech koruna". */
  readonly name: string;
  readonly symbol: string;
  /** Colour of the currency's mark, so a currency is recognisable at a glance across the app. */
  readonly tone: string;
}

/**
 * Display details for every currency in the API contract. `satisfies Record<CurrencyCode, …>`
 * makes this a type error the day the contract gains a currency that isn't described here.
 */
export const currencies = {
  EUR: { code: 'EUR', name: 'Euro', symbol: '€', tone: '#2446d8' },
  USD: { code: 'USD', name: 'US dollar', symbol: '$', tone: '#0b7a5c' },
  CZK: { code: 'CZK', name: 'Czech koruna', symbol: 'Kč', tone: '#a3245a' },
  GBP: { code: 'GBP', name: 'British pound', symbol: '£', tone: '#5f45c2' },
} as const satisfies Record<CurrencyCode, CurrencyInfo>;

/** The order currencies appear in everywhere. */
export const currencyCodes: readonly CurrencyCode[] = ['EUR', 'USD', 'CZK', 'GBP'];
