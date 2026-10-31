import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '@/test/msw/server';
import { API, givenWallet } from '@/test/msw/walletApi';
import { renderRoute } from '@/test/renderRoute';

describe('OverviewPage', () => {
  it('is the page at the root URL', async () => {
    renderRoute('/');

    expect(screen.getByRole('heading', { level: 1, name: 'Overview' })).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Open EUR wallet' })).toBeInTheDocument();
  });

  it('shows the balance of each wallet the user holds', async () => {
    givenWallet('EUR', '1250.00');
    renderRoute('/');

    const euro = await screen.findByRole('article', { name: 'Euro (EUR)' });
    expect(await within(euro).findByText('€1,250.00')).toBeInTheDocument();
    expect(within(euro).getByRole('link', { name: 'View Euro wallet' })).toHaveAttribute(
      'href',
      '/wallets/00000000-0000-4000-8000-000000000001',
    );
  });

  it('offers to open every currency the user does not hold', async () => {
    givenWallet('USD');
    renderRoute('/');

    expect(await screen.findByRole('button', { name: 'Open EUR wallet' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Open CZK wallet' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Open GBP wallet' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Open USD wallet' })).not.toBeInTheDocument();
  });

  it('opens a wallet and shows its balance', async () => {
    const user = userEvent.setup();
    renderRoute('/');

    await user.click(await screen.findByRole('button', { name: 'Open CZK wallet' }));
    const dialog = screen.getByRole('dialog', { name: 'Open a wallet' });
    expect(within(dialog).getByRole('radio', { name: 'Czech koruna' })).toBeChecked();
    await user.click(within(dialog).getByRole('button', { name: 'Open wallet' }));

    expect(await screen.findByText('Czech koruna wallet opened')).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    const koruna = screen.getByRole('article', { name: 'Czech koruna (CZK)' });
    expect(
      await within(koruna).findByRole('link', { name: 'View Czech koruna wallet' }),
    ).toBeInTheDocument();
  });

  it('does not let the user pick a currency they already hold', async () => {
    const user = userEvent.setup();
    givenWallet('EUR');
    renderRoute('/');

    await user.click(await screen.findByRole('button', { name: 'Open wallet' }));
    const dialog = screen.getByRole('dialog', { name: 'Open a wallet' });
    expect(within(dialog).getByRole('radio', { name: 'Euro' })).toBeDisabled();
    expect(within(dialog).getByRole('radio', { name: 'US dollar' })).toBeChecked();
  });

  it('shows the reason when the server refuses to open a wallet', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(`${API}/wallets`, () =>
        HttpResponse.json(
          {
            type: 'about:blank',
            title: 'Wallet already open',
            status: 409,
            detail: 'You already have a GBP wallet.',
          },
          { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    renderRoute('/');

    await user.click(await screen.findByRole('button', { name: 'Open GBP wallet' }));
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Open wallet' }),
    );

    expect(await screen.findByText('You already have a GBP wallet.')).toBeInTheDocument();
  });

  it('explains the failure and retries when the wallets cannot be loaded', async () => {
    const user = userEvent.setup();
    server.use(http.get(`${API}/wallets`, () => HttpResponse.error(), { once: true }));
    renderRoute('/');

    const alert = await screen.findByRole('alert');
    expect(within(alert).getByText("Your wallets couldn't be loaded")).toBeInTheDocument();

    await user.click(within(alert).getByRole('button', { name: 'Try again' }));
    expect(await screen.findByRole('button', { name: 'Open EUR wallet' })).toBeInTheDocument();
  });
});
