import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { givenNotification } from '@/test/msw/paymentsApi';
import { renderRoute } from '@/test/renderRoute';

// The bell is in the sidebar and, on narrow screens, in the top bar; tests use the first.
async function bell(name: string | RegExp) {
  const [first] = await screen.findAllByRole('button', { name });
  if (first === undefined) {
    throw new Error('No notifications button');
  }
  return first;
}

describe('Notifications', () => {
  it('shows how many are unread', async () => {
    givenNotification('Money received', 'Jana Nováková sent you €30.00.');
    givenNotification('Payment waiting for approval', 'Your €1,500.00 transfer is being checked.');
    renderRoute('/');

    expect(await bell('Notifications, 2 unread')).toBeInTheDocument();
  });

  it('opens a notification, marks it read and goes to its page', async () => {
    const user = userEvent.setup();
    givenNotification('Your statement is ready', 'October is ready to download.', '/payments');
    renderRoute('/');

    await user.click(await bell('Notifications, 1 unread'));
    const panel = screen.getByRole('dialog', { name: 'Notifications' });
    await user.click(within(panel).getByText('Your statement is ready'));

    expect(await screen.findByRole('heading', { level: 1, name: 'Payments' })).toBeInTheDocument();
    expect(await bell('Notifications')).toBeInTheDocument();
  });

  it('marks all as read', async () => {
    const user = userEvent.setup();
    givenNotification('Money added', '€100.00 is in your EUR wallet.');
    givenNotification('Exchange done', 'You exchanged €50.00 to CZK.');
    renderRoute('/');

    await user.click(await bell('Notifications, 2 unread'));
    await user.click(screen.getByRole('button', { name: 'Mark all read' }));

    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Mark all read' })).toBeDisabled(),
    );
    expect(screen.queryByLabelText('Unread')).not.toBeInTheDocument();
  });

  it('says when there is nothing yet', async () => {
    const user = userEvent.setup();
    renderRoute('/');

    await user.click(await bell('Notifications'));

    expect(
      await screen.findByText('Nothing yet. Payments, approvals and statements show up here.'),
    ).toBeInTheDocument();
  });
});
