import type { SvgIconComponent } from '@mui/icons-material';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';

interface EmptyStateProps {
  readonly icon: SvgIconComponent;
  readonly title: string;
  readonly description: string;
  readonly action?: ReactNode;
}

/** Shown where a list or section has nothing in it yet, with the next step to take. */
export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
  return (
    <Card>
      <CardContent sx={{ py: 6, textAlign: 'center' }}>
        <Icon sx={{ fontSize: 40, color: 'text.secondary' }} aria-hidden />
        <Typography variant="h2" component="p" sx={{ mt: 2 }}>
          {title}
        </Typography>
        <Typography sx={{ mt: 1, color: 'text.secondary', maxWidth: 420, mx: 'auto' }}>
          {description}
        </Typography>
        {action}
      </CardContent>
    </Card>
  );
}
