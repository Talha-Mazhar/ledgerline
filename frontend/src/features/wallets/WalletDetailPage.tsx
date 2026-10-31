import AddRounded from '@mui/icons-material/AddRounded';
import ArrowBackRounded from '@mui/icons-material/ArrowBackRounded';
import NorthEastRounded from '@mui/icons-material/NorthEastRounded';
import ContentCopyRounded from '@mui/icons-material/ContentCopyRounded';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Link from '@mui/material/Link';
import Paper from '@mui/material/Paper';
import Skeleton from '@mui/material/Skeleton';
import Snackbar from '@mui/material/Snackbar';
import Stack from '@mui/material/Stack';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import { useQuery } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { Link as RouterLink, useParams } from 'react-router';
import { ActivityLedger } from '@/features/activity/ActivityLedger';
import { MoveMoneyDialog, type MoveMoneyMode } from '@/features/payments/MoveMoneyDialog';
import type { Wallet } from '@/shared/api/generated';
import {
  getWalletOptions,
  listWalletActivityOptions,
} from '@/shared/api/generated/@tanstack/react-query.gen';
import { problemStatus } from '@/shared/api/problem';
import { paths } from '@/shared/config/paths';
import { currencies } from '@/shared/money/currencies';
import { formatDateTime } from '@/shared/money/format';
import { CurrencyMark } from '@/shared/ui/CurrencyMark';
import { ErrorState } from '@/shared/ui/ErrorState';
import { Figure } from '@/shared/ui/Figure';
import { PageHeader } from '@/shared/ui/PageHeader';

/** One wallet: its balance, its details and its activity. */
export function WalletDetailPage() {
  const { walletId = '' } = useParams();
  const wallet = useQuery({ ...getWalletOptions({ path: { walletId } }), retry: false });

  const back = (
    <Link
      component={RouterLink}
      to={paths.overview}
      underline="hover"
      sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, mb: 1.5, fontWeight: 500 }}
    >
      <ArrowBackRounded fontSize="small" aria-hidden />
      Overview
    </Link>
  );

  if (wallet.isError) {
    const status = problemStatus(wallet.error);
    const missing = status === 404 || status === 400;
    return (
      <>
        <PageHeader back={back} title={missing ? 'Wallet not found' : 'Wallet'} />
        <ErrorState
          title={missing ? "This wallet doesn't exist" : "This wallet couldn't be loaded"}
          description={
            missing
              ? 'The link may be wrong, or the wallet belongs to someone else. Your wallets are on the overview.'
              : "The server didn't answer. Check your connection and try again."
          }
          onRetry={missing ? undefined : () => void wallet.refetch()}
        />
      </>
    );
  }

  if (wallet.data === undefined) {
    return (
      <>
        <PageHeader back={back} title={<Skeleton width={280} />} />
        <Skeleton variant="rounded" height={160} />
      </>
    );
  }

  return <WalletDetails wallet={wallet.data} back={back} />;
}

interface WalletDetailsProps {
  readonly wallet: Wallet;
  readonly back: ReactNode;
}

function WalletDetails({ wallet, back }: WalletDetailsProps) {
  const { name } = currencies[wallet.currency];
  const activity = useQuery(listWalletActivityOptions({ path: { walletId: wallet.id } }));
  const [moving, setMoving] = useState<MoveMoneyMode | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  return (
    <>
      <PageHeader
        back={back}
        title={
          <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 1.5 }}>
            <CurrencyMark currency={wallet.currency} size={36} />
            {name} wallet
          </Box>
        }
        actions={
          <Stack direction="row" spacing={1.5}>
            <Button
              variant="contained"
              size="large"
              startIcon={<AddRounded />}
              onClick={() => setMoving('deposit')}
            >
              Add money
            </Button>
            <Button
              variant="outlined"
              size="large"
              startIcon={<NorthEastRounded />}
              onClick={() => setMoving('withdraw')}
            >
              Withdraw
            </Button>
          </Stack>
        }
      />

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1fr) minmax(0, 1.2fr)' },
          gap: { xs: 3, md: 6 },
          alignItems: 'end',
          mb: { xs: 4, md: 6 },
        }}
      >
        <div>
          <Typography sx={{ color: 'text.secondary', mb: 1 }}>Balance</Typography>
          <Figure money={wallet.balance} size="large" />
        </div>
        <WalletFacts wallet={wallet} />
      </Box>

      <ActivityLedger
        items={activity.data?.items}
        showBalance
        error={activity.isError ? { onRetry: () => void activity.refetch() } : undefined}
        emptyMessage={`No entries yet. Money added to or sent from this ${name.toLowerCase()} wallet is recorded here.`}
      />

      <MoveMoneyDialog
        wallet={wallet}
        mode={moving}
        onClose={() => setMoving(null)}
        onDone={setNotice}
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

function WalletFacts({ wallet }: { readonly wallet: Wallet }) {
  const [copied, setCopied] = useState(false);
  const facts: readonly (readonly [string, ReactNode])[] = [
    ['Currency', `${currencies[wallet.currency].name} (${wallet.currency})`],
    [
      'Status',
      <Box
        component="span"
        key="status"
        sx={{ display: 'inline-flex', alignItems: 'center', gap: 1 }}
      >
        <Box
          component="span"
          aria-hidden
          sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: 'success.main' }}
        />
        Active
      </Box>,
    ],
    ['Opened', formatDateTime(wallet.openedAt)],
    [
      'Wallet ID',
      <Box
        component="span"
        key="id"
        sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, minWidth: 0 }}
      >
        <Box
          component="span"
          sx={{ overflow: 'hidden', textOverflow: 'ellipsis', fontVariantNumeric: 'tabular-nums' }}
        >
          {wallet.id}
        </Box>
        <Tooltip title={copied ? 'Copied' : 'Copy wallet ID'}>
          <IconButton
            size="small"
            aria-label="Copy wallet ID"
            onClick={() => {
              void navigator.clipboard.writeText(wallet.id).then(() => setCopied(true));
            }}
          >
            <ContentCopyRounded fontSize="inherit" />
          </IconButton>
        </Tooltip>
      </Box>,
    ],
  ];

  return (
    <Paper variant="outlined" component="dl" sx={{ m: 0, px: 2.5, py: 0.5 }}>
      {facts.map(([label, value]) => (
        <Box
          key={label}
          sx={{
            display: 'grid',
            gridTemplateColumns: '7.5rem minmax(0, 1fr)',
            alignItems: 'center',
            gap: 2,
            py: 1.25,
            '&:not(:last-of-type)': { borderBottom: 1, borderColor: 'ledger.rule' },
          }}
        >
          <Typography component="dt" variant="body2" sx={{ color: 'text.secondary' }}>
            {label}
          </Typography>
          <Typography component="dd" variant="body2" sx={{ m: 0, fontWeight: 500, minWidth: 0 }}>
            {value}
          </Typography>
        </Box>
      ))}
    </Paper>
  );
}
