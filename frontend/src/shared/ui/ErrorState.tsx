import Button from '@mui/material/Button';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';

interface ErrorStateProps {
  readonly title: string;
  readonly description: string;
  readonly onRetry?: () => void;
}

/** A section that failed to load: what happened and, when it can help, a way to try again. */
export function ErrorState({ title, description, onRetry }: ErrorStateProps) {
  return (
    <Paper
      variant="outlined"
      role="alert"
      sx={{ p: { xs: 3, md: 4 }, borderLeft: '3px solid', borderLeftColor: 'error.main' }}
    >
      <Typography variant="h3" component="p">
        {title}
      </Typography>
      <Typography sx={{ mt: 0.75, color: 'text.secondary' }}>{description}</Typography>
      {onRetry !== undefined && (
        <Button variant="outlined" onClick={onRetry} sx={{ mt: 2 }}>
          Try again
        </Button>
      )}
    </Paper>
  );
}
