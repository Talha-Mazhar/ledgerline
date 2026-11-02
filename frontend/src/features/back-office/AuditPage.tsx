import Paper from '@mui/material/Paper';
import Skeleton from '@mui/material/Skeleton';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useQuery } from '@tanstack/react-query';
import { listAuditEntriesOptions } from '@/shared/api/generated/@tanstack/react-query.gen';
import { formatDateTime } from '@/shared/money/format';
import { EmptyState } from '@/shared/ui/EmptyState';
import { ErrorState } from '@/shared/ui/ErrorState';
import { PageHeader } from '@/shared/ui/PageHeader';

/** "PAYMENT_APPROVED" → "Payment approved" */
function sentence(action: string): string {
  const words = action.toLowerCase().replaceAll('_', ' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}

/**
 * Who did what, and when: every approval, rejection, refund, verification decision and role
 * change. The database only lets rows be added, never changed or deleted.
 */
export function AuditPage() {
  const entries = useQuery(listAuditEntriesOptions({ query: { limit: 200 } }));

  return (
    <>
      <PageHeader
        title="Audit log"
        description="Every staff decision and sensitive change, newest first. Entries can't be edited or deleted."
      />
      {entries.isError ? (
        <ErrorState
          title="The audit log couldn't be loaded"
          description="The server didn't answer. Check your connection and try again."
          onRetry={() => void entries.refetch()}
        />
      ) : (
        <Paper variant="outlined" sx={{ overflowX: 'auto' }}>
          {entries.data?.items.length === 0 ? (
            <EmptyState title="Nothing recorded yet" description="Staff decisions appear here." />
          ) : (
            <Table size="small" aria-label="Audit log" sx={{ minWidth: 720 }}>
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
                  <TableCell>When</TableCell>
                  <TableCell>Who</TableCell>
                  <TableCell>What</TableCell>
                  <TableCell>Details</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {entries.data === undefined ? (
                  <TableRow>
                    <TableCell colSpan={4}>
                      <Skeleton variant="text" />
                    </TableCell>
                  </TableRow>
                ) : (
                  entries.data.items.map((entry) => (
                    <TableRow
                      key={entry.id}
                      sx={{ '& td': { borderColor: 'ledger.rule', py: 1.25 } }}
                    >
                      <TableCell sx={{ whiteSpace: 'nowrap', color: 'text.secondary' }}>
                        {formatDateTime(entry.occurredAt)}
                      </TableCell>
                      <TableCell>{entry.actor ?? 'System'}</TableCell>
                      <TableCell sx={{ fontWeight: 500 }}>{sentence(entry.action)}</TableCell>
                      <TableCell>
                        <Typography variant="body2">{entry.details}</Typography>
                        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                          {entry.targetType.toLowerCase()} {entry.targetId}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          )}
        </Paper>
      )}
    </>
  );
}
