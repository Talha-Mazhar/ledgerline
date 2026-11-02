import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { givenWallet } from '@/test/msw/walletApi';
import { first } from '@/test/first';
import { renderRoute } from '@/test/renderRoute';

async function send(amount: string) {
  const user = userEvent.setup();
  const wallet = givenWallet('EUR', '5000.00');
  renderRoute(`/wallets/${wallet.id}`);
  await user.click(await screen.findByRole('button', { name: 'Send' }));
  const dialog = screen.getByRole('dialog', { name: 'Send money' });
  await user.type(within(dialog).getByLabelText("Recipient's email"), 'jana@ledgerline.dev');
  await user.type(within(dialog).getByLabelText('Amount (EUR)'), amount);
  await user.click(within(dialog).getByRole('button', { name: 'Send' }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  return user;
}

describe('Payments', () => {
  it('lists the payments with their status', async () => {
    const user = await send('30');

    await user.click(first(screen.getAllByRole('link', { name: 'Payments' })));

    const table = await screen.findByRole('table', { name: 'Payments' });
    expect(await within(table).findByText('To Jana Nováková')).toBeInTheDocument();
    expect(within(table).getByText('Completed')).toBeInTheDocument();
    expect(within(table).getByText('-€30.00')).toBeInTheDocument();
  });

  it('says a large transfer waits for approval, and why', async () => {
    const user = await send('1500');

    expect(
      await screen.findByText(
        "€1,500.00 to Jana Nováková is waiting for approval. We've set it aside from your balance.",
      ),
    ).toBeInTheDocument();
    const ledger = screen.getByRole('table', { name: 'Activity' });
    expect(
      await within(ledger).findByText('Set aside: transfer to Jana Nováková'),
    ).toBeInTheDocument();

    await user.click(first(screen.getAllByRole('link', { name: 'Payments' })));
    const table = await screen.findByRole('table', { name: 'Payments' });
    expect(await within(table).findByText('Waiting for approval')).toBeInTheDocument();
    expect(
      within(table).getByText(/Above the single-payment limit of €1,000.00/),
    ).toBeInTheDocument();
  });

  it('explains an empty list', async () => {
    renderRoute('/payments');

    expect(await screen.findByText('No payments yet')).toBeInTheDocument();
  });
});
