import { http, HttpResponse } from 'msw';
import type {
  ActivityItem,
  ActivityList,
  CurrencyCode,
  Exchange,
  ExchangeQuote,
  Transfer,
  Wallet,
  WalletList,
} from '@/shared/api/generated';
import {
  zAmountRequest,
  zExchangeRequest,
  zOpenWalletRequest,
  zTransferRequest,
} from '@/shared/api/generated/zod.gen';
import { currentUser, findUser, JANA } from '@/test/msw/authApi';
import { API, post, problem } from '@/test/msw/http';
import { recordPayment } from '@/test/msw/paymentsApi';

/**
 * A fake wallets API for tests, backed by in-memory lists. It follows api/openapi.yaml, and the
 * generated client validates every response against the contract, so a fixture that drifts from
 * the contract fails the test that uses it.
 *
 * Amounts are kept in cents (whole numbers) so the fake's arithmetic is exact.
 *
 * The wallets belong to the signed-in user. Jana, the other demo user, has EUR and CZK wallets
 * that can receive transfers; the fake doesn't track their balances.
 *
 * Like the backend's risk rules for a basic customer, a transfer above 1,000 is held for staff
 * approval: the money is set aside (a HELD line) and the transfer's status is HELD.
 */

const HOLD_ABOVE_CENTS = 100_000;

let wallets: Wallet[] = [];
let activity: ActivityItem[] = [];
let idempotency = new Map<string, ActivityItem>();
let sequence = 0;

export function resetWallets(): void {
  wallets = [];
  activity = [];
  idempotency = new Map();
  sequence = 0;
}

function nextId(): string {
  sequence += 1;
  return `00000000-0000-4000-8000-${String(sequence).padStart(12, '0')}`;
}

const toCents = (amount: string): number => Math.round(Number(amount) * 100);
const fromCents = (cents: number): string => (cents / 100).toFixed(2);

/** Puts a wallet in the store directly, as if the user had opened it earlier. */
export function givenWallet(currency: CurrencyCode, amount = '0.00'): Wallet {
  const wallet: Wallet = {
    id: nextId(),
    currency,
    balance: { amount, currency },
    status: 'ACTIVE',
    openedAt: '2026-10-30T12:00:00Z',
  };
  wallets.push(wallet);
  return wallet;
}

/** The currencies Jana can receive, as seeded by the backend's V5 migration. */
const janasCurrencies: readonly CurrencyCode[] = ['EUR', 'CZK'];

function notEnough(wallet: Wallet) {
  return problem(
    422,
    'Not enough money',
    `This wallet has ${wallet.balance.amount} ${wallet.currency} available, which isn't enough.`,
  );
}

/** Records a movement on a wallet, like the ledger would, and returns the wallet's new line. */
function move(wallet: Wallet, cents: number, kind: ActivityItem['kind'], description: string) {
  const after = toCents(wallet.balance.amount) + cents;
  wallet.balance = { amount: fromCents(after), currency: wallet.currency };
  const line: ActivityItem = {
    id: nextId(),
    entryId: nextId(),
    walletId: wallet.id,
    kind,
    description,
    amount: { amount: fromCents(cents), currency: wallet.currency },
    balanceAfter: { ...wallet.balance },
    occurredAt: '2026-10-31T09:30:00Z',
  };
  activity.unshift(line);
  return line;
}

/** The demo rates, per euro, as in the backend's migration. */
const perEur: Record<CurrencyCode, number> = { EUR: 1, USD: 1.085, CZK: 25.15, GBP: 0.855 };

/** Ledgerline's rate with its 0.5% spread, six decimals, like the backend. */
function rate(from: CurrencyCode, to: CurrencyCode): string {
  return ((perEur[to] / perEur[from]) * 0.995).toFixed(6);
}

/** Prices an exchange request, or answers with the problem the backend would give. */
function priced(body: unknown) {
  const request = zExchangeRequest.parse(body);
  const from = wallets.find((wallet) => wallet.id === request.fromWalletId);
  const to = wallets.find((wallet) => wallet.id === request.toWalletId);
  if (from === undefined || to === undefined) {
    return { problem: problem(404, 'Wallet not found', 'You have no wallet with this id.') };
  }
  const sellCents = toCents(request.amount);
  const quoted = rate(from.currency, to.currency);
  const buyCents = Math.floor(sellCents * Number(quoted) + 1e-9);
  return { from, to, sellCents, buyCents, rate: quoted };
}

export const walletHandlers = [
  http.get(`${API}/wallets`, () => HttpResponse.json<WalletList>({ items: wallets })),

  post('/wallets', async ({ request }) => {
    const { currency } = zOpenWalletRequest.parse(await request.json());
    if (wallets.some((wallet) => wallet.currency === currency)) {
      return problem(409, 'Wallet already open', `You already have a ${currency} wallet.`);
    }
    const wallet = givenWallet(currency);
    return HttpResponse.json<Wallet>(wallet, {
      status: 201,
      headers: { Location: `${API}/wallets/${wallet.id}` },
    });
  }),

  http.get(`${API}/wallets/:walletId`, ({ params }) => {
    const wallet = wallets.find((candidate) => candidate.id === params['walletId']);
    return wallet === undefined
      ? problem(
          404,
          'Wallet not found',
          `You have no wallet with id ${String(params['walletId'])}.`,
        )
      : HttpResponse.json<Wallet>(wallet);
  }),

  post('/wallets/:walletId/:movement', async ({ params, request }) => {
    const wallet = wallets.find((candidate) => candidate.id === params['walletId']);
    if (wallet === undefined) {
      return problem(404, 'Wallet not found', 'You have no wallet with this id.');
    }
    const key = request.headers.get('Idempotency-Key') ?? '';
    const replay = idempotency.get(key);
    if (replay !== undefined) {
      return HttpResponse.json<ActivityItem>(replay, { status: 201 });
    }
    const cents = toCents(zAmountRequest.parse(await request.json()).amount);
    if (params['movement'] === 'withdrawals') {
      if (cents > toCents(wallet.balance.amount)) {
        return notEnough(wallet);
      }
      const line = move(wallet, -cents, 'WITHDRAWAL', 'Withdrawal to bank');
      idempotency.set(key, line);
      return HttpResponse.json<ActivityItem>(line, { status: 201 });
    }
    const line = move(wallet, cents, 'DEPOSIT', 'Added money');
    idempotency.set(key, line);
    return HttpResponse.json<ActivityItem>(line, { status: 201 });
  }),

  http.get(`${API}/wallets/:walletId/activity`, ({ params }) =>
    HttpResponse.json<ActivityList>({
      items: activity.filter((line) => line.walletId === params['walletId']),
    }),
  ),

  post('/exchanges/quote', async ({ request }) => {
    const price = priced(await request.json());
    if ('problem' in price) {
      return price.problem;
    }
    return HttpResponse.json<ExchangeQuote>({
      sell: { amount: fromCents(price.sellCents), currency: price.from.currency },
      buy: { amount: fromCents(price.buyCents), currency: price.to.currency },
      rate: price.rate,
    });
  }),

  post('/exchanges', async ({ request }) => {
    const price = priced(await request.json());
    if ('problem' in price) {
      return price.problem;
    }
    if (price.sellCents > toCents(price.from.balance.amount)) {
      return notEnough(price.from);
    }
    const sell = move(
      price.from,
      -price.sellCents,
      'EXCHANGE_OUT',
      `Exchanged to ${price.to.currency}`,
    );
    const buy = move(
      price.to,
      price.buyCents,
      'EXCHANGE_IN',
      `Exchanged from ${price.from.currency}`,
    );
    return HttpResponse.json<Exchange>(
      { entryId: sell.entryId, sell, buy: { ...buy, entryId: sell.entryId }, rate: price.rate },
      { status: 201 },
    );
  }),

  post('/transfers', async ({ request }) => {
    const body = zTransferRequest.parse(await request.json());
    const wallet = wallets.find((candidate) => candidate.id === body.fromWalletId);
    if (wallet === undefined) {
      return problem(404, 'Wallet not found', 'You have no wallet with this id.');
    }
    const recipient = findUser(body.recipientEmail);
    if (recipient === undefined) {
      return problem(404, 'Recipient not found', 'Nobody on Ledgerline uses this email.');
    }
    if (recipient.id === currentUser()?.id) {
      return problem(
        400,
        "Can't send to yourself",
        'To move money between your own wallets, use Exchange.',
      );
    }
    if (recipient.id !== JANA.id || !janasCurrencies.includes(wallet.currency)) {
      return problem(
        422,
        `Recipient can't receive ${wallet.currency}`,
        `${recipient.displayName} has no ${wallet.currency} wallet yet.`,
      );
    }
    const cents = toCents(body.amount);
    if (cents > toCents(wallet.balance.amount)) {
      return notEnough(wallet);
    }
    const held = cents > HOLD_ABOVE_CENTS;
    const line = held
      ? move(wallet, -cents, 'HELD', `Set aside: transfer to ${recipient.displayName}`)
      : move(wallet, -cents, 'TRANSFER_OUT', `Sent to ${recipient.displayName}`);
    const sender = currentUser();
    const payment =
      sender === null
        ? undefined
        : recordPayment({
            customer: sender,
            counterparty: recipient,
            kind: 'TRANSFER',
            amount: { amount: fromCents(cents), currency: wallet.currency },
            description: `To ${recipient.displayName}`,
            status: held ? 'HELD' : 'COMPLETED',
            holdReason: held ? 'Above the single-payment limit of €1,000.00' : undefined,
          });
    return HttpResponse.json<Transfer>(
      {
        activity: line,
        recipient: { email: recipient.email, displayName: recipient.displayName },
        paymentId: payment?.id ?? nextId(),
        status: held ? 'HELD' : 'COMPLETED',
      },
      { status: 201 },
    );
  }),

  http.get(`${API}/activity`, ({ request }) => {
    const limit = Number(new URL(request.url).searchParams.get('limit') ?? '50');
    return HttpResponse.json<ActivityList>({ items: activity.slice(0, limit) });
  }),
];
