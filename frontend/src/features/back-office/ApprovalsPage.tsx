import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Paper from '@mui/material/Paper';
import Skeleton from '@mui/material/Skeleton';
import Snackbar from '@mui/material/Snackbar';
import Stack from '@mui/material/Stack';
import Tab from '@mui/material/Tab';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tabs from '@mui/material/Tabs';
import Typography from '@mui/material/Typography';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ReasonDialog } from '@/features/back-office/ReasonDialog';
import { PaymentStatusChip } from '@/features/payments/PaymentStatusChip';
import type { PaymentStatus, StaffPayment } from '@/shared/api/generated';
import {
  approvePaymentMutation,
  listStaffPaymentsOptions,
  refundPaymentMutation,
  rejectPaymentMutation,
} from '@/shared/api/generated/@tanstack/react-query.gen';
import { problemMessage } from '@/shared/api/problem';
import { formatDateTime, formatMoney } from '@/shared/money/format';
import { EmptyState } from '@/shared/ui/EmptyState';
import { ErrorState } from '@/shared/ui/ErrorState';
import { PageHeader } from '@/shared/ui/PageHeader';

const WAITING: PaymentStatus[] = ['HELD', 'AWAITING_SECOND_APPROVAL'];
const EVERY: PaymentStatus[] = [...WAITING, 'COMPLETED', 'REJECTED', 'REFUNDED'];

type View = 'waiting' | 'all';

/**
 * Payments held by the risk rules, waiting for staff. Releasing one needs two approvals by two
 * different people (four eyes); the backend refuses a second approval by the same person. Any
 * completed top-up or transfer can be refunded from the "All" view.
 */
export function ApprovalsPage() {
  const [view, setView] = useState<View>('waiting');
  const payments = useQuery(
    listStaffPaymentsOptions({ query: { status: view === 'waiting' ? WAITING : EVERY } }),
  );
  const [notice, setNotice] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [rejecting, setRejecting] = useState<StaffPayment | null>(null);
  const queryClient = useQueryClient();

  const refresh = () => queryClient.invalidateQueries();
  const onError = (error: unknown) =>
    setActionError(problemMessage(error, "That didn't work. Try again in a moment."));

  const approve = useMutation({
    ...approvePaymentMutation(),
    onSuccess: async (payment) => {
      await refresh();
      setNotice(
        payment.status === 'COMPLETED'
          ? `Second approval given: ${formatMoney(payment.amount)} from ${payment.customer.displayName} is released.`
          : `First approval given. A second person must approve ${formatMoney(payment.amount)} from ${payment.customer.displayName}.`,
      );
    },
    onError,
  });
  const reject = useMutation({
    ...rejectPaymentMutation(),
    onSuccess: async (payment) => {
      await refresh();
      setRejecting(null);
      setNotice(
        `Rejected; ${formatMoney(payment.amount)} is back with ${payment.customer.displayName}.`,
      );
    },
  });
  const refund = useMutation({
    ...refundPaymentMutation(),
    onSuccess: async (payment) => {
      await refresh();
      setNotice(`Refunded ${formatMoney(payment.amount)} to ${payment.customer.displayName}.`);
    },
    onError,
  });

  const busy = approve.isPending || refund.isPending;

  return (
    <>
      <PageHeader
        title="Approvals"
        description="Payments above a customer's limits, or that look unusual, wait here. Each needs two approvals from two different people."
      />

      <Tabs value={view} onChange={(_, next: View) => setView(next)} sx={{ mb: 2 }}>
        <Tab value="waiting" label="Waiting" />
        <Tab value="all" label="All payments" />
      </Tabs>

      {actionError !== null && (
        <Alert severity="error" onClose={() => setActionError(null)} sx={{ mb: 2 }}>
          {actionError}
        </Alert>
      )}

      {payments.isError ? (
        <ErrorState
          title="Payments couldn't be loaded"
          description="The server didn't answer. Check your connection and try again."
          onRetry={() => void payments.refetch()}
        />
      ) : (
        <Paper variant="outlined" sx={{ overflowX: 'auto' }}>
          {payments.data?.items.length === 0 ? (
            <EmptyState
              title={view === 'waiting' ? 'Nothing to approve' : 'No payments yet'}
              description={
                view === 'waiting'
                  ? 'Held payments appear here as soon as the risk rules stop one.'
                  : 'Customer payments appear here.'
              }
            />
          ) : (
            <Table size="small" aria-label="Payments" sx={{ minWidth: 860 }}>
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
                  <TableCell>Created</TableCell>
                  <TableCell>Customer</TableCell>
                  <TableCell>Payment</TableCell>
                  <TableCell align="right">Amount</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {payments.data === undefined
                  ? [0, 1, 2].map((row) => (
                      <TableRow key={row}>
                        <TableCell colSpan={6}>
                          <Skeleton variant="text" />
                        </TableCell>
                      </TableRow>
                    ))
                  : payments.data.items.map((payment) => (
                      <TableRow
                        key={payment.id}
                        sx={{
                          '& td': { borderColor: 'ledger.rule', py: 1.25, verticalAlign: 'top' },
                        }}
                      >
                        <TableCell sx={{ whiteSpace: 'nowrap', color: 'text.secondary' }}>
                          {formatDateTime(payment.createdAt)}
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 500 }}>
                            {payment.customer.displayName}
                          </Typography>
                          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                            {payment.customer.email}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <PaymentSummary payment={payment} />
                        </TableCell>
                        <TableCell
                          align="right"
                          sx={{
                            whiteSpace: 'nowrap',
                            fontWeight: 600,
                            fontVariantNumeric: 'tabular-nums',
                          }}
                        >
                          {formatMoney(payment.amount)}
                        </TableCell>
                        <TableCell>
                          <PaymentStatusChip status={payment.status} />
                        </TableCell>
                        <TableCell align="right">
                          <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                            {WAITING.includes(payment.status) && (
                              <>
                                <Button
                                  size="small"
                                  variant="contained"
                                  disabled={busy}
                                  aria-label={`Approve ${formatMoney(payment.amount)} from ${payment.customer.displayName}`}
                                  onClick={() => {
                                    setActionError(null);
                                    approve.mutate({ path: { paymentId: payment.id } });
                                  }}
                                >
                                  Approve
                                </Button>
                                <Button
                                  size="small"
                                  color="error"
                                  disabled={busy}
                                  aria-label={`Reject ${formatMoney(payment.amount)} from ${payment.customer.displayName}`}
                                  onClick={() => {
                                    setActionError(null);
                                    reject.reset();
                                    setRejecting(payment);
                                  }}
                                >
                                  Reject
                                </Button>
                              </>
                            )}
                            {payment.refundable && (
                              <Button
                                size="small"
                                disabled={busy}
                                aria-label={`Refund ${formatMoney(payment.amount)} to ${payment.customer.displayName}`}
                                onClick={() => {
                                  setActionError(null);
                                  refund.mutate({ path: { paymentId: payment.id } });
                                }}
                              >
                                Refund
                              </Button>
                            )}
                          </Stack>
                        </TableCell>
                      </TableRow>
                    ))}
              </TableBody>
            </Table>
          )}
        </Paper>
      )}

      <ReasonDialog
        open={rejecting !== null}
        title="Reject payment"
        description={
          rejecting === null
            ? ''
            : `The ${formatMoney(rejecting.amount)} set aside goes back to ${rejecting.customer.displayName}, who sees your reason.`
        }
        confirmLabel="Reject"
        pending={reject.isPending}
        error={
          reject.isError
            ? problemMessage(reject.error, "The payment wasn't rejected. Try again.")
            : null
        }
        onCancel={() => setRejecting(null)}
        onConfirm={(reason) => {
          if (rejecting !== null) {
            reject.mutate({ path: { paymentId: rejecting.id }, body: { reason } });
          }
        }}
      />

      <Snackbar
        open={notice !== null}
        message={notice}
        autoHideDuration={5000}
        onClose={() => setNotice(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      />
    </>
  );
}

const kinds = {
  DEPOSIT: 'Top-up',
  WITHDRAWAL: 'Withdrawal',
  TRANSFER: 'Transfer',
  EXCHANGE: 'Exchange',
  REFUND: 'Refund',
} as const;

function PaymentSummary({ payment }: { readonly payment: StaffPayment }) {
  const approvers = [payment.firstApprover, payment.secondApprover].filter(
    (name): name is string => typeof name === 'string',
  );
  return (
    <Box>
      <Typography variant="body2" sx={{ fontWeight: 500 }}>
        {kinds[payment.kind]}
        {payment.counterparty !== null &&
          payment.counterparty !== undefined &&
          ` to ${payment.counterparty.displayName}`}
      </Typography>
      {typeof payment.holdReason === 'string' && (
        <Typography variant="caption" component="p" sx={{ color: 'warning.dark' }}>
          Held: {payment.holdReason}
        </Typography>
      )}
      {typeof payment.rejectionReason === 'string' && (
        <Typography variant="caption" component="p" sx={{ color: 'error.main' }}>
          Rejected: {payment.rejectionReason}
        </Typography>
      )}
      {approvers.length > 0 && (
        <Typography variant="caption" component="p" sx={{ color: 'text.secondary' }}>
          Approved by {approvers.join(' and ')}
        </Typography>
      )}
    </Box>
  );
}
