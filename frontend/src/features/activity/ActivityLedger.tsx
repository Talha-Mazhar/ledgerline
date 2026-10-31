import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Paper from '@mui/material/Paper';
import Skeleton from '@mui/material/Skeleton';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import type { ActivityItem } from '@/shared/api/generated';
import { formatDate, formatMoney, formatSignedMoney } from '@/shared/money/format';
import { CurrencyMark } from '@/shared/ui/CurrencyMark';

/** Height of one ledger line, in pixels. */
const ROW = 48;

interface ActivityLedgerProps {
  /** The lines to show, newest first, or undefined while they load. */
  readonly items: readonly ActivityItem[] | undefined;
  /** What the empty ledger says, e.g. where this wallet's entries will come from. */
  readonly emptyMessage: string;
  /** Shows which wallet each line belongs to; used on the overview, where lines are mixed. */
  readonly showWallet?: boolean;
  /** Shows the running balance after each line; used on a single wallet's page. */
  readonly showBalance?: boolean;
  /** Set when loading failed; shows the message and a retry button. */
  readonly error?: { readonly onRetry: () => void } | undefined;
}

/**
 * The record of money moving in and out, laid out as a ledger: date, description, amount and, on a
 * wallet's page, the balance after each line. Money in is green with a plus sign; money out keeps
 * the ink colour with a minus sign, so the eye finds incoming money first.
 */
export function ActivityLedger({
  items,
  emptyMessage,
  showWallet = false,
  showBalance = false,
  error,
}: ActivityLedgerProps) {
  const columns = 3 + (showWallet ? 1 : 0) + (showBalance ? 1 : 0);

  return (
    <section aria-labelledby="activity-heading">
      <Typography variant="h2" id="activity-heading" sx={{ mb: 1.5 }}>
        Activity
      </Typography>
      <Paper variant="outlined" sx={{ overflowX: 'auto' }}>
        <Table size="small" aria-labelledby="activity-heading" sx={{ minWidth: 520 }}>
          <TableHead>
            <TableRow
              sx={{
                '& th': {
                  color: 'text.secondary',
                  fontWeight: 500,
                  py: 1.25,
                  borderColor: 'ledger.rule',
                  whiteSpace: 'nowrap',
                },
              }}
            >
              <TableCell>Date</TableCell>
              <TableCell>Description</TableCell>
              {showWallet && <TableCell>Wallet</TableCell>}
              <TableCell align="right">Amount</TableCell>
              {showBalance && <TableCell align="right">Balance</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {error !== undefined ? (
              <MessageRow columns={columns}>
                <Typography component="span" sx={{ color: 'error.main', mr: 2 }}>
                  Activity couldn&apos;t be loaded.
                </Typography>
                <Button size="small" onClick={error.onRetry}>
                  Try again
                </Button>
              </MessageRow>
            ) : items === undefined ? (
              [0, 1, 2].map((row) => (
                <TableRow key={row}>
                  <TableCell colSpan={columns} sx={{ borderColor: 'ledger.rule' }}>
                    <Skeleton variant="text" />
                  </TableCell>
                </TableRow>
              ))
            ) : items.length === 0 ? (
              <MessageRow columns={columns}>
                <Typography component="span" sx={{ color: 'text.secondary' }}>
                  {emptyMessage}
                </Typography>
              </MessageRow>
            ) : (
              items.map((item) => (
                <ActivityRow
                  key={item.id}
                  item={item}
                  showWallet={showWallet}
                  showBalance={showBalance}
                />
              ))
            )}
          </TableBody>
        </Table>
      </Paper>
    </section>
  );
}

interface ActivityRowProps {
  readonly item: ActivityItem;
  readonly showWallet: boolean;
  readonly showBalance: boolean;
}

function ActivityRow({ item, showWallet, showBalance }: ActivityRowProps) {
  const incoming = !item.amount.amount.startsWith('-');
  return (
    <TableRow sx={{ '& td': { borderColor: 'ledger.rule', height: ROW } }}>
      <TableCell sx={{ whiteSpace: 'nowrap', color: 'text.secondary' }}>
        {formatDate(item.occurredAt)}
      </TableCell>
      <TableCell>{item.description}</TableCell>
      {showWallet && (
        <TableCell>
          <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 1 }}>
            <CurrencyMark currency={item.amount.currency} size={20} />
            {item.amount.currency}
          </Box>
        </TableCell>
      )}
      <TableCell
        align="right"
        sx={{
          whiteSpace: 'nowrap',
          fontWeight: 600,
          fontVariantNumeric: 'tabular-nums',
          color: incoming ? 'ledger.credit' : 'text.primary',
        }}
      >
        {formatSignedMoney(item.amount)}
      </TableCell>
      {showBalance && (
        <TableCell
          align="right"
          sx={{ whiteSpace: 'nowrap', color: 'text.secondary', fontVariantNumeric: 'tabular-nums' }}
        >
          {formatMoney(item.balanceAfter)}
        </TableCell>
      )}
    </TableRow>
  );
}

interface MessageRowProps {
  readonly columns: number;
  readonly children: ReactNode;
}

/** A message on the ledger's first ruled line, with two blank lines under it. */
function MessageRow({ columns, children }: MessageRowProps) {
  return (
    <TableRow>
      <TableCell colSpan={columns} sx={{ p: 0, border: 0 }}>
        <Box
          sx={(theme) => ({
            px: 2,
            minHeight: ROW * 3,
            // Faint ruling, like the empty lines of an accounts book.
            backgroundImage: `repeating-linear-gradient(to bottom, transparent 0, transparent ${ROW - 1}px, ${theme.palette.ledger.rule} ${ROW - 1}px, ${theme.palette.ledger.rule} ${ROW}px)`,
          })}
        >
          <Box sx={{ minHeight: ROW, display: 'flex', alignItems: 'center' }}>{children}</Box>
        </Box>
      </TableCell>
    </TableRow>
  );
}
