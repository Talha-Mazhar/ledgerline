import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';

/**
 * The Ledgerline wordmark. The mark is a ledger's double rule: the two lines drawn under a final
 * total, turned upright.
 */
export function BrandMark() {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
      <Box component="svg" viewBox="0 0 20 24" sx={{ width: 18, height: 22 }} aria-hidden>
        <rect x="2" y="0" width="4" height="24" rx="1" fill="#8ea2ff" />
        <rect x="11" y="0" width="4" height="24" rx="1" fill="currentColor" />
      </Box>
      <Typography
        component="span"
        sx={{ fontWeight: 650, fontSize: '1.0625rem', letterSpacing: '-0.01em' }}
      >
        Ledgerline
      </Typography>
    </Box>
  );
}
