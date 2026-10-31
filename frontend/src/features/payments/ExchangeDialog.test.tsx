import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { givenWallet } from '@/test/msw/walletApi';
import { renderRoute } from '@/test/renderRoute';

describe('Exchanging money', () => {
  it('previews the exchange as you type, then exchanges', async () => {
    const user = userEvent.setup();
    const euros = givenWallet('EUR', '100.00');
    givenWallet('CZK');
    renderRoute(`/wallets/${euros.id}`);

    await user.click(await screen.findByRole('button', { name: 'Exchange' }));
    const dialog = screen.getByRole('dialog', { name: 'Exchange' });
    await user.type(within(dialog).getByLabelText('You sell (EUR)'), '100');

    expect(
      await within(dialog).findByText('Kč 2,502.42', { normalizer: plain }),
    ).toBeInTheDocument();
    expect(within(dialog).getByText('1 EUR = 25.024250 CZK, Ledgerline rate')).toBeInTheDocument();

    await user.click(within(dialog).getByRole('button', { name: 'Exchange' }));

    expect(
      await screen.findByText('Exchanged €100.00 to Kč 2,502.42', { normalizer: plain }),
    ).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await screen.findByText('Exchanged to CZK')).toBeInTheDocument();
  });

  it('asks for a second currency when there is only one wallet', async () => {
    const user = userEvent.setup();
    const euros = givenWallet('EUR', '100.00');
    renderRoute(`/wallets/${euros.id}`);

    await user.click(await screen.findByRole('button', { name: 'Exchange' }));

    expect(
      within(screen.getByRole('dialog')).getByText(/Open a wallet in another currency first/),
    ).toBeInTheDocument();
  });
});

/** Intl puts a no-break space after Kč; compare text with plain spaces. */
function plain(text: string): string {
  return text.replace(/\s+/g, ' ').trim();
}
