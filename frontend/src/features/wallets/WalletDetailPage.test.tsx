import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { givenWallet } from '@/test/msw/walletApi';
import { renderRoute } from '@/test/renderRoute';

describe('WalletDetailPage', () => {
  it('shows the wallet, its balance and its details', async () => {
    const wallet = givenWallet('GBP', '99.50');
    renderRoute(`/wallets/${wallet.id}`);

    expect(
      await screen.findByRole('heading', { level: 1, name: 'British pound wallet' }),
    ).toBeInTheDocument();
    expect(screen.getByText('£99.50')).toBeInTheDocument();
    expect(screen.getByText('British pound (GBP)')).toBeInTheDocument();
    expect(screen.getByText(wallet.id)).toBeInTheDocument();
  });

  it('says so when the wallet does not exist', async () => {
    renderRoute('/wallets/00000000-0000-4000-8000-000000000999');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Wallet not found' }),
    ).toBeInTheDocument();
    expect(
      within(screen.getByRole('main')).getByRole('link', { name: 'Overview' }),
    ).toHaveAttribute('href', '/');
  });
});
