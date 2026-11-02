import Chip from '@mui/material/Chip';
import type { PaymentStatus } from '@/shared/api/generated';

const labels = {
  COMPLETED: { label: 'Completed', color: 'success' },
  HELD: { label: 'Waiting for approval', color: 'warning' },
  AWAITING_SECOND_APPROVAL: { label: 'Waiting for second approval', color: 'warning' },
  REJECTED: { label: 'Not approved', color: 'error' },
  REFUNDED: { label: 'Refunded', color: 'default' },
} as const satisfies Record<
  PaymentStatus,
  { label: string; color: 'success' | 'warning' | 'error' | 'default' }
>;

/** A payment's status in words a customer understands. */
export function PaymentStatusChip({ status }: { readonly status: PaymentStatus }) {
  const { label, color } = labels[status];
  return <Chip size="small" variant="outlined" color={color} label={label} />;
}
