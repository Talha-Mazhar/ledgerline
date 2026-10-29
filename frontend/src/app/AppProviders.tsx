import CssBaseline from '@mui/material/CssBaseline';
import { ThemeProvider } from '@mui/material/styles';
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { useState, type ReactNode } from 'react';
import { createQueryClient } from '@/app/queryClient';
import { theme } from '@/app/theme';

interface AppProvidersProps {
  readonly children: ReactNode;
  /** Tests pass their own client; the app creates one. */
  readonly queryClient?: QueryClient;
}

/** Everything the app needs around it: theme, CSS reset and the server-state cache. */
export function AppProviders({ children, queryClient }: AppProvidersProps) {
  // useState, not a module constant: one client per mounted app, created once.
  const [client] = useState(() => queryClient ?? createQueryClient());

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={client}>
        {children}
        {/* Left out of production builds by the package itself. */}
        <ReactQueryDevtools buttonPosition="bottom-left" />
      </QueryClientProvider>
    </ThemeProvider>
  );
}
