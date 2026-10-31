import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Paper from '@mui/material/Paper';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import { Link as RouterLink } from 'react-router';
import type { CurrencyCode, Wallet } from '@/shared/api/generated';
import { walletPath } from '@/shared/config/paths';
import { currencies, currencyCodes } from '@/shared/money/currencies';
import { formatDate } from '@/shared/money/format';
import { CurrencyMark } from '@/shared/ui/CurrencyMark';
import { Figure } from '@/shared/ui/Figure';

interface WalletSummaryProps {
  /** The user's wallets, or undefined while they load. */
  readonly wallets: readonly Wallet[] | undefined;
  readonly onOpenWallet: (currency: CurrencyCode) => void;
}

/**
 * The four currencies side by side, as one summary line of a ledger. A currency the user holds
 * shows its balance; one they don't shows how to open it. Showing all four always, rather than
 * only the wallets that exist, keeps every currency in the same place on every visit.
 */
export function WalletSummary({ wallets, onOpenWallet }: WalletSummaryProps) {
  return (
    <Paper
      variant="outlined"
      component="section"
      aria-label="Balances"
      sx={{
        display: 'grid',
        gridTemplateColumns: {
          xs: '1fr',
          sm: 'repeat(2, minmax(0, 1fr))',
          lg: 'repeat(4, minmax(0, 1fr))',
        },
        mb: { xs: 4, md: 6 },
        // Hairlines between columns and, when they wrap, between rows.
        '& > *': { borderColor: 'ledger.rule', borderStyle: 'solid', borderWidth: 0 },
        '& > *:not(:first-of-type)': { borderTopWidth: { xs: 1, lg: 0 } },
        '& > *:nth-of-type(2)': { borderTopWidth: { sm: 0 } },
        '& > *:nth-of-type(even)': { borderLeftWidth: { sm: 1 } },
        '& > *:nth-of-type(n+2)': { borderLeftWidth: { lg: 1 } },
      }}
    >
      {currencyCodes.map((code) => (
        <CurrencyColumn
          key={code}
          currency={code}
          loading={wallets === undefined}
          wallet={wallets?.find((wallet) => wallet.currency === code)}
          onOpenWallet={onOpenWallet}
        />
      ))}
    </Paper>
  );
}

interface CurrencyColumnProps {
  readonly currency: CurrencyCode;
  readonly loading: boolean;
  readonly wallet: Wallet | undefined;
  readonly onOpenWallet: (currency: CurrencyCode) => void;
}

function CurrencyColumn({ currency, loading, wallet, onOpenWallet }: CurrencyColumnProps) {
  const { name } = currencies[currency];
  return (
    <Box
      component="article"
      aria-label={`${name} (${currency})`}
      sx={{
        p: { xs: 2.5, md: 3 },
        minHeight: { sm: 184 },
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, mb: { xs: 2, sm: 3 } }}>
        <CurrencyMark currency={currency} size={28} />
        <Typography sx={{ fontWeight: 600 }}>{name}</Typography>
        <Typography sx={{ color: 'text.secondary', ml: 'auto' }}>{currency}</Typography>
      </Box>

      {loading ? (
        <>
          <Skeleton variant="text" width="60%" sx={{ fontSize: '2rem' }} />
          <Skeleton variant="text" width="40%" />
        </>
      ) : wallet === undefined ? (
        <Box sx={{ mt: 'auto' }}>
          <Typography sx={{ color: 'text.secondary', mb: 1.5 }}>
            No {currency} wallet yet.
          </Typography>
          <Button variant="outlined" size="small" onClick={() => onOpenWallet(currency)}>
            Open {currency} wallet
          </Button>
        </Box>
      ) : (
        <Box sx={{ mt: 'auto' }}>
          <Figure money={wallet.balance} />
          <Box
            sx={{
              display: 'flex',
              alignItems: 'baseline',
              justifyContent: 'space-between',
              mt: 1.5,
              gap: 2,
            }}
          >
            <Typography variant="body2" sx={{ color: 'text.secondary', whiteSpace: 'nowrap' }}>
              Opened {formatDate(wallet.openedAt)}
            </Typography>
            <Link
              component={RouterLink}
              to={walletPath(wallet.id)}
              aria-label={`View ${name} wallet`}
              variant="body2"
              underline="hover"
              sx={{ fontWeight: 600, whiteSpace: 'nowrap' }}
            >
              View wallet
            </Link>
          </Box>
        </Box>
      )}
    </Box>
  );
}
