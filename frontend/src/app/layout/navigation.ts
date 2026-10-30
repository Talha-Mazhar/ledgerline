import type { SvgIconComponent } from '@mui/icons-material';
import SpaceDashboardOutlined from '@mui/icons-material/SpaceDashboardOutlined';
import { paths, type AppPath } from '@/shared/config/paths';

export interface NavigationItem {
  readonly label: string;
  readonly path: AppPath;
  readonly icon: SvgIconComponent;
}

/** The side navigation. Each feature adds its entry here when its page exists. */
export const navigationItems: readonly NavigationItem[] = [
  { label: 'Overview', path: paths.overview, icon: SpaceDashboardOutlined },
];
