import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { givenAssistantOffline } from '@/test/msw/paymentsApi';
import { renderRoute } from '@/test/renderRoute';

describe('Assistant', () => {
  it('answers a question about the money', async () => {
    const user = userEvent.setup();
    renderRoute('/assistant');

    await user.type(await screen.findByLabelText('Your question'), 'What are my balances?');
    await user.click(screen.getByRole('button', { name: 'Ask' }));

    expect(await screen.findByText('You have €1,250.00 in your EUR wallet.')).toBeInTheDocument();
    expect(screen.getByLabelText('Your question')).toHaveValue('');
  });

  it('asks a suggested question', async () => {
    const user = userEvent.setup();
    renderRoute('/assistant');

    await user.click(
      await screen.findByRole('button', { name: 'Where did my money go this month?' }),
    );

    expect(
      await screen.findByText('This month you sent €30.00 to Jana Nováková and added €100.00.'),
    ).toBeInTheDocument();
  });

  it('says when the model is not running', async () => {
    const user = userEvent.setup();
    givenAssistantOffline();
    renderRoute('/assistant');

    await user.type(await screen.findByLabelText('Your question'), 'Where did my money go?');
    await user.click(screen.getByRole('button', { name: 'Ask' }));

    expect(await screen.findByText(/The assistant is offline right now/)).toBeInTheDocument();
  });
});
