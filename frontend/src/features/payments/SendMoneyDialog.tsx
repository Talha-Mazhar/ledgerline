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
import { demoRecipient } from '@/features/auth/demoAccounts';
import type { Transfer, Wallet } from '@/shared/api/generated';
import { sendMoneyMutation } from '@/shared/api/generated/@tanstack/react-query.gen';
import { problemMessage } from '@/shared/api/problem';
import { amountField } from '@/shared/money/amountField';
import { currencies } from '@/shared/money/currencies';
import { formatMoney } from '@/shared/money/format';

interface SendMoneyDialogProps {
  readonly open: boolean;
  /** The wallet the money is sent from; the recipient gets it in the same currency. */
  readonly from: Wallet;
  readonly onClose: () => void;
  readonly onDone: (message: string) => void;
}

const sendForm = z.object({
  recipientEmail: z
    .string()
    .trim()
    .regex(/^[^@\s]+@[^@\s]+\.[^@\s]+$/, "Enter the recipient's email address."),
  amount: amountField,
});

type SendForm = z.infer<typeof sendForm>;

/**
 * Sends money to another Ledgerline user, found by the email they signed up with. Like every
 * movement, each attempt carries an Idempotency-Key, reused when the same transfer is retried.
 */
export function SendMoneyDialog({ open, from, onClose, onDone }: SendMoneyDialogProps) {
  const queryClient = useQueryClient();
  const lastAttempt = useRef<{ readonly signature: string; readonly key: string } | null>(null);
  const { register, handleSubmit, reset, formState } = useForm<SendForm>({
    resolver: zodResolver(sendForm),
    defaultValues: { recipientEmail: '', amount: '' },
  });

  const send = useMutation({
    ...sendMoneyMutation(),
    onSuccess: async ({ activity, recipient }: Transfer) => {
      await queryClient.invalidateQueries();
      const sent = { ...activity.amount, amount: activity.amount.amount.replace(/^-/, '') };
      onDone(`${formatMoney(sent)} sent to ${recipient.displayName}`);
      close();
    },
  });

  function close() {
    send.reset();
    reset();
    lastAttempt.current = null;
    onClose();
  }

  const submit = (values: SendForm) => {
    const signature = `${values.recipientEmail.toLowerCase()}|${values.amount}`;
    const key =
      lastAttempt.current?.signature === signature ? lastAttempt.current.key : crypto.randomUUID();
    lastAttempt.current = { signature, key };
    send.mutate({
      headers: { 'Idempotency-Key': key },
      body: { fromWalletId: from.id, recipientEmail: values.recipientEmail, amount: values.amount },
    });
  };

  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="xs" aria-labelledby="send-title">
      <form
        noValidate
        onSubmit={(event) => {
          void handleSubmit(submit)(event);
        }}
      >
        <DialogTitle id="send-title">Send money</DialogTitle>
        <DialogContent>
          <Typography sx={{ color: 'text.secondary', mb: 2.5 }}>
            The recipient gets {currencies[from.currency].name.toLowerCase()} in their{' '}
            {from.currency} wallet. Available: {formatMoney(from.balance)}.
          </Typography>
          <TextField
            {...register('recipientEmail')}
            label="Recipient's email"
            type="email"
            fullWidth
            autoComplete="off"
            error={formState.errors.recipientEmail !== undefined}
            helperText={
              formState.errors.recipientEmail?.message ??
              `Try ${demoRecipient}, the second demo account.`
            }
            sx={{ mb: 1.5 }}
          />
          <TextField
            {...register('amount')}
            label={`Amount (${from.currency})`}
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
          {send.isError && (
            <Alert severity="error" sx={{ mt: 1 }}>
              {problemMessage(send.error, "The money wasn't sent. Try again in a moment.")}
            </Alert>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button onClick={close}>Cancel</Button>
          <Button type="submit" variant="contained" loading={send.isPending}>
            Send
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
