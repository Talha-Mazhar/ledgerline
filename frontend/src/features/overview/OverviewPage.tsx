import AccountBalanceWalletOutlined from '@mui/icons-material/AccountBalanceWalletOutlined';
import { EmptyState } from '@/shared/ui/EmptyState';
import { PageHeader } from '@/shared/ui/PageHeader';

/** Landing page after sign-in. Wallet balances and recent activity arrive in later milestones. */
export function OverviewPage() {
  return (
    <>
      <PageHeader
        title="Overview"
        description="Balances and recent activity across your wallets."
      />
      <EmptyState
        icon={AccountBalanceWalletOutlined}
        title="No wallets yet"
        description="Open a wallet in EUR, USD, CZK or GBP to start sending and receiving money."
      />
    </>
  );
}
