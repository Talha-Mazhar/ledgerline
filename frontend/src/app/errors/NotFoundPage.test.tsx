import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderRoute } from '@/test/renderRoute';

describe('NotFoundPage', () => {
  it('is shown for an unknown address, inside the app shell', async () => {
    renderRoute('/no-such-page');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Page not found' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Main navigation' })).toBeInTheDocument();
  });

  it('links back to the overview', async () => {
    renderRoute('/no-such-page');

    expect(await screen.findByRole('link', { name: 'Go to overview' })).toHaveAttribute(
      'href',
      '/',
    );
  });
});
