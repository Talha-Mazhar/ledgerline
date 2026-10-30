import Button from '@mui/material/Button';
import { Link } from 'react-router';
import { paths } from '@/shared/config/paths';
import { PageHeader } from '@/shared/ui/PageHeader';

export function NotFoundPage() {
  return (
    <PageHeader
      title="Page not found"
      description="The address may be mistyped, or the page has moved."
      actions={
        <Button component={Link} to={paths.overview} variant="contained">
          Go to overview
        </Button>
      }
    />
  );
}
