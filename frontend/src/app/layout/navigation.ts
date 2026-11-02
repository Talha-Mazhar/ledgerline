import type { SvgIconComponent } from '@mui/icons-material';
import AutoAwesomeOutlined from '@mui/icons-material/AutoAwesomeOutlined';
import BadgeOutlined from '@mui/icons-material/BadgeOutlined';
import FactCheckOutlined from '@mui/icons-material/FactCheckOutlined';
import GroupOutlined from '@mui/icons-material/GroupOutlined';
import HistoryEduOutlined from '@mui/icons-material/HistoryEduOutlined';
import ReceiptLongOutlined from '@mui/icons-material/ReceiptLongOutlined';
import SpaceDashboardOutlined from '@mui/icons-material/SpaceDashboardOutlined';
import VerifiedUserOutlined from '@mui/icons-material/VerifiedUserOutlined';
import type { Role } from '@/shared/api/generated';
import { paths, type AppPath } from '@/shared/config/paths';

export interface NavigationItem {
  readonly label: string;
  readonly path: AppPath;
  readonly icon: SvgIconComponent;
}

export interface NavigationSection {
  /** Shown above the section's links; the customer's own section has none. */
  readonly title?: string;
  readonly items: readonly NavigationItem[];
}

const customerSection: NavigationSection = {
  items: [
    { label: 'Overview', path: paths.overview, icon: SpaceDashboardOutlined },
    { label: 'Payments', path: paths.payments, icon: ReceiptLongOutlined },
    { label: 'Assistant', path: paths.assistant, icon: AutoAwesomeOutlined },
    { label: 'Identity & limits', path: paths.verification, icon: BadgeOutlined },
  ],
};

const backOfficeSection: NavigationSection = {
  title: 'Back office',
  items: [
    { label: 'Approvals', path: paths.approvals, icon: FactCheckOutlined },
    { label: 'Verifications', path: paths.reviews, icon: VerifiedUserOutlined },
    { label: 'Users', path: paths.users, icon: GroupOutlined },
    { label: 'Audit log', path: paths.audit, icon: HistoryEduOutlined },
  ],
};

/**
 * The side navigation for a role. Customers see their money; support and admin staff see the back
 * office. The backend enforces the same split, so hiding links here is a convenience, not security.
 */
export function navigationFor(role: Role): readonly NavigationSection[] {
  return isStaff(role) ? [backOfficeSection] : [customerSection];
}

export function isStaff(role: Role): boolean {
  return role === 'SUPPORT' || role === 'ADMIN';
}
