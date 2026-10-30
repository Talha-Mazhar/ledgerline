import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';

/** Height of one ledger line, in pixels. */
const ROW = 48;

interface ActivityLedgerProps {
  /** What the empty ledger says, e.g. where this wallet's entries will come from. */
  readonly emptyMessage: string;
}

/**
 * The record of money moving in and out, laid out as a ledger: date, description, wallet, amount.
 * Nothing moves money yet, so it shows its columns and an empty, ruled page; the rows arrive with
 * transfers.
 */
export function ActivityLedger({ emptyMessage }: ActivityLedgerProps) {
  return (
    <section aria-labelledby="activity-heading">
      <Typography variant="h2" id="activity-heading" sx={{ mb: 1.5 }}>
        Activity
      </Typography>
      <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
        <Table size="small" aria-labelledby="activity-heading">
          <TableHead>
            <TableRow
              sx={{
                '& th': {
                  color: 'text.secondary',
                  fontWeight: 500,
                  py: 1.25,
                  borderColor: 'ledger.rule',
                },
              }}
            >
              <TableCell>Date</TableCell>
              <TableCell>Description</TableCell>
              <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' } }}>Wallet</TableCell>
              <TableCell align="right">Amount</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell colSpan={4} sx={{ p: 0, border: 0 }}>
                <Box
                  sx={(theme) => ({
                    px: 2,
                    minHeight: ROW * 3,
                    // Faint ruling, like the empty lines of an accounts book. The message sits
                    // on the first line; the next two stay blank.
                    backgroundImage: `repeating-linear-gradient(to bottom, transparent 0, transparent ${ROW - 1}px, ${theme.palette.ledger.rule} ${ROW - 1}px, ${theme.palette.ledger.rule} ${ROW}px)`,
                  })}
                >
                  <Typography sx={{ color: 'text.secondary', lineHeight: `${ROW}px` }}>
                    {emptyMessage}
                  </Typography>
                </Box>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Paper>
    </section>
  );
}
