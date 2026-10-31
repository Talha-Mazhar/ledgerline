import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import { BrandMark } from '@/app/layout/BrandMark';

interface AuthLayoutProps {
  readonly title: string;
  readonly subtitle: string;
  readonly children: ReactNode;
}

/**
 * The frame of the sign-in and sign-up pages: on wide screens an ink panel that says what
 * Ledgerline is, with a specimen journal entry; next to it, on ledger paper, the form.
 */
export function AuthLayout({ title, subtitle, children }: AuthLayoutProps) {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 5fr) minmax(0, 6fr)' },
      }}
    >
      <Box
        component="aside"
        sx={{
          display: { xs: 'none', md: 'flex' },
          flexDirection: 'column',
          bgcolor: 'ledger.ink',
          color: 'ledger.onInk',
          px: 6,
          py: 5,
        }}
      >
        <BrandMark />
        <Box sx={{ mt: 'auto', maxWidth: 440 }}>
          <Typography variant="h1" component="p" sx={{ fontSize: '2.75rem', color: 'inherit' }}>
            Every cent, written down twice.
          </Typography>
          <Typography sx={{ color: 'ledger.onInkMuted', mt: 2 }}>
            Behind every wallet is a double-entry ledger. Money is never created or lost, only
            moved, and every movement balances to zero.
          </Typography>
          <JournalSpecimen />
        </Box>
      </Box>

      <Box
        component="main"
        sx={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          px: { xs: 2, sm: 4 },
          py: { xs: 4, md: 8 },
        }}
      >
        <Box sx={{ width: '100%', maxWidth: 400, mx: 'auto' }}>
          <Box sx={{ display: { md: 'none' }, mb: 5 }}>
            <BrandMark />
          </Box>
          <Typography variant="h1">{title}</Typography>
          <Typography sx={{ color: 'text.secondary', mt: 1, mb: 4 }}>{subtitle}</Typography>
          {children}
        </Box>
      </Box>
    </Box>
  );
}

const specimen = [
  ['Demo user · EUR wallet', '−30.00'],
  ['Jana Nováková · EUR wallet', '+30.00'],
] as const;

/** A decorative journal entry: two legs of a transfer that add up to zero. */
function JournalSpecimen() {
  return (
    <Box
      aria-hidden
      sx={{
        mt: 6,
        p: 2.5,
        borderRadius: 2,
        border: '1px solid rgba(255, 255, 255, 0.12)',
        bgcolor: 'rgba(255, 255, 255, 0.03)',
        fontVariantNumeric: 'tabular-nums',
      }}
    >
      <Typography
        variant="body2"
        sx={{ color: 'ledger.onInkMuted', letterSpacing: '0.08em', fontSize: '0.75rem', mb: 1.5 }}
      >
        JOURNAL ENTRY · TRANSFER
      </Typography>
      {specimen.map(([account, amount]) => (
        <Box
          key={account}
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
            py: 1,
            borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
          }}
        >
          <Typography variant="body2">{account}</Typography>
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {amount}
          </Typography>
        </Box>
      ))}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', pt: 1.25 }}>
        <Typography variant="body2" sx={{ color: 'ledger.onInkMuted' }}>
          Sum
        </Typography>
        <Typography
          variant="body2"
          sx={{ fontWeight: 600, borderBottom: '3px double', borderColor: '#8ea2ff' }}
        >
          0.00
        </Typography>
      </Box>
    </Box>
  );
}
