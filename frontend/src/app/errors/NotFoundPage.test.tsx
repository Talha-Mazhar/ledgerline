import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderRoute } from '@/test/renderRoute';

describe('NotFoundPage', () => {
  it('is shown for an unknown address, inside the app shell', () => {
    renderRoute('/no-such-page');

    expect(screen.getByRole('heading', { level: 1, name: 'Page not found' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Main navigation' })).toBeInTheDocument();
  });

  it('links back to the overview', () => {
    renderRoute('/no-such-page');

    expect(screen.getByRole('link', { name: 'Go to overview' })).toHaveAttribute('href', '/');
  });
});
