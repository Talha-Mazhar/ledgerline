import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import InputAdornment from '@mui/material/InputAdornment';
import MenuItem from '@mui/material/MenuItem';
import Skeleton from '@mui/material/Skeleton';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRef } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { z } from 'zod';
import type { Wallet } from '@/shared/api/generated';
import { exchangeMoneyMutation } from '@/shared/api/generated/@tanstack/react-query.gen';
import { quoteExchange } from '@/shared/api/generated/sdk.gen';
import { problemMessage } from '@/shared/api/problem';
import { useDebouncedValue } from '@/shared/hooks/useDebouncedValue';
import { amountField, isValidAmount } from '@/shared/money/amountField';
import { currencies } from '@/shared/money/currencies';
import { formatMoney } from '@/shared/money/format';

interface ExchangeDialogProps {
  readonly open: boolean;
  /** The wallet money is sold from. */
  readonly from: Wallet;
  /** All of the user's wallets; the others are the choices to buy into. */
  readonly wallets: readonly Wallet[];
  readonly onClose: () => void;
  readonly onDone: (message: string) => void;
}

const exchangeForm = z.object({
  toWalletId: z.string().min(1, 'Choose the wallet to receive the money.'),
  amount: amountField,
});

type ExchangeForm = z.infer<typeof exchangeForm>;

/**
 * Sells money from one wallet and buys another currency into a second wallet.
 *
 * While the user types, the dialog asks the server for a quote (debounced, so not on every
 * keystroke): the server does the rounding, so the preview is exactly what the exchange will give.
 * Like the other movements, every attempt carries an Idempotency-Key that is reused on a retry of
 * the same exchange.
 */
export function ExchangeDialog({ open, from, wallets, onClose, onDone }: ExchangeDialogProps) {
  const queryClient = useQueryClient();
  const lastAttempt = useRef<{ readonly signature: string; readonly key: string } | null>(null);
  const targets = wallets.filter((wallet) => wallet.currency !== from.currency);
  const firstTarget = targets[0];

  const { register, handleSubmit, reset, control, formState } = useForm<ExchangeForm>({
    resolver: zodResolver(exchangeForm),
    values: { toWalletId: firstTarget?.id ?? '', amount: '' },
  });
  const toWalletId = useWatch({ control, name: 'toWalletId' });
  const amount = useDebouncedValue(useWatch({ control, name: 'amount' }).trim());
  const to = targets.find((wallet) => wallet.id === toWalletId);

  const quote = useQuery({
    queryKey: ['exchangeQuote', from.id, toWalletId, amount],
    queryFn: async ({ signal }) =>
      (
        await quoteExchange({
          body: { fromWalletId: from.id, toWalletId, amount },
          signal,
          throwOnError: true,
        })
      ).data,
    enabled: open && to !== undefined && isValidAmount(amount),
    placeholderData: keepPreviousData,
    retry: false,
  });

  const exchange = useMutation({
    ...exchangeMoneyMutation(),
    onSuccess: async (done) => {
      await queryClient.invalidateQueries();
      onDone(
        `Exchanged ${formatMoney(positive(done.sell.amount))} to ${formatMoney(done.buy.amount)}`,
      );
      close();
    },
  });

  function close() {
    exchange.reset();
    reset();
    lastAttempt.current = null;
    onClose();
  }

  const submit = (values: ExchangeForm) => {
    const signature = `${values.toWalletId}|${values.amount}`;
    const key =
      lastAttempt.current?.signature === signature ? lastAttempt.current.key : crypto.randomUUID();
    lastAttempt.current = { signature, key };
    exchange.mutate({
      headers: { 'Idempotency-Key': key },
      body: { fromWalletId: from.id, toWalletId: values.toWalletId, amount: values.amount },
    });
  };

  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="xs" aria-labelledby="exchange-title">
      <form
        noValidate
        onSubmit={(event) => {
          void handleSubmit(submit)(event);
        }}
      >
        <DialogTitle id="exchange-title">Exchange</DialogTitle>
        <DialogContent>
          {firstTarget === undefined ? (
            <Typography sx={{ color: 'text.secondary' }}>
              Open a wallet in another currency first. Exchange moves money between two of your
              wallets.
            </Typography>
          ) : (
            <>
              <Typography sx={{ color: 'text.secondary', mb: 2.5 }}>
                Sell {currencies[from.currency].name.toLowerCase()} from this wallet. Available:{' '}
                {formatMoney(from.balance)}.
              </Typography>
              <TextField
                {...register('amount')}
                label={`You sell (${from.currency})`}
                fullWidth
                autoComplete="off"
                error={formState.errors.amount !== undefined}
                helperText={formState.errors.amount?.message ?? ' '}
                slotProps={{
                  htmlInput: { inputMode: 'decimal' },
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        {currencies[from.currency].symbol}
                      </InputAdornment>
                    ),
                    sx: { fontVariantNumeric: 'tabular-nums', fontSize: '1.25rem' },
                  },
                }}
              />
              <Controller
                control={control}
                name="toWalletId"
                render={({ field, fieldState }) => (
                  <TextField
                    {...field}
                    select
                    label="Into wallet"
                    fullWidth
                    error={fieldState.error !== undefined}
                    helperText={fieldState.error?.message ?? ' '}
                  >
                    {targets.map((wallet) => (
                      <MenuItem key={wallet.id} value={wallet.id}>
                        {currencies[wallet.currency].name} ({wallet.currency})
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />

              <Box
                aria-live="polite"
                sx={{
                  mt: 0.5,
                  p: 2,
                  borderRadius: 2,
                  bgcolor: 'background.default',
                  minHeight: 76,
                }}
              >
                {quote.isError ? (
                  <Typography sx={{ color: 'error.main' }}>
                    {problemMessage(quote.error, 'No quote right now. Try again in a moment.')}
                  </Typography>
                ) : quote.data !== undefined && to !== undefined ? (
                  <>
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                      You get
                    </Typography>
                    <Typography variant="figure" sx={{ fontSize: '1.75rem' }}>
                      {formatMoney(quote.data.buy)}
                    </Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
                      1 {from.currency} = {quote.data.rate} {to.currency}, Ledgerline rate
                    </Typography>
                  </>
                ) : quote.isFetching ? (
                  <Skeleton variant="text" width="60%" sx={{ fontSize: '1.75rem' }} />
                ) : (
                  <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                    Enter an amount to see what you get.
                  </Typography>
                )}
              </Box>

              {exchange.isError && (
                <Alert severity="error" sx={{ mt: 2 }}>
                  {problemMessage(exchange.error, 'The exchange failed. Try again in a moment.')}
                </Alert>
              )}
            </>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button onClick={close}>Cancel</Button>
          {firstTarget !== undefined && (
            <Button type="submit" variant="contained" loading={exchange.isPending}>
              Exchange
            </Button>
          )}
        </DialogActions>
      </form>
    </Dialog>
  );
}

/** An amount without its minus sign: "-100.00" becomes "100.00". */
function positive(money: Wallet['balance']): Wallet['balance'] {
  return { ...money, amount: money.amount.replace(/^-/, '') };
}
