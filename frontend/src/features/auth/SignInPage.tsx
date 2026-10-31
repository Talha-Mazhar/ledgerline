import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Navigate, Link as RouterLink, useLocation } from 'react-router';
import { z } from 'zod';
import { AuthLayout } from '@/features/auth/AuthLayout';
import { demoAccount } from '@/features/auth/demoAccounts';
import { returnPath, useStartSession } from '@/features/auth/useStartSession';
import {
  getCurrentUserOptions,
  signInMutation,
} from '@/shared/api/generated/@tanstack/react-query.gen';
import { problemMessage } from '@/shared/api/problem';
import { paths } from '@/shared/config/paths';

const signInForm = z.object({
  email: z.string().trim().min(1, 'Enter your email.'),
  password: z.string().min(1, 'Enter your password.'),
});

type SignInForm = z.infer<typeof signInForm>;

/**
 * Sign-in with email and password, or with one click into the demo account.
 *
 * The page asks who is signed in when it opens. A signed-in user is sent on to the overview; for
 * everyone else the answer (a 401) also hands the browser its CSRF cookie, which the sign-in POST
 * needs. That's why the buttons wait until the answer is in.
 */
export function SignInPage() {
  const location = useLocation();
  const session = useQuery({ ...getCurrentUserOptions(), retry: false, staleTime: Infinity });
  const startSession = useStartSession();
  const signIn = useMutation({ ...signInMutation(), onSuccess: startSession });
  const { register, handleSubmit, reset, formState } = useForm<SignInForm>({
    resolver: zodResolver(signInForm),
    defaultValues: { email: '', password: '' },
  });

  if (session.data !== undefined && !signIn.isPending) {
    return <Navigate to={returnPath(location.state)} replace />;
  }

  const ready = !session.isPending;

  return (
    <AuthLayout title="Sign in" subtitle="Welcome back. Your wallets are where you left them.">
      <form
        noValidate
        onSubmit={(event) => {
          void handleSubmit((values) => signIn.mutate({ body: values }))(event);
        }}
      >
        <Stack spacing={2.5}>
          <TextField
            {...register('email')}
            label="Email"
            type="email"
            autoComplete="email"
            fullWidth
            error={formState.errors.email !== undefined}
            helperText={formState.errors.email?.message}
          />
          <TextField
            {...register('password')}
            label="Password"
            type="password"
            autoComplete="current-password"
            fullWidth
            error={formState.errors.password !== undefined}
            helperText={formState.errors.password?.message}
          />
          {signIn.isError && (
            <Alert severity="error">
              {problemMessage(signIn.error, "Signing in didn't work. Try again in a moment.")}
            </Alert>
          )}
          <Button
            type="submit"
            variant="contained"
            size="large"
            fullWidth
            disabled={!ready}
            loading={signIn.isPending}
          >
            Sign in
          </Button>
        </Stack>
      </form>

      <Divider sx={{ my: 3, color: 'text.secondary', fontSize: '0.875rem' }}>or</Divider>

      <Button
        variant="outlined"
        size="large"
        fullWidth
        disabled={!ready || signIn.isPending}
        onClick={() => {
          reset(demoAccount);
          signIn.mutate({ body: demoAccount });
        }}
      >
        Try the demo account
      </Button>
      <Typography variant="body2" sx={{ color: 'text.secondary', mt: 1.5, textAlign: 'center' }}>
        Sample data only. No real money moves anywhere in Ledgerline.
      </Typography>

      <Typography sx={{ mt: 5 }}>
        New to Ledgerline?{' '}
        <Link component={RouterLink} to={paths.signUp} sx={{ fontWeight: 600 }}>
          Create an account
        </Link>
      </Typography>
    </AuthLayout>
  );
}
