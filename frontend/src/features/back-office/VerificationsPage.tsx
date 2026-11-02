import OpenInNewRounded from '@mui/icons-material/OpenInNewRounded';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Paper from '@mui/material/Paper';
import Skeleton from '@mui/material/Skeleton';
import Snackbar from '@mui/material/Snackbar';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ReasonDialog } from '@/features/back-office/ReasonDialog';
import type { ReviewItem } from '@/shared/api/generated';
import { client } from '@/shared/api/generated/client.gen';
import {
  approveVerificationMutation,
  listVerificationsToReviewOptions,
  rejectVerificationMutation,
} from '@/shared/api/generated/@tanstack/react-query.gen';
import { problemMessage } from '@/shared/api/problem';
import { formatDateTime } from '@/shared/money/format';
import { EmptyState } from '@/shared/ui/EmptyState';
import { ErrorState } from '@/shared/ui/ErrorState';
import { PageHeader } from '@/shared/ui/PageHeader';

/**
 * Identity documents waiting for a check. Staff open the document, compare it with the
 * customer's name, and approve (the customer's limits go up) or reject with a reason.
 */
export function VerificationsPage() {
  const queue = useQuery(listVerificationsToReviewOptions());
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [rejecting, setRejecting] = useState<ReviewItem | null>(null);
  const base = client.getConfig().baseUrl ?? '';

  const approve = useMutation({
    ...approveVerificationMutation(),
    onSuccess: async (_, { path }) => {
      await queryClient.invalidateQueries();
      const item = queue.data?.items.find((candidate) => candidate.userId === path.userId);
      setNotice(`${item?.displayName ?? 'The customer'} is verified.`);
    },
    onError: (error) =>
      setActionError(problemMessage(error, "The verification wasn't approved. Try again.")),
  });
  const reject = useMutation({
    ...rejectVerificationMutation(),
    onSuccess: async () => {
      await queryClient.invalidateQueries();
      setNotice(`${rejecting?.displayName ?? 'The customer'} is asked for another document.`);
      setRejecting(null);
    },
  });

  return (
    <>
      <PageHeader
        title="Verifications"
        description="Identity documents customers uploaded. Check that the document is genuine and the name matches."
      />
      {actionError !== null && (
        <Alert severity="error" onClose={() => setActionError(null)} sx={{ mb: 2 }}>
          {actionError}
        </Alert>
      )}
      {queue.isError ? (
        <ErrorState
          title="The review queue couldn't be loaded"
          description="The server didn't answer. Check your connection and try again."
          onRetry={() => void queue.refetch()}
        />
      ) : (
        <Paper variant="outlined" sx={{ overflowX: 'auto' }}>
          {queue.data?.items.length === 0 ? (
            <EmptyState
              title="No documents to review"
              description="New uploads appear here, and staff get a notification."
            />
          ) : (
            <Table size="small" aria-label="Documents to review" sx={{ minWidth: 720 }}>
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
                  <TableCell>Uploaded</TableCell>
                  <TableCell>Customer</TableCell>
                  <TableCell>Document</TableCell>
                  <TableCell align="right">Decision</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {queue.data === undefined ? (
                  <TableRow>
                    <TableCell colSpan={4}>
                      <Skeleton variant="text" />
                    </TableCell>
                  </TableRow>
                ) : (
                  queue.data.items.map((item) => (
                    <TableRow
                      key={item.userId}
                      sx={{ '& td': { borderColor: 'ledger.rule', py: 1.25 } }}
                    >
                      <TableCell sx={{ whiteSpace: 'nowrap', color: 'text.secondary' }}>
                        {formatDateTime(item.submittedAt)}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: 500 }}>
                          {item.displayName}
                        </Typography>
                        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                          {item.email}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Button
                          size="small"
                          href={`${base}/back-office/verifications/${encodeURIComponent(item.userId)}/document`}
                          target="_blank"
                          rel="noopener"
                          endIcon={<OpenInNewRounded fontSize="small" />}
                        >
                          {item.documentName}
                        </Button>
                      </TableCell>
                      <TableCell align="right">
                        <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                          <Button
                            size="small"
                            variant="contained"
                            disabled={approve.isPending}
                            aria-label={`Approve ${item.displayName}`}
                            onClick={() => {
                              setActionError(null);
                              approve.mutate({ path: { userId: item.userId } });
                            }}
                          >
                            Approve
                          </Button>
                          <Button
                            size="small"
                            color="error"
                            aria-label={`Reject ${item.displayName}`}
                            onClick={() => {
                              reject.reset();
                              setRejecting(item);
                            }}
                          >
                            Reject
                          </Button>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          )}
        </Paper>
      )}

      <ReasonDialog
        open={rejecting !== null}
        title="Reject document"
        description={`${rejecting?.displayName ?? 'The customer'} is asked to upload another document and sees your reason.`}
        confirmLabel="Reject"
        pending={reject.isPending}
        error={
          reject.isError
            ? problemMessage(reject.error, "The document wasn't rejected. Try again.")
            : null
        }
        onCancel={() => setRejecting(null)}
        onConfirm={(reason) => {
          if (rejecting !== null) {
            reject.mutate({ path: { userId: rejecting.userId }, body: { reason } });
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
