import { alpha, createTheme } from '@mui/material/styles';
import type { CSSProperties } from 'react';

/**
 * Ledgerline's design tokens on top of MUI. Components read colours, spacing and type from the
 * theme, never hard-coded values, so the look changes from this one file.
 *
 * The direction is ledger paper: cool grey-blue paper, navy ink, one ultramarine accent, and thin
 * rules that structure the page the way an accounts book does. The one bold element is money:
 * balances are set large in a serif with tabular figures, underlined twice like a total in a
 * ledger. Everything else stays quiet.
 */

const ink = '#121a33';
const paper = '#eef1f6';
const rule = '#d5dbe6';
const ultramarine = '#2446d8';
const credit = '#0b7a5c';
const debit = '#b42318';

const sans = '"Inter Variable", "Helvetica Neue", Arial, sans-serif';
const serif = '"Source Serif 4 Variable", Georgia, "Times New Roman", serif';

declare module '@mui/material/styles' {
  interface Palette {
    ledger: LedgerPalette;
  }
  interface PaletteOptions {
    ledger: LedgerPalette;
  }
  interface TypographyVariants {
    /** Money amounts: serif, tabular figures, so digits line up in columns. */
    figure: CSSProperties;
  }
  interface TypographyVariantsOptions {
    figure?: CSSProperties;
  }
}

declare module '@mui/material/Typography' {
  interface TypographyPropsVariantOverrides {
    figure: true;
  }
}

interface LedgerPalette {
  /** Navy used for the sidebar and the darkest text. */
  readonly ink: string;
  /** Hairline rules between ledger columns and rows. */
  readonly rule: string;
  /** Money coming in. */
  readonly credit: string;
  /** Money going out. */
  readonly debit: string;
  /** Text on the ink sidebar. */
  readonly onInk: string;
  readonly onInkMuted: string;
}

export const theme = createTheme({
  cssVariables: true,
  palette: {
    primary: { main: ultramarine },
    success: { main: credit },
    error: { main: debit },
    background: { default: paper, paper: '#ffffff' },
    text: { primary: ink, secondary: '#566079' },
    divider: rule,
    ledger: {
      ink,
      rule,
      credit,
      debit,
      onInk: '#f2f4fb',
      onInkMuted: alpha('#f2f4fb', 0.62),
    },
  },
  shape: { borderRadius: 8 },
  typography: {
    fontFamily: sans,
    h1: {
      fontFamily: serif,
      fontSize: '2.25rem',
      fontWeight: 500,
      letterSpacing: '-0.015em',
      lineHeight: 1.15,
    },
    h2: { fontSize: '1.125rem', fontWeight: 600, letterSpacing: '-0.005em' },
    h3: { fontSize: '1rem', fontWeight: 600 },
    body1: { fontSize: '0.9375rem', lineHeight: 1.55 },
    body2: { fontSize: '0.875rem', lineHeight: 1.5 },
    button: { textTransform: 'none', fontWeight: 600, letterSpacing: 0 },
    figure: {
      fontFamily: serif,
      fontSize: '2rem',
      fontWeight: 500,
      lineHeight: 1.1,
      letterSpacing: '-0.01em',
      fontVariantNumeric: 'lining-nums tabular-nums',
    },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: { WebkitFontSmoothing: 'antialiased' },
        ':focus-visible': { outline: `2px solid ${ultramarine}`, outlineOffset: 2 },
        '@media (prefers-reduced-motion: reduce)': {
          '*, *::before, *::after': {
            animationDuration: '0.01ms !important',
            transitionDuration: '0.01ms !important',
          },
        },
      },
    },
    MuiTypography: {
      defaultProps: { variantMapping: { figure: 'p' } },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { borderRadius: 6, paddingInline: 16 },
        sizeLarge: { paddingBlock: 10 },
      },
    },
    MuiPaper: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        outlined: { borderColor: rule },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: { borderRadius: 12, boxShadow: `0 24px 64px ${alpha(ink, 0.22)}` },
      },
    },
    MuiDialogTitle: {
      styleOverrides: {
        root: { fontFamily: serif, fontSize: '1.5rem', fontWeight: 500, paddingTop: 24 },
      },
    },
    MuiTooltip: {
      styleOverrides: { tooltip: { backgroundColor: ink, fontSize: '0.75rem' } },
    },
    MuiSnackbarContent: {
      styleOverrides: { root: { backgroundColor: ink, borderRadius: 8 } },
    },
  },
});
