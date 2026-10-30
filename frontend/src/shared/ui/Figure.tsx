import Typography from '@mui/material/Typography';
import type { Money } from '@/shared/api/generated';
import { formatMoney } from '@/shared/money/format';

interface FigureProps {
  readonly money: Money;
  readonly size?: 'medium' | 'large';
}

/**
 * A money amount set as a ledger total: serif, tabular figures, and the double rule accountants
 * draw under a final balance.
 */
export function Figure({ money, size = 'medium' }: FigureProps) {
  return (
    <Typography
      variant="figure"
      sx={{
        display: 'inline-block',
        fontSize: size === 'large' ? { xs: '2.5rem', md: '3.25rem' } : '2rem',
        pb: 0.75,
        borderBottom: '3px double',
        borderColor: 'ledger.ink',
      }}
    >
      {formatMoney(money)}
    </Typography>
  );
}
