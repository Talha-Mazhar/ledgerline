import { Navigate, Outlet } from 'react-router';
import { isStaff } from '@/app/layout/navigation';
import { useSignedInUser } from '@/features/auth/signedInUser';
import { paths } from '@/shared/config/paths';

/**
 * Keeps each kind of user on their own pages: staff in the back office, customers with their
 * money. The backend refuses the other side's API calls anyway (403); this only saves a user from
 * a page full of errors.
 */
export function RequireStaff() {
  const user = useSignedInUser();
  return isStaff(user.role) ? <Outlet /> : <Navigate to={paths.overview} replace />;
}

export function RequireCustomer() {
  const user = useSignedInUser();
  return isStaff(user.role) ? <Navigate to={paths.approvals} replace /> : <Outlet />;
}
