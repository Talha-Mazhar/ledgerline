import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderRoute } from '@/test/renderRoute';

describe('AppShell', () => {
  it('shows the brand and the main navigation', async () => {
    renderRoute('/');

    const navigation = await screen.findByRole('navigation', { name: 'Main navigation' });
    expect(within(navigation).getByRole('link', { name: 'Overview' })).toHaveAttribute('href', '/');
    expect(screen.getAllByText('Ledgerline').length).toBeGreaterThan(0);
  });

  it('marks the link of the current page as active', async () => {
    renderRoute('/');

    expect(await screen.findByRole('link', { name: 'Overview' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('shows who is signed in and signs out', async () => {
    const user = userEvent.setup();
    renderRoute('/');

    expect(await screen.findByText('demo@ledgerline.dev')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Sign out' }));

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
  });
});
