import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { givenWallet } from '@/test/msw/walletApi';
import { renderRoute } from '@/test/renderRoute';

async function openSendDialog(currency: 'EUR' | 'USD', balance: string) {
  const user = userEvent.setup();
  const wallet = givenWallet(currency, balance);
  renderRoute(`/wallets/${wallet.id}`);
  await user.click(await screen.findByRole('button', { name: 'Send' }));
  return { user, dialog: screen.getByRole('dialog', { name: 'Send money' }) };
}

describe('Sending money', () => {
  it('sends money to another user and records it in the activity', async () => {
    const { user, dialog } = await openSendDialog('EUR', '100.00');

    await user.type(within(dialog).getByLabelText("Recipient's email"), 'jana@ledgerline.dev');
    await user.type(within(dialog).getByLabelText('Amount (EUR)'), '30');
    await user.click(within(dialog).getByRole('button', { name: 'Send' }));

    expect(await screen.findByText('€30.00 sent to Jana Nováková')).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    const ledger = screen.getByRole('table', { name: 'Activity' });
    expect(await within(ledger).findByText('Sent to Jana Nováková')).toBeInTheDocument();
    expect(within(ledger).getByText('-€30.00')).toBeInTheDocument();
  });

  it('explains when nobody uses the email', async () => {
    const { user, dialog } = await openSendDialog('EUR', '100.00');

    await user.type(within(dialog).getByLabelText("Recipient's email"), 'nobody@example.com');
    await user.type(within(dialog).getByLabelText('Amount (EUR)'), '10');
    await user.click(within(dialog).getByRole('button', { name: 'Send' }));

    expect(
      await within(dialog).findByText('Nobody on Ledgerline uses this email.'),
    ).toBeInTheDocument();
  });

  it('explains when the recipient has no wallet in the currency', async () => {
    const { user, dialog } = await openSendDialog('USD', '50.00');

    await user.type(within(dialog).getByLabelText("Recipient's email"), 'jana@ledgerline.dev');
    await user.type(within(dialog).getByLabelText('Amount (USD)'), '10');
    await user.click(within(dialog).getByRole('button', { name: 'Send' }));

    expect(
      await within(dialog).findByText('Jana Nováková has no USD wallet yet.'),
    ).toBeInTheDocument();
  });
});
