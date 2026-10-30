import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';

interface PageHeaderProps {
  readonly title: string;
  readonly description?: string;
  /** Buttons shown on the right, e.g. "New transfer". */
  readonly actions?: ReactNode;
}

/** Title row at the top of every page. The title is the page's only h1. */
export function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={2}
      sx={{ mb: 4, alignItems: { sm: 'flex-end' }, justifyContent: 'space-between' }}
    >
      <div>
        <Typography variant="h1">{title}</Typography>
        {description !== undefined && (
          <Typography sx={{ mt: 0.5, color: 'text.secondary' }}>{description}</Typography>
        )}
      </div>
      {actions}
    </Stack>
  );
}
