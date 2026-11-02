import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderRoute } from '@/test/renderRoute';

describe('Identity & limits', () => {
  it('shows the basic limits of an unverified customer', async () => {
    renderRoute('/verification');

    const limits = await screen.findByRole('region', { name: 'Your limits' });
    expect(within(limits).getByText('€1,000.00')).toBeInTheDocument();
    expect(within(limits).getByText('€2,000.00')).toBeInTheDocument();
    expect(screen.getByText('Not verified')).toBeInTheDocument();
  });

  it('uploads an identity document for review', async () => {
    const user = userEvent.setup();
    renderRoute('/verification');

    const passport = new File(['%PDF-1.7'], 'passport.pdf', { type: 'application/pdf' });
    await user.upload(await screen.findByLabelText('Identity document'), passport);
    await user.click(screen.getByRole('button', { name: 'Send for review' }));

    expect(await screen.findByText('In review')).toBeInTheDocument();
    expect(screen.getByText(/We received .* Our team checks it/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Send for review' })).not.toBeInTheDocument();
  });

  it('refuses a file that is not a PDF or an image', async () => {
    const user = userEvent.setup({ applyAccept: false });
    renderRoute('/verification');

    const notes = new File(['hello'], 'notes.txt', { type: 'text/plain' });
    await user.upload(await screen.findByLabelText('Identity document'), notes);

    expect(screen.getByText('Choose a PDF, JPEG or PNG file.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Send for review' })).toBeDisabled();
  });
});
