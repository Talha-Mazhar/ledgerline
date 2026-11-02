import SearchRounded from '@mui/icons-material/SearchRounded';
import Alert from '@mui/material/Alert';
import InputAdornment from '@mui/material/InputAdornment';
import MenuItem from '@mui/material/MenuItem';
import Paper from '@mui/material/Paper';
import Skeleton from '@mui/material/Skeleton';
import Snackbar from '@mui/material/Snackbar';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useSignedInUser } from '@/features/auth/signedInUser';
import type { Role } from '@/shared/api/generated';
import {
  changeUserRoleMutation,
  listUsersOptions,
} from '@/shared/api/generated/@tanstack/react-query.gen';
import { problemMessage } from '@/shared/api/problem';
import { useDebouncedValue } from '@/shared/hooks/useDebouncedValue';
import { ErrorState } from '@/shared/ui/ErrorState';
import { PageHeader } from '@/shared/ui/PageHeader';

const roles = {
  CUSTOMER: 'Customer',
  SUPPORT: 'Support',
  ADMIN: 'Admin',
} as const satisfies Record<Role, string>;

const roleCodes: readonly Role[] = ['CUSTOMER', 'SUPPORT', 'ADMIN'];

/**
 * Everyone with an account. Support staff can look people up; only admins change roles, and the
 * backend refuses to demote the last admin.
 */
export function UsersPage() {
  const me = useSignedInUser();
  const [search, setSearch] = useState('');
  const query = useDebouncedValue(search.trim(), 300);
  const users = useQuery({
    ...listUsersOptions(query === '' ? {} : { query: { query } }),
    placeholderData: keepPreviousData,
  });
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<string | null>(null);
  const changeRole = useMutation({
    ...changeUserRoleMutation(),
    onSuccess: async (user) => {
      await queryClient.invalidateQueries();
      setNotice(`${user.displayName} is now ${roles[user.role].toLowerCase()}.`);
    },
  });
  const isAdmin = me.role === 'ADMIN';

  return (
    <>
      <PageHeader
        title="Users"
        description={
          isAdmin
            ? 'Find a customer or colleague, and give staff their role.'
            : 'Find a customer or colleague. Only admins can change roles.'
        }
      />
      <TextField
        label="Search by name or email"
        value={search}
        onChange={(event) => setSearch(event.target.value)}
        sx={{ mb: 2, width: { xs: '100%', sm: 380 } }}
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <SearchRounded fontSize="small" />
              </InputAdornment>
            ),
          },
        }}
      />
      {changeRole.isError && (
        <Alert severity="error" onClose={() => changeRole.reset()} sx={{ mb: 2 }}>
          {problemMessage(changeRole.error, "The role wasn't changed. Try again.")}
        </Alert>
      )}
      {users.isError ? (
        <ErrorState
          title="Users couldn't be loaded"
          description="The server didn't answer. Check your connection and try again."
          onRetry={() => void users.refetch()}
        />
      ) : (
        <Paper variant="outlined" sx={{ overflowX: 'auto' }}>
          <Table size="small" aria-label="Users" sx={{ minWidth: 560 }}>
            <TableHead>
              <TableRow
                sx={{
                  '& th': { color: 'text.secondary', fontWeight: 500, borderColor: 'ledger.rule' },
                }}
              >
                <TableCell>Name</TableCell>
                <TableCell>Email</TableCell>
                <TableCell sx={{ width: 200 }}>Role</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {users.data === undefined ? (
                <TableRow>
                  <TableCell colSpan={3}>
                    <Skeleton variant="text" />
                  </TableCell>
                </TableRow>
              ) : users.data.items.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3} sx={{ color: 'text.secondary', py: 3 }}>
                    Nobody matches &ldquo;{query}&rdquo;.
                  </TableCell>
                </TableRow>
              ) : (
                users.data.items.map((user) => (
                  <TableRow key={user.id} sx={{ '& td': { borderColor: 'ledger.rule' } }}>
                    <TableCell sx={{ fontWeight: 500 }}>{user.displayName}</TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell>
                      {isAdmin ? (
                        <TextField
                          select
                          size="small"
                          fullWidth
                          value={user.role}
                          disabled={changeRole.isPending}
                          slotProps={{ htmlInput: { 'aria-label': `Role of ${user.displayName}` } }}
                          onChange={(event) => {
                            const role = roleCodes.find((code) => code === event.target.value);
                            if (role !== undefined) {
                              changeRole.mutate({ path: { userId: user.id }, body: { role } });
                            }
                          }}
                        >
                          {roleCodes.map((role) => (
                            <MenuItem key={role} value={role}>
                              {roles[role]}
                            </MenuItem>
                          ))}
                        </TextField>
                      ) : (
                        <Typography variant="body2">{roles[user.role]}</Typography>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </Paper>
      )}
      <Snackbar
        open={notice !== null}
        message={notice}
        autoHideDuration={4000}
        onClose={() => setNotice(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      />
    </>
  );
}
