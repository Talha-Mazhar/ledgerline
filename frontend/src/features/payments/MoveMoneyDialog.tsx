import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import InputAdornment from '@mui/material/InputAdornment';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRef } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import type { ActivityItem, Wallet } from '@/shared/api/generated';
import {
  depositMoneyMutation,
  withdrawMoneyMutation,
} from '@/shared/api/generated/@tanstack/react-query.gen';
import { problemMessage } from '@/shared/api/problem';
import { amountField } from '@/shared/money/amountField';
import { currencies } from '@/shared/money/currencies';
import { formatMoney } from '@/shared/money/format';

export type MoveMoneyMode = 'deposit' | 'withdraw';

interface MoveMoneyDialogProps {
  readonly wallet: Wallet;
  /** Which movement the dialog makes, or null when it's closed. */
  readonly mode: MoveMoneyMode | null;
  readonly onClose: () => void;
  /** Called with the confirmation to show, e.g. "€250.00 added", once the ledger has the entry. */
  readonly onDone: (message: string) => void;
}

const amountForm = z.object({ amount: amountField });

type AmountForm = z.infer<typeof amountForm>;

const copy = {
  deposit: {
    title: 'Add money',
    help: 'This simulates a card top-up. No real card is charged.',
    button: 'Add money',
    done: 'added',
  },
  withdraw: {
    title: 'Withdraw',
    help: 'This simulates a payout to your bank account.',
    button: 'Withdraw',
    done: 'withdrawn',
  },
} as const;

/**
 * Adds money to a wallet or withdraws it.
 *
 * Every attempt carries an Idempotency-Key. If the request fails on the way (a dropped
 * connection) and the user presses the button again with the same amount, the same key is sent,
 * and the backend returns the first result instead of moving the money twice. A different amount
 * is a new request and gets a new key.
 */
export function MoveMoneyDialog({ wallet, mode, onClose, onDone }: MoveMoneyDialogProps) {
  const queryClient = useQueryClient();
  const lastAttempt = useRef<{ readonly amount: string; readonly key: string } | null>(null);
  const { register, handleSubmit, reset, formState } = useForm<AmountForm>({
    resolver: zodResolver(amountForm),
    defaultValues: { amount: '' },
  });

  const finish = async (line: ActivityItem) => {
    // Any movement changes balances and activity everywhere, so every cached query refetches.
    await queryClient.invalidateQueries();
    onDone(
      `${formatMoney({ ...line.amount, amount: line.amount.amount.replace('-', '') })} ${copy[mode ?? 'deposit'].done}`,
    );
    close();
  };
  const deposit = useMutation({ ...depositMoneyMutation(), onSuccess: finish });
  const withdraw = useMutation({ ...withdrawMoneyMutation(), onSuccess: finish });
  const active = mode === 'withdraw' ? withdraw : deposit;

  function close() {
    deposit.reset();
    withdraw.reset();
    reset();
    lastAttempt.current = null;
    onClose();
  }

  const submit = ({ amount }: AmountForm) => {
    const key =
      lastAttempt.current?.amount === amount ? lastAttempt.current.key : crypto.randomUUID();
    lastAttempt.current = { amount, key };
    active.mutate({
      path: { walletId: wallet.id },
      headers: { 'Idempotency-Key': key },
      body: { amount },
    });
  };

  const { symbol } = currencies[wallet.currency];
  const text = copy[mode ?? 'deposit'];

  return (
    <Dialog
      open={mode !== null}
      onClose={close}
      fullWidth
      maxWidth="xs"
      aria-labelledby="move-money-title"
    >
      <form
        noValidate
        onSubmit={(event) => {
          void handleSubmit(submit)(event);
        }}
      >
        <DialogTitle id="move-money-title">{text.title}</DialogTitle>
        <DialogContent>
          <Typography sx={{ color: 'text.secondary', mb: 2.5 }}>
            {text.help} {mode === 'withdraw' && `Available: ${formatMoney(wallet.balance)}.`}
          </Typography>
          <TextField
            {...register('amount')}
            label={`Amount (${wallet.currency})`}
            fullWidth
            autoComplete="off"
            error={formState.errors.amount !== undefined}
            helperText={formState.errors.amount?.message ?? ' '}
            slotProps={{
              htmlInput: { inputMode: 'decimal' },
              input: {
                startAdornment: <InputAdornment position="start">{symbol}</InputAdornment>,
                sx: { fontVariantNumeric: 'tabular-nums', fontSize: '1.25rem' },
              },
            }}
          />
          {active.isError && (
            <Alert severity="error" sx={{ mt: 1 }}>
              {problemMessage(active.error, 'The money could not be moved. Try again in a moment.')}
            </Alert>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button onClick={close}>Cancel</Button>
          <Button type="submit" variant="contained" loading={active.isPending}>
            {text.button}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
