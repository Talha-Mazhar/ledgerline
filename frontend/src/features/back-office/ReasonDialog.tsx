import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState } from 'react';

interface ReasonDialogProps {
  readonly open: boolean;
  readonly title: string;
  readonly description: string;
  readonly confirmLabel: string;
  readonly pending: boolean;
  /** The error to show, already in words for staff, or null. */
  readonly error: string | null;
  readonly onCancel: () => void;
  readonly onConfirm: (reason: string) => void;
}

/**
 * Asks staff for the reason of a rejection. The customer sees it, and it goes into the audit log,
 * so it is required.
 */
export function ReasonDialog({
  open,
  title,
  description,
  confirmLabel,
  pending,
  error,
  onCancel,
  onConfirm,
}: ReasonDialogProps) {
  const [reason, setReason] = useState('');
  const [touched, setTouched] = useState(false);
  const missing = reason.trim().length === 0;

  function close() {
    setReason('');
    setTouched(false);
    onCancel();
  }

  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="xs" aria-labelledby="reason-title">
      <form
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          setTouched(true);
          if (!missing) {
            onConfirm(reason.trim());
          }
        }}
      >
        <DialogTitle id="reason-title">{title}</DialogTitle>
        <DialogContent>
          <Typography sx={{ color: 'text.secondary', mb: 2 }}>{description}</Typography>
          <TextField
            label="Reason"
            fullWidth
            multiline
            minRows={2}
            value={reason}
            onChange={(event) => setReason(event.target.value.slice(0, 500))}
            error={touched && missing}
            helperText={touched && missing ? 'Give a reason; the customer will see it.' : ' '}
          />
          {error !== null && (
            <Alert severity="error" sx={{ mt: 1 }}>
              {error}
            </Alert>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button onClick={close}>Cancel</Button>
          <Button type="submit" variant="contained" color="error" loading={pending}>
            {confirmLabel}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
