import UploadFileRounded from '@mui/icons-material/UploadFileRounded';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Paper from '@mui/material/Paper';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useId, useState } from 'react';
import type { Limits, Verification, VerificationStatus } from '@/shared/api/generated';
import {
  getLimitsOptions,
  getVerificationOptions,
  submitVerificationMutation,
} from '@/shared/api/generated/@tanstack/react-query.gen';
import { problemMessage } from '@/shared/api/problem';
import { formatDateTime, formatMoney } from '@/shared/money/format';
import { ErrorState } from '@/shared/ui/ErrorState';
import { PageHeader } from '@/shared/ui/PageHeader';

/** The backend accepts these, up to 5 MB; it checks the file's content, not just its name. */
const ACCEPTED = ['application/pdf', 'image/jpeg', 'image/png'];
const MAX_BYTES = 5 * 1024 * 1024;

const statuses = {
  NOT_STARTED: { label: 'Not verified', color: 'default' },
  PENDING_REVIEW: { label: 'In review', color: 'warning' },
  VERIFIED: { label: 'Verified', color: 'success' },
  REJECTED: { label: 'Not accepted', color: 'error' },
} as const satisfies Record<
  VerificationStatus,
  { label: string; color: 'default' | 'warning' | 'success' | 'error' }
>;

/**
 * Identity verification (KYC) and the limits that come with it. A new customer is BASIC, with low
 * limits; uploading an identity document that our team approves makes them VERIFIED.
 */
export function VerificationPage() {
  const verification = useQuery(getVerificationOptions());
  const limits = useQuery(getLimitsOptions());

  return (
    <>
      <PageHeader
        title="Identity & limits"
        description="Verify your identity to send and withdraw more."
      />
      {verification.isError || limits.isError ? (
        <ErrorState
          title="Your verification couldn't be loaded"
          description="The server didn't answer. Check your connection and try again."
          onRetry={() => {
            void verification.refetch();
            void limits.refetch();
          }}
        />
      ) : (
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1.3fr) minmax(0, 1fr)' },
            gap: 3,
            alignItems: 'start',
          }}
        >
          {verification.data === undefined ? (
            <Skeleton variant="rounded" height={220} />
          ) : (
            <VerificationCard verification={verification.data} />
          )}
          {limits.data === undefined ? (
            <Skeleton variant="rounded" height={220} />
          ) : (
            <LimitsCard limits={limits.data} />
          )}
        </Box>
      )}
    </>
  );
}

function VerificationCard({ verification }: { readonly verification: Verification }) {
  const status = statuses[verification.status];
  const canUpload = verification.status === 'NOT_STARTED' || verification.status === 'REJECTED';

  return (
    <Paper
      variant="outlined"
      component="section"
      aria-labelledby="verification-heading"
      sx={{ p: 3 }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h2" id="verification-heading">
          Identity
        </Typography>
        <Chip label={status.label} color={status.color} variant="outlined" />
      </Box>

      {verification.status === 'VERIFIED' && (
        <Typography>
          Your identity is confirmed
          {typeof verification.reviewedAt === 'string' &&
            ` since ${formatDateTime(verification.reviewedAt)}`}
          . You have the higher limits of a verified customer.
        </Typography>
      )}
      {verification.status === 'PENDING_REVIEW' && (
        <Typography>
          We received {verification.documentName ?? 'your document'}
          {typeof verification.submittedAt === 'string' &&
            ` on ${formatDateTime(verification.submittedAt)}`}
          . Our team checks it, usually within a day; we&apos;ll let you know.
        </Typography>
      )}
      {verification.status === 'REJECTED' && (
        <Alert severity="error" sx={{ mb: 2 }}>
          We couldn&apos;t accept your document: {verification.rejectionReason ?? 'no reason given'}
          . Please upload another one.
        </Alert>
      )}
      {verification.status === 'NOT_STARTED' && (
        <Typography sx={{ mb: 2 }}>
          Upload a photo or scan of your passport, ID card or driving licence. Our team checks it
          and raises your limits.
        </Typography>
      )}
      {canUpload && <UploadForm />}
    </Paper>
  );
}

function UploadForm() {
  const queryClient = useQueryClient();
  const inputId = useId();
  const [file, setFile] = useState<File | null>(null);
  const [problem, setProblem] = useState<string | null>(null);
  const submit = useMutation({
    ...submitVerificationMutation(),
    onSuccess: () => queryClient.invalidateQueries(),
  });

  function choose(chosen: File | undefined) {
    submit.reset();
    setFile(null);
    setProblem(null);
    if (chosen === undefined) {
      return;
    }
    if (!ACCEPTED.includes(chosen.type)) {
      setProblem('Choose a PDF, JPEG or PNG file.');
    } else if (chosen.size > MAX_BYTES) {
      setProblem('The file is larger than 5 MB.');
    } else {
      setFile(chosen);
    }
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center', flexWrap: 'wrap' }}>
        <Button
          component="label"
          htmlFor={inputId}
          variant="outlined"
          startIcon={<UploadFileRounded />}
        >
          Choose document
          <input
            id={inputId}
            type="file"
            accept={ACCEPTED.join(',')}
            aria-label="Identity document"
            hidden
            onChange={(event) => choose(event.target.files?.[0])}
          />
        </Button>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {file?.name ?? 'PDF, JPEG or PNG, up to 5 MB'}
        </Typography>
      </Box>
      {problem !== null && (
        <Alert severity="warning" sx={{ mt: 2 }}>
          {problem}
        </Alert>
      )}
      {submit.isError && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {problemMessage(submit.error, "The document wasn't uploaded. Try again in a moment.")}
        </Alert>
      )}
      <Button
        variant="contained"
        sx={{ mt: 2 }}
        disabled={file === null}
        loading={submit.isPending}
        onClick={() => {
          if (file !== null) {
            submit.mutate({ body: { document: file } });
          }
        }}
      >
        Send for review
      </Button>
    </Box>
  );
}

function LimitsCard({ limits }: { readonly limits: Limits }) {
  const rows = [
    ['Single payment', limits.singlePayment],
    ['Per day', limits.daily],
  ] as const;
  return (
    <Paper variant="outlined" component="section" aria-labelledby="limits-heading" sx={{ p: 3 }}>
      <Typography variant="h2" id="limits-heading" sx={{ mb: 0.5 }}>
        Your limits
      </Typography>
      <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
        {limits.tier === 'VERIFIED' ? 'Verified customer' : 'Basic customer'}. Payments above a
        limit wait for approval by our team.
      </Typography>
      <Box component="dl" sx={{ m: 0 }}>
        {rows.map(([label, amount]) => (
          <Box
            key={label}
            sx={{
              display: 'flex',
              justifyContent: 'space-between',
              py: 1.25,
              '&:not(:last-of-type)': { borderBottom: 1, borderColor: 'ledger.rule' },
            }}
          >
            <Typography component="dt" sx={{ color: 'text.secondary' }}>
              {label}
            </Typography>
            <Typography
              component="dd"
              sx={{ m: 0, fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}
            >
              {formatMoney({ amount, currency: limits.currency })}
            </Typography>
          </Box>
        ))}
      </Box>
    </Paper>
  );
}
