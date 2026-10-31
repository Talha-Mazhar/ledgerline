import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { givenSignedOut } from '@/test/msw/authApi';
import { givenWallet } from '@/test/msw/walletApi';
import { renderRoute } from '@/test/renderRoute';

describe('Signing in', () => {
  it('sends a visitor without a session to sign in, then back to the page they asked for', async () => {
    const user = userEvent.setup();
    givenSignedOut();
    const wallet = givenWallet('EUR', '12.00');
    renderRoute(`/wallets/${wallet.id}`);

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    await user.type(screen.getByLabelText('Email'), 'Demo@Ledgerline.dev');
    await user.type(screen.getByLabelText('Password'), 'ledgerline-demo');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('heading', { name: /Euro wallet/ })).toBeInTheDocument();
    expect(screen.getByText('Demo user')).toBeInTheDocument();
  });

  it('signs into the demo account with one click', async () => {
    const user = userEvent.setup();
    givenSignedOut();
    renderRoute('/sign-in');

    await user.click(await screen.findByRole('button', { name: 'Try the demo account' }));

    expect(await screen.findByRole('heading', { name: 'Overview' })).toBeInTheDocument();
  });

  it('says the email or password is wrong, without saying which', async () => {
    const user = userEvent.setup();
    givenSignedOut();
    renderRoute('/sign-in');

    await user.type(await screen.findByLabelText('Email'), 'demo@ledgerline.dev');
    await user.type(screen.getByLabelText('Password'), 'not-the-password');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('Wrong email or password.')).toBeInTheDocument();
  });

  it('sends a signed-in user straight on to the overview', async () => {
    renderRoute('/sign-in');

    expect(await screen.findByRole('heading', { name: 'Overview' })).toBeInTheDocument();
  });
});

describe('Signing up', () => {
  it('creates an account and signs in', async () => {
    const user = userEvent.setup();
    givenSignedOut();
    renderRoute('/sign-up');

    await user.type(await screen.findByLabelText('Your name'), 'Pavel Dvořák');
    await user.type(screen.getByLabelText('Email'), 'pavel@example.com');
    await user.type(screen.getByLabelText('Password'), 'a-long-password');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByRole('heading', { name: 'Overview' })).toBeInTheDocument();
    expect(screen.getByText('Pavel Dvořák')).toBeInTheDocument();
  });

  it('checks the password length before sending anything', async () => {
    const user = userEvent.setup();
    givenSignedOut();
    renderRoute('/sign-up');

    await user.type(await screen.findByLabelText('Your name'), 'Short');
    await user.type(screen.getByLabelText('Email'), 'short@example.com');
    await user.type(screen.getByLabelText('Password'), 'too-short');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByText('Use at least 10 characters.')).toBeInTheDocument();
  });

  it('explains when the email already has an account', async () => {
    const user = userEvent.setup();
    givenSignedOut();
    renderRoute('/sign-up');

    await user.type(await screen.findByLabelText('Your name'), 'Jana');
    await user.type(screen.getByLabelText('Email'), 'jana@ledgerline.dev');
    await user.type(screen.getByLabelText('Password'), 'a-long-password');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(
      await screen.findByText('An account with this email already exists. Sign in instead.'),
    ).toBeInTheDocument();
  });
});
