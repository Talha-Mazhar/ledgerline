import { createTheme } from '@mui/material/styles';

/**
 * Ledgerline design tokens on top of MUI. Components read colours, spacing and type from here, never
 * hard-coded values, so the whole app changes from one file. `cssVariables` exposes the tokens as CSS
 * custom properties, which keeps a later dark mode cheap.
 */
export const theme = createTheme({
  cssVariables: true,
  palette: {
    primary: { main: '#2446d8' },
    secondary: { main: '#0f9d7a' },
    background: { default: '#f6f7fb', paper: '#ffffff' },
    text: { primary: '#141a2e', secondary: '#5b6378' },
    divider: '#e3e6ef',
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: '"Inter Variable", "Helvetica Neue", Arial, sans-serif',
    h1: { fontSize: '1.75rem', fontWeight: 600, letterSpacing: '-0.01em' },
    h2: { fontSize: '1.25rem', fontWeight: 600 },
    h3: { fontSize: '1.0625rem', fontWeight: 600 },
    button: { textTransform: 'none', fontWeight: 600 },
  },
  components: {
    MuiAppBar: {
      defaultProps: { elevation: 0, color: 'inherit' },
      styleOverrides: {
        root: ({ theme: current }) => ({ borderBottom: `1px solid ${current.palette.divider}` }),
      },
    },
    MuiButton: { defaultProps: { disableElevation: true } },
    MuiCard: { defaultProps: { variant: 'outlined' } },
  },
});
