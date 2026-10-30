import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import type { ReactNode } from 'react';

interface EmptyStateProps {
  readonly title: string;
  readonly description: string;
  readonly action?: ReactNode;
}

/** Shown where a section has nothing in it yet, with the next step to take. */
export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <Box sx={{ py: 5, px: 3, textAlign: 'center' }}>
      <Typography variant="h3" component="p">
        {title}
      </Typography>
      <Typography sx={{ mt: 0.75, color: 'text.secondary', maxWidth: 440, mx: 'auto' }}>
        {description}
      </Typography>
      {action}
    </Box>
  );
}
