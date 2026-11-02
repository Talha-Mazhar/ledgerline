import Paper from '@mui/material/Paper';
import Skeleton from '@mui/material/Skeleton';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useQuery } from '@tanstack/react-query';
import { PaymentStatusChip } from '@/features/payments/PaymentStatusChip';
import type { Payment, PaymentKind } from '@/shared/api/generated';
import { listPaymentsOptions } from '@/shared/api/generated/@tanstack/react-query.gen';
import { formatDate, formatMoney } from '@/shared/money/format';
import { EmptyState } from '@/shared/ui/EmptyState';
import { ErrorState } from '@/shared/ui/ErrorState';
import { PageHeader } from '@/shared/ui/PageHeader';

const kinds = {
  DEPOSIT: 'Top-up',
  WITHDRAWAL: 'Withdrawal',
  TRANSFER: 'Transfer',
  EXCHANGE: 'Exchange',
  REFUND: 'Refund',
} as const satisfies Record<PaymentKind, string>;

/**
 * Every payment the customer made or received, with its status. Unlike the activity ledger, which
 * records money that moved, this shows payments that are still waiting for approval, and why.
 */
export function PaymentsPage() {
  const payments = useQuery(listPaymentsOptions({ query: { limit: 100 } }));

  return (
    <>
      <PageHeader
        title="Payments"
        description="Your payments and their status. Large or unusual payments wait for a check by our team."
      />
      {payments.isError ? (
        <ErrorState
          title="Your payments couldn't be loaded"
          description="The server didn't answer. Check your connection and try again."
          onRetry={() => void payments.refetch()}
        />
      ) : (
        <Paper variant="outlined" sx={{ overflowX: 'auto' }}>
          {payments.data?.items.length === 0 ? (
            <EmptyState
              title="No payments yet"
              description="Money you add, send, withdraw or exchange shows up here."
            />
          ) : (
            <Table size="small" aria-label="Payments" sx={{ minWidth: 640 }}>
              <TableHead>
                <TableRow
                  sx={{
                    '& th': {
                      color: 'text.secondary',
                      fontWeight: 500,
                      borderColor: 'ledger.rule',
                    },
                  }}
                >
                  <TableCell>Date</TableCell>
                  <TableCell>Payment</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Amount</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {payments.data === undefined
                  ? [0, 1, 2].map((row) => (
                      <TableRow key={row}>
                        <TableCell colSpan={4}>
                          <Skeleton variant="text" />
                        </TableCell>
                      </TableRow>
                    ))
                  : payments.data.items.map((payment) => (
                      <PaymentRow key={payment.id} payment={payment} />
                    ))}
              </TableBody>
            </Table>
          )}
        </Paper>
      )}
    </>
  );
}

function PaymentRow({ payment }: { readonly payment: Payment }) {
  const note = payment.rejectionReason ?? payment.holdReason;
  const incoming = payment.direction === 'IN';
  return (
    <TableRow sx={{ '& td': { borderColor: 'ledger.rule', py: 1.25 } }}>
      <TableCell sx={{ whiteSpace: 'nowrap', color: 'text.secondary' }}>
        {formatDate(payment.createdAt)}
      </TableCell>
      <TableCell>
        <Typography variant="body2" sx={{ fontWeight: 500 }}>
          {payment.description}
        </Typography>
        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          {kinds[payment.kind]}
          {typeof note === 'string' && ` · ${note}`}
        </Typography>
      </TableCell>
      <TableCell>
        <PaymentStatusChip status={payment.status} />
      </TableCell>
      <TableCell
        align="right"
        sx={{
          whiteSpace: 'nowrap',
          fontWeight: 600,
          fontVariantNumeric: 'tabular-nums',
          color: incoming ? 'ledger.credit' : 'text.primary',
        }}
      >
        {incoming ? '+' : '-'}
        {formatMoney(payment.amount)}
      </TableCell>
    </TableRow>
  );
}
