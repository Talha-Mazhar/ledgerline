import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';

interface PageHeaderProps {
  readonly title: ReactNode;
  readonly description?: string;
  /** Buttons shown on the right, e.g. "Open wallet". */
  readonly actions?: ReactNode;
  /** Shown above the title, e.g. a link back to the overview. */
  readonly back?: ReactNode;
}

/** Title row at the top of every page. The title is the page's only h1. */
export function PageHeader({ title, description, actions, back }: PageHeaderProps) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={2}
      sx={{ mb: { xs: 3, md: 5 }, alignItems: { sm: 'flex-end' }, justifyContent: 'space-between' }}
    >
      <div>
        {back}
        <Typography variant="h1">{title}</Typography>
        {description !== undefined && (
          <Typography sx={{ mt: 1, color: 'text.secondary' }}>{description}</Typography>
        )}
      </div>
      {actions}
    </Stack>
  );
}
