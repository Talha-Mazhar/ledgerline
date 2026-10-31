import Box from '@mui/material/Box';
import type { CurrencyCode } from '@/shared/api/generated';
import { currencies } from '@/shared/money/currencies';

interface CurrencyMarkProps {
  readonly currency: CurrencyCode;
  readonly size?: number;
}

/** The currency's symbol in a disc of its colour. Decorative: the name is always written next to it. */
export function CurrencyMark({ currency, size = 32 }: CurrencyMarkProps) {
  const { symbol, tone } = currencies[currency];
  return (
    <Box
      aria-hidden
      sx={{
        width: size,
        height: size,
        flexShrink: 0,
        display: 'grid',
        placeItems: 'center',
        borderRadius: '50%',
        bgcolor: tone,
        color: '#fff',
        fontSize: size * (symbol.length > 1 ? 0.36 : 0.46),
        fontWeight: 600,
        lineHeight: 1,
      }}
    >
      {symbol}
    </Box>
  );
}
