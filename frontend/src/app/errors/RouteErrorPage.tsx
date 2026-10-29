import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Box from '@mui/material/Box';
import { isRouteErrorResponse, useRouteError } from 'react-router';

/**
 * Shown when a page throws while rendering or loading. `useRouteError()` returns `unknown`: anything
 * can be thrown in JavaScript, so we narrow it before reading it instead of trusting a type.
 */
export function RouteErrorPage() {
  const error = useRouteError();

  return (
    <Box sx={{ p: 4 }}>
      <Alert severity="error">
        <AlertTitle>Something went wrong</AlertTitle>
        {describe(error)}
      </Alert>
    </Box>
  );
}

function describe(error: unknown): string {
  if (isRouteErrorResponse(error)) {
    return `${error.status} ${error.statusText}`;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'An unexpected error occurred.';
}
