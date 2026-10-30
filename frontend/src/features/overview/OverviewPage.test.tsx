import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderRoute } from '@/test/renderRoute';

describe('OverviewPage', () => {
  it('is the page at the root URL', () => {
    renderRoute('/');

    expect(screen.getByRole('heading', { level: 1, name: 'Overview' })).toBeInTheDocument();
  });

  it('explains what to do when there are no wallets', () => {
    renderRoute('/');

    expect(screen.getByText('No wallets yet')).toBeInTheDocument();
  });
});
