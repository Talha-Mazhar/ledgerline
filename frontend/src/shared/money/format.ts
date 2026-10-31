import type { Money } from '@/shared/api/generated';

/**
 * The app's locale for numbers and dates. Fixed for now so every user (and every test) sees the
 * same format; it becomes the user's setting once profiles exist.
 */
const LOCALE = 'en-GB';

/**
 * Formats a money amount for display: "€1,250.00", "Kč 0.00".
 *
 * The API sends amounts as decimal strings and they are passed to Intl as strings, so they are
 * never converted to a floating-point number on the way.
 */
export function formatMoney(money: Money): string {
  if (!isDecimal(money.amount)) {
    throw new Error(`Not a decimal amount: "${money.amount}"`);
  }
  return new Intl.NumberFormat(LOCALE, {
    style: 'currency',
    currency: money.currency,
    currencyDisplay: 'narrowSymbol',
  }).format(money.amount);
}

/** Like {@link formatMoney}, with a plus sign on money in: "+€250.00", "-€40.00". */
export function formatSignedMoney(money: Money): string {
  const formatted = formatMoney(money);
  return money.amount.startsWith('-') ? formatted : `+${formatted}`;
}

/** True for strings like "12.50" or "-3": the kind of numeric string Intl formats exactly. */
function isDecimal(amount: string): amount is Intl.StringNumericLiteral {
  return /^-?\d+(\.\d+)?$/.test(amount);
}

/** "30 Oct 2026" */
export function formatDate(isoDateTime: string): string {
  return new Intl.DateTimeFormat(LOCALE, { dateStyle: 'medium' }).format(new Date(isoDateTime));
}

/** "30 October 2026 at 14:05" */
export function formatDateTime(isoDateTime: string): string {
  return new Intl.DateTimeFormat(LOCALE, { dateStyle: 'long', timeStyle: 'short' }).format(
    new Date(isoDateTime),
  );
}
