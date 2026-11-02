import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { JANA, SUPPORT_USER, givenSignedInAs } from '@/test/msw/authApi';
import { givenDocumentToReview } from '@/test/msw/paymentsApi';
import { first } from '@/test/first';
import { renderRoute } from '@/test/renderRoute';

describe('Verifications', () => {
  it('opens the document and approves the customer', async () => {
    const user = userEvent.setup();
    givenDocumentToReview(JANA);
    givenSignedInAs(SUPPORT_USER);
    renderRoute('/back-office/verifications');

    const table = await screen.findByRole('table', { name: 'Documents to review' });
    expect(await within(table).findByRole('link', { name: /passport.pdf/ })).toHaveAttribute(
      'href',
      `http://localhost/api/v1/back-office/verifications/${JANA.id}/document`,
    );
    await user.click(within(table).getByRole('button', { name: 'Approve Jana Nováková' }));

    expect(await screen.findByText('Jana Nováková is verified.')).toBeInTheDocument();
    expect(await screen.findByText('No documents to review')).toBeInTheDocument();
  });

  it('rejects a document with a reason, and the audit log records it', async () => {
    const user = userEvent.setup();
    givenDocumentToReview(JANA);
    givenSignedInAs(SUPPORT_USER);
    renderRoute('/back-office/verifications');

    await user.click(await screen.findByRole('button', { name: 'Reject Jana Nováková' }));
    const dialog = screen.getByRole('dialog', { name: 'Reject document' });
    await user.type(within(dialog).getByLabelText('Reason'), 'The photo is blurred');
    await user.click(within(dialog).getByRole('button', { name: 'Reject' }));
    expect(
      await screen.findByText('Jana Nováková is asked for another document.'),
    ).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    await user.click(first(screen.getAllByRole('link', { name: 'Audit log' })));
    const log = await screen.findByRole('table', { name: 'Audit log' });
    expect(await within(log).findByText('Verification rejected')).toBeInTheDocument();
    expect(within(log).getByText('The photo is blurred')).toBeInTheDocument();
    expect(within(log).getByText('support@ledgerline.dev')).toBeInTheDocument();
  });
});
