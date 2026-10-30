import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Radio from '@mui/material/Radio';
import RadioGroup from '@mui/material/RadioGroup';
import Typography from '@mui/material/Typography';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Controller, useForm } from 'react-hook-form';
import {
  listWalletsQueryKey,
  openWalletMutation,
} from '@/shared/api/generated/@tanstack/react-query.gen';
import type { CurrencyCode, OpenWalletRequest, Wallet } from '@/shared/api/generated';
import { zOpenWalletRequest } from '@/shared/api/generated/zod.gen';
import { problemMessage } from '@/shared/api/problem';
import { currencies, currencyCodes } from '@/shared/money/currencies';
import { CurrencyMark } from '@/shared/ui/CurrencyMark';

interface OpenWalletDialogProps {
  readonly open: boolean;
  /** Currencies the user already holds; they are shown but can't be chosen. */
  readonly heldCurrencies: readonly CurrencyCode[];
  /** Preselected currency, e.g. when opened from that currency's column. */
  readonly initialCurrency: CurrencyCode;
  readonly onClose: () => void;
  readonly onOpened: (wallet: Wallet) => void;
}

/**
 * Picks a currency and opens a wallet in it. The form is validated with the contract's own zod
 * schema (generated from openapi.yaml), so the browser and the API agree on what a valid request is.
 */
export function OpenWalletDialog({
  open,
  heldCurrencies,
  initialCurrency,
  onClose,
  onOpened,
}: OpenWalletDialogProps) {
  const queryClient = useQueryClient();
  const { control, handleSubmit, reset } = useForm<OpenWalletRequest>({
    resolver: zodResolver(zOpenWalletRequest),
    values: { currency: initialCurrency },
  });

  const openWallet = useMutation({
    ...openWalletMutation(),
    onSuccess: async (wallet) => {
      await queryClient.invalidateQueries({ queryKey: listWalletsQueryKey() });
      onOpened(wallet);
    },
  });

  const close = () => {
    openWallet.reset();
    reset();
    onClose();
  };

  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="xs" aria-labelledby="open-wallet-title">
      <form
        noValidate
        onSubmit={(event) => {
          void handleSubmit((request) => openWallet.mutate({ body: request }))(event);
        }}
      >
        <DialogTitle id="open-wallet-title">Open a wallet</DialogTitle>
        <DialogContent>
          <Typography id="open-wallet-help" sx={{ color: 'text.secondary', mb: 2.5 }}>
            Choose a currency. The wallet opens with a zero balance.
          </Typography>

          <Controller
            control={control}
            name="currency"
            render={({ field }) => (
              <RadioGroup
                {...field}
                aria-labelledby="open-wallet-title"
                aria-describedby="open-wallet-help"
                sx={{ gap: 1 }}
              >
                {currencyCodes.map((code) => (
                  <CurrencyOption
                    key={code}
                    currency={code}
                    held={heldCurrencies.includes(code)}
                    selected={field.value === code}
                  />
                ))}
              </RadioGroup>
            )}
          />

          {openWallet.isError && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {problemMessage(
                openWallet.error,
                'The wallet could not be opened. Try again in a moment.',
              )}
            </Alert>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button onClick={close}>Cancel</Button>
          <Button type="submit" variant="contained" loading={openWallet.isPending}>
            Open wallet
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

interface CurrencyOptionProps {
  readonly currency: CurrencyCode;
  readonly held: boolean;
  readonly selected: boolean;
}

function CurrencyOption({ currency, held, selected }: CurrencyOptionProps) {
  const { name } = currencies[currency];
  return (
    <Box
      component="label"
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 1.5,
        px: 1.5,
        py: 1,
        border: 1,
        borderRadius: 2,
        borderColor: selected ? 'primary.main' : 'ledger.rule',
        bgcolor: selected ? 'action.hover' : 'transparent',
        cursor: held ? 'default' : 'pointer',
        opacity: held ? 0.55 : 1,
      }}
    >
      <CurrencyMark currency={currency} size={28} />
      <Box sx={{ flexGrow: 1 }}>
        <Typography sx={{ fontWeight: 600, lineHeight: 1.3 }}>{name}</Typography>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {held ? 'Already open' : currency}
        </Typography>
      </Box>
      <Radio value={currency} disabled={held} slotProps={{ input: { 'aria-label': name } }} />
    </Box>
  );
}
