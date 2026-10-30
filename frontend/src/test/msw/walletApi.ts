import { http, HttpResponse } from 'msw';
import type { CurrencyCode, Problem, Wallet, WalletList } from '@/shared/api/generated';
import { zOpenWalletRequest } from '@/shared/api/generated/zod.gen';

/**
 * A fake wallets API for tests, backed by an in-memory list. It follows api/openapi.yaml, and the
 * generated client validates every response against the contract, so a fixture that drifts from
 * the contract fails the test that uses it.
 */
export const API = 'http://localhost/api/v1';

let wallets: Wallet[] = [];
let sequence = 0;

export function resetWallets(): void {
  wallets = [];
  sequence = 0;
}

/** Puts a wallet in the store directly, as if the user had opened it earlier. */
export function givenWallet(currency: CurrencyCode, amount = '0.00'): Wallet {
  sequence += 1;
  const wallet: Wallet = {
    id: `00000000-0000-4000-8000-${String(sequence).padStart(12, '0')}`,
    currency,
    balance: { amount, currency },
    status: 'ACTIVE',
    openedAt: '2026-10-30T12:00:00Z',
  };
  wallets.push(wallet);
  return wallet;
}

function problem(status: number, title: string, detail: string) {
  const body: Problem = { type: 'about:blank', title, status, detail };
  return HttpResponse.json(body, {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  });
}

export const walletHandlers = [
  http.get(`${API}/wallets`, () => HttpResponse.json<WalletList>({ items: wallets })),

  http.post(`${API}/wallets`, async ({ request }) => {
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
];
