import { describe, expect, it } from 'vitest';
import { formatDate, formatMoney, formatSignedMoney } from '@/shared/money/format';

describe('formatMoney', () => {
  it('formats each supported currency with its symbol', () => {
    expect(formatMoney({ amount: '1250.00', currency: 'EUR' })).toBe('€1,250.00');
    expect(formatMoney({ amount: '0.00', currency: 'USD' })).toBe('$0.00');
    expect(formatMoney({ amount: '99.50', currency: 'GBP' })).toBe('£99.50');
    // Intl puts a no-break space after Kč; compare with plain spaces.
    expect(formatMoney({ amount: '12000.00', currency: 'CZK' }).replace(/\s/g, ' ')).toBe(
      'Kč 12,000.00',
    );
  });

  it('keeps amounts exact beyond floating-point precision', () => {
    expect(formatMoney({ amount: '90071992547409.93', currency: 'EUR' })).toBe(
      '€90,071,992,547,409.93',
    );
  });

  it('shows negative amounts with a minus sign', () => {
    expect(formatMoney({ amount: '-5.00', currency: 'EUR' })).toBe('-€5.00');
  });
});

describe('formatSignedMoney', () => {
  it('marks money in with a plus sign and money out with a minus sign', () => {
    expect(formatSignedMoney({ amount: '250.00', currency: 'EUR' })).toBe('+€250.00');
    expect(formatSignedMoney({ amount: '-40.00', currency: 'EUR' })).toBe('-€40.00');
  });
});

describe('formatDate', () => {
  it('formats a timestamp as a short date', () => {
    expect(formatDate('2026-10-30T12:00:00Z')).toBe('30 Oct 2026');
  });
});
