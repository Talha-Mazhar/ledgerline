import AddRounded from '@mui/icons-material/AddRounded';
import Button from '@mui/material/Button';
import Snackbar from '@mui/material/Snackbar';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { ActivityLedger } from '@/features/activity/ActivityLedger';
import { OpenWalletDialog } from '@/features/wallets/OpenWalletDialog';
import { WalletSummary } from '@/features/wallets/WalletSummary';
import type { CurrencyCode } from '@/shared/api/generated';
import {
  listActivityOptions,
  listWalletsOptions,
} from '@/shared/api/generated/@tanstack/react-query.gen';
import { currencies, currencyCodes } from '@/shared/money/currencies';
import { ErrorState } from '@/shared/ui/ErrorState';
import { PageHeader } from '@/shared/ui/PageHeader';

/** Landing page: the balance in every currency, and the activity across all wallets. */
export function OverviewPage() {
  const wallets = useQuery(listWalletsOptions());
  const activity = useQuery(listActivityOptions({ query: { limit: 10 } }));
  const [dialogCurrency, setDialogCurrency] = useState<CurrencyCode | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const held = wallets.data?.items.map((wallet) => wallet.currency) ?? [];
  const firstUnheld = currencyCodes.find((code) => !held.includes(code));

  return (
    <>
      <PageHeader
        title="Overview"
        description="Your balance in each currency."
        actions={
          firstUnheld !== undefined && (
            <Button
              variant="contained"
              size="large"
              startIcon={<AddRounded />}
              disabled={!wallets.isSuccess}
              onClick={() => setDialogCurrency(firstUnheld)}
            >
              Open wallet
            </Button>
          )
        }
      />

      {wallets.isError ? (
        <ErrorState
          title="Your wallets couldn't be loaded"
          description="The server didn't answer. Check your connection and try again."
          onRetry={() => void wallets.refetch()}
        />
      ) : (
        <WalletSummary wallets={wallets.data?.items} onOpenWallet={setDialogCurrency} />
      )}

      <ActivityLedger
        items={activity.data?.items}
        showWallet
        error={activity.isError ? { onRetry: () => void activity.refetch() } : undefined}
        emptyMessage="No entries yet. Money you add, send or exchange is recorded here."
      />

      <OpenWalletDialog
        open={dialogCurrency !== null}
        initialCurrency={dialogCurrency ?? firstUnheld ?? 'EUR'}
        heldCurrencies={held}
        onClose={() => setDialogCurrency(null)}
        onOpened={(wallet) => {
          setDialogCurrency(null);
          setNotice(`${currencies[wallet.currency].name} wallet opened`);
        }}
      />

      <Snackbar
        open={notice !== null}
        message={notice}
        autoHideDuration={4000}
        onClose={() => setNotice(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      />
    </>
  );
}
