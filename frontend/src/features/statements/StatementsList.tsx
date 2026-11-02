import PictureAsPdfOutlined from '@mui/icons-material/PictureAsPdfOutlined';
import Box from '@mui/material/Box';
import Link from '@mui/material/Link';
import Paper from '@mui/material/Paper';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import { useQuery } from '@tanstack/react-query';
import { client } from '@/shared/api/generated/client.gen';
import { listStatementsOptions } from '@/shared/api/generated/@tanstack/react-query.gen';

/** "October 2026" for "2026-10". */
function monthName(month: string): string {
  const [year = '', number = ''] = month.split('-');
  return new Intl.DateTimeFormat('en-GB', {
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(Date.UTC(Number(year), Number(number) - 1, 1)));
}

/**
 * Monthly statements of a wallet as PDF downloads, newest first. A statement is built from the
 * ledger, so the closing balance always matches the wallet. The current month's statement covers
 * the month so far.
 */
export function StatementsList({ walletId }: { readonly walletId: string }) {
  const statements = useQuery(listStatementsOptions({ path: { walletId } }));
  const base = client.getConfig().baseUrl ?? '';

  return (
    <Box component="section" aria-labelledby="statements-heading" sx={{ mt: { xs: 4, md: 6 } }}>
      <Typography variant="h2" id="statements-heading" sx={{ mb: 1.5 }}>
        Statements
      </Typography>
      <Paper variant="outlined" sx={{ px: 2.5, py: 1 }}>
        {statements.isError ? (
          <Typography sx={{ py: 1.5, color: 'error.main' }}>
            Statements couldn&apos;t be loaded.
          </Typography>
        ) : statements.data === undefined ? (
          <Skeleton variant="text" sx={{ my: 1.5 }} />
        ) : (
          <Box component="ul" sx={{ listStyle: 'none', m: 0, p: 0 }}>
            {statements.data.items.toReversed().map(({ month }) => (
              <Box
                component="li"
                key={month}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1.5,
                  py: 1.25,
                  '&:not(:last-of-type)': { borderBottom: 1, borderColor: 'ledger.rule' },
                }}
              >
                <PictureAsPdfOutlined
                  fontSize="small"
                  sx={{ color: 'text.secondary' }}
                  aria-hidden
                />
                <Link
                  href={`${base}/wallets/${encodeURIComponent(walletId)}/statements/${month}`}
                  download={`ledgerline-statement-${month}.pdf`}
                  underline="hover"
                  sx={{ fontWeight: 500 }}
                >
                  {monthName(month)}
                </Link>
              </Box>
            ))}
          </Box>
        )}
      </Paper>
    </Box>
  );
}
