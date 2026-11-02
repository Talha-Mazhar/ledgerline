import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { ADMIN_USER, DEMO_USER, JANA, SUPPORT_USER, givenSignedInAs } from '@/test/msw/authApi';
import { recordPayment } from '@/test/msw/paymentsApi';
import { renderRoute } from '@/test/renderRoute';

function givenHeldTransfer() {
  return recordPayment({
    customer: DEMO_USER,
    counterparty: JANA,
    kind: 'TRANSFER',
    amount: { amount: '1500.00', currency: 'EUR' },
    description: 'To Jana Nováková',
    status: 'HELD',
    holdReason: 'Above the single-payment limit of €1,000.00',
  });
}

describe('Approvals', () => {
  it('lists held payments with the reason they were held', async () => {
    givenHeldTransfer();
    givenSignedInAs(SUPPORT_USER);
    renderRoute('/back-office/approvals');

    const table = await screen.findByRole('table', { name: 'Payments' });
    expect(await within(table).findByText('Demo user')).toBeInTheDocument();
    expect(within(table).getByText('Transfer to Jana Nováková')).toBeInTheDocument();
    expect(
      within(table).getByText('Held: Above the single-payment limit of €1,000.00'),
    ).toBeInTheDocument();
    expect(within(table).getByText('Waiting for approval')).toBeInTheDocument();
  });

  it('needs two different people to release a payment', async () => {
    const user = userEvent.setup();
    givenHeldTransfer();
    givenSignedInAs(SUPPORT_USER);
    renderRoute('/back-office/approvals');

    const approve = { name: 'Approve €1,500.00 from Demo user' };
    await user.click(await screen.findByRole('button', approve));
    expect(
      await screen.findByText(
        'First approval given. A second person must approve €1,500.00 from Demo user.',
      ),
    ).toBeInTheDocument();
    expect(await screen.findByText('Waiting for second approval')).toBeInTheDocument();

    // The same person can't give the second approval.
    await user.click(screen.getByRole('button', approve));
    expect(
      await screen.findByText(
        'You already approved this payment. A second, different person must approve it.',
      ),
    ).toBeInTheDocument();
  });

  it('releases the payment on a second approval by someone else', async () => {
    const user = userEvent.setup();
    givenHeldTransfer();
    givenSignedInAs(SUPPORT_USER);
    const first = renderRoute('/back-office/approvals');
    await user.click(
      await screen.findByRole('button', { name: 'Approve €1,500.00 from Demo user' }),
    );
    await screen.findByText('Waiting for second approval');
    first.unmount();

    givenSignedInAs(ADMIN_USER);
    renderRoute('/back-office/approvals');
    await user.click(
      await screen.findByRole('button', { name: 'Approve €1,500.00 from Demo user' }),
    );

    expect(
      await screen.findByText('Second approval given: €1,500.00 from Demo user is released.'),
    ).toBeInTheDocument();
    expect(await screen.findByText('Nothing to approve')).toBeInTheDocument();
  });

  it('rejects a payment with a reason', async () => {
    const user = userEvent.setup();
    givenHeldTransfer();
    givenSignedInAs(SUPPORT_USER);
    renderRoute('/back-office/approvals');

    await user.click(
      await screen.findByRole('button', { name: 'Reject €1,500.00 from Demo user' }),
    );
    const dialog = screen.getByRole('dialog', { name: 'Reject payment' });
    await user.click(within(dialog).getByRole('button', { name: 'Reject' }));
    expect(
      within(dialog).getByText('Give a reason; the customer will see it.'),
    ).toBeInTheDocument();

    await user.type(within(dialog).getByLabelText('Reason'), 'Recipient could not be confirmed');
    await user.click(within(dialog).getByRole('button', { name: 'Reject' }));

    expect(
      await screen.findByText('Rejected; €1,500.00 is back with Demo user.'),
    ).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    await user.click(screen.getByRole('tab', { name: 'All payments' }));
    expect(
      await screen.findByText('Rejected: Recipient could not be confirmed'),
    ).toBeInTheDocument();
  });

  it('refunds a completed payment', async () => {
    const user = userEvent.setup();
    recordPayment({
      customer: DEMO_USER,
      kind: 'DEPOSIT',
      amount: { amount: '100.00', currency: 'EUR' },
      description: 'Added money',
    });
    givenSignedInAs(SUPPORT_USER);
    renderRoute('/back-office/approvals');

    await user.click(await screen.findByRole('tab', { name: 'All payments' }));
    await user.click(await screen.findByRole('button', { name: 'Refund €100.00 to Demo user' }));

    expect(await screen.findByText('Refunded €100.00 to Demo user.')).toBeInTheDocument();
    expect(await screen.findByText('Refunded')).toBeInTheDocument();
  });
});

describe('Back office access', () => {
  it('shows staff the back office instead of wallets', async () => {
    givenSignedInAs(SUPPORT_USER);
    renderRoute('/');

    expect(await screen.findByRole('heading', { level: 1, name: 'Approvals' })).toBeInTheDocument();
    const navigation = screen.getByRole('navigation', { name: 'Main navigation' });
    expect(within(navigation).getByRole('heading', { name: 'Back office' })).toBeInTheDocument();
    expect(within(navigation).getByRole('link', { name: 'Audit log' })).toBeInTheDocument();
    expect(within(navigation).queryByRole('link', { name: 'Overview' })).not.toBeInTheDocument();
  });

  it('keeps customers out of the back office', async () => {
    renderRoute('/back-office/approvals');

    expect(await screen.findByRole('heading', { level: 1, name: 'Overview' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Approvals' })).not.toBeInTheDocument();
  });
});
