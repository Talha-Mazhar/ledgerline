import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { givenWallet } from '@/test/msw/walletApi';
import { renderRoute } from '@/test/renderRoute';

describe('Adding and withdrawing money', () => {
  it('adds money and shows the new balance and activity line', async () => {
    const user = userEvent.setup();
    const wallet = givenWallet('EUR');
    renderRoute(`/wallets/${wallet.id}`);

    await user.click(await screen.findByRole('button', { name: 'Add money' }));
    const dialog = screen.getByRole('dialog', { name: 'Add money' });
    await user.type(within(dialog).getByLabelText('Amount (EUR)'), '250');
    await user.click(within(dialog).getByRole('button', { name: 'Add money' }));

    expect(await screen.findByText('€250.00 added')).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    const ledger = screen.getByRole('table', { name: 'Activity' });
    expect(await within(ledger).findByText('+€250.00')).toBeInTheDocument();
    expect(within(ledger).getByText('Added money')).toBeInTheDocument();
  });

  it('explains what to type when the amount is not a number', async () => {
    const user = userEvent.setup();
    const wallet = givenWallet('EUR');
    renderRoute(`/wallets/${wallet.id}`);

    await user.click(await screen.findByRole('button', { name: 'Add money' }));
    const dialog = screen.getByRole('dialog', { name: 'Add money' });
    await user.type(within(dialog).getByLabelText('Amount (EUR)'), '12.345');
    await user.click(within(dialog).getByRole('button', { name: 'Add money' }));

    expect(
      await within(dialog).findByText(
        'Enter an amount like 250 or 250.50, with at most two decimals.',
      ),
    ).toBeInTheDocument();
  });

  it('shows the reason when a withdrawal is more than the wallet holds', async () => {
    const user = userEvent.setup();
    const wallet = givenWallet('GBP', '20.00');
    renderRoute(`/wallets/${wallet.id}`);

    await user.click(await screen.findByRole('button', { name: 'Withdraw' }));
    const dialog = screen.getByRole('dialog', { name: 'Withdraw' });
    expect(within(dialog).getByText(/Available: £20\.00/)).toBeInTheDocument();
    await user.type(within(dialog).getByLabelText('Amount (GBP)'), '25');
    await user.click(within(dialog).getByRole('button', { name: 'Withdraw' }));

    expect(
      await within(dialog).findByText("This wallet has 20.00 GBP available, which isn't enough."),
    ).toBeInTheDocument();
  });
});
