import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Navigate, Link as RouterLink, useLocation } from 'react-router';
import { z } from 'zod';
import { AuthLayout } from '@/features/auth/AuthLayout';
import { returnPath, useStartSession } from '@/features/auth/useStartSession';
import {
  getCurrentUserOptions,
  signUpMutation,
} from '@/shared/api/generated/@tanstack/react-query.gen';
import { problemMessage } from '@/shared/api/problem';
import { paths } from '@/shared/config/paths';

/** The backend's rules, checked here first so mistakes show up as the user types. */
const signUpForm = z.object({
  displayName: z.string().trim().min(1, 'Enter your name.').max(80, 'Use at most 80 characters.'),
  email: z
    .string()
    .trim()
    .regex(/^[^@\s]+@[^@\s]+\.[^@\s]+$/, 'Enter a valid email address.'),
  password: z.string().min(10, 'Use at least 10 characters.').max(64, 'Use at most 64 characters.'),
});

type SignUpForm = z.infer<typeof signUpForm>;

/** Creates an account and signs straight in. */
export function SignUpPage() {
  const location = useLocation();
  const session = useQuery({ ...getCurrentUserOptions(), retry: false, staleTime: Infinity });
  const startSession = useStartSession();
  const signUp = useMutation({ ...signUpMutation(), onSuccess: startSession });
  const { register, handleSubmit, formState } = useForm<SignUpForm>({
    resolver: zodResolver(signUpForm),
    defaultValues: { displayName: '', email: '', password: '' },
  });

  if (session.data !== undefined && !signUp.isPending) {
    return <Navigate to={returnPath(location.state)} replace />;
  }

  return (
    <AuthLayout
      title="Create your account"
      subtitle="Open wallets in four currencies, move money and send it to friends."
    >
      <form
        noValidate
        onSubmit={(event) => {
          void handleSubmit((values) => signUp.mutate({ body: values }))(event);
        }}
      >
        <Stack spacing={2.5}>
          <TextField
            {...register('displayName')}
            label="Your name"
            autoComplete="name"
            fullWidth
            error={formState.errors.displayName !== undefined}
            helperText={formState.errors.displayName?.message}
          />
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
            autoComplete="new-password"
            fullWidth
            error={formState.errors.password !== undefined}
            helperText={formState.errors.password?.message ?? '10 to 64 characters.'}
          />
          {signUp.isError && (
            <Alert severity="error">
              {problemMessage(signUp.error, "The account couldn't be created. Try again.")}
            </Alert>
          )}
          <Button
            type="submit"
            variant="contained"
            size="large"
            fullWidth
            disabled={session.isPending}
            loading={signUp.isPending}
          >
            Create account
          </Button>
        </Stack>
      </form>

      <Typography sx={{ mt: 5 }}>
        Already have an account?{' '}
        <Link component={RouterLink} to={paths.signIn} sx={{ fontWeight: 600 }}>
          Sign in
        </Link>
      </Typography>
    </AuthLayout>
  );
}
