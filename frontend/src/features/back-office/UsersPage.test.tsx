import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { ADMIN_USER, SUPPORT_USER, givenSignedInAs } from '@/test/msw/authApi';
import { renderRoute } from '@/test/renderRoute';

describe('Users', () => {
  it('finds people by name or email', async () => {
    const user = userEvent.setup();
    givenSignedInAs(SUPPORT_USER);
    renderRoute('/back-office/users');

    const table = await screen.findByRole('table', { name: 'Users' });
    expect(await within(table).findByText('jana@ledgerline.dev')).toBeInTheDocument();
    await user.type(screen.getByLabelText('Search by name or email'), 'jana');

    await waitFor(() => expect(within(table).getAllByRole('row')).toHaveLength(2));
    expect(within(table).getByText('Jana Nováková')).toBeInTheDocument();
  });

  it('lets an admin change a role', async () => {
    const user = userEvent.setup();
    givenSignedInAs(ADMIN_USER);
    renderRoute('/back-office/users');

    await user.click(await screen.findByRole('combobox', { name: 'Role of Jana Nováková' }));
    await user.click(screen.getByRole('option', { name: 'Support' }));

    expect(await screen.findByText('Jana Nováková is now support.')).toBeInTheDocument();
  });

  it('refuses to demote the last admin', async () => {
    const user = userEvent.setup();
    givenSignedInAs(ADMIN_USER);
    renderRoute('/back-office/users');

    await user.click(await screen.findByRole('combobox', { name: 'Role of Alex Admin' }));
    await user.click(screen.getByRole('option', { name: 'Customer' }));

    expect(await screen.findByText('Make someone else an admin first.')).toBeInTheDocument();
  });

  it('shows support staff the roles without letting them change them', async () => {
    givenSignedInAs(SUPPORT_USER);
    renderRoute('/back-office/users');

    expect(
      await screen.findByText('Find a customer or colleague. Only admins can change roles.'),
    ).toBeInTheDocument();
    await screen.findByText('jana@ledgerline.dev');
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });
});
