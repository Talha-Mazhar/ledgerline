import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { givenWallet } from '@/test/msw/walletApi';
import { renderRoute } from '@/test/renderRoute';

describe('Statements', () => {
  it('offers each month as a PDF, newest first', async () => {
    const wallet = givenWallet('EUR', '100.00');
    renderRoute(`/wallets/${wallet.id}`);

    const section = await screen.findByRole('region', { name: 'Statements' });
    const links = await within(section).findAllByRole('link');
    expect(links.map((link) => link.textContent)).toEqual(['October 2026', 'September 2026']);
    expect(links[0]).toHaveAttribute(
      'href',
      `http://localhost/api/v1/wallets/${wallet.id}/statements/2026-10`,
    );
  });
});
