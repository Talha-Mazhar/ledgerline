import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderRoute } from '@/test/renderRoute';

describe('AppShell', () => {
  it('shows the brand and the main navigation', () => {
    renderRoute('/');

    expect(screen.getByText('Ledgerline')).toBeInTheDocument();
    const navigation = screen.getByRole('navigation', { name: 'Main navigation' });
    expect(within(navigation).getByRole('link', { name: 'Overview' })).toHaveAttribute('href', '/');
  });

  it('marks the link of the current page as active', () => {
    renderRoute('/');

    expect(screen.getByRole('link', { name: 'Overview' })).toHaveAttribute('aria-current', 'page');
  });
});
