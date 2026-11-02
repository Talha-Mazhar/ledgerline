import LogoutRounded from '@mui/icons-material/LogoutRounded';
import MenuRounded from '@mui/icons-material/MenuRounded';
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import IconButton from '@mui/material/IconButton';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Toolbar from '@mui/material/Toolbar';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router';
import { BrandMark } from '@/app/layout/BrandMark';
import { navigationFor, type NavigationItem } from '@/app/layout/navigation';
import { useSignedInUser } from '@/features/auth/signedInUser';
import { NotificationsBell } from '@/features/notifications/NotificationsBell';
import { signOutMutation } from '@/shared/api/generated/@tanstack/react-query.gen';
import { paths } from '@/shared/config/paths';

const SIDEBAR_WIDTH = 248;

/**
 * The frame around every page: an ink sidebar with the navigation on wide screens; on narrow ones
 * a top bar whose menu button slides the same sidebar in.
 */
export function AppShell() {
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        elevation={0}
        sx={{ display: { md: 'none' }, bgcolor: 'ledger.ink', color: 'ledger.onInk' }}
      >
        <Toolbar sx={{ gap: 1 }}>
          <IconButton
            edge="start"
            color="inherit"
            aria-label="Open navigation"
            onClick={() => setMenuOpen(true)}
          >
            <MenuRounded />
          </IconButton>
          <BrandMark />
          <Box sx={{ ml: 'auto' }}>
            <NotificationsBell />
          </Box>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="temporary"
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
        sx={{ display: { md: 'none' }, '& .MuiDrawer-paper': { width: SIDEBAR_WIDTH } }}
        slotProps={{ paper: { sx: { bgcolor: 'ledger.ink', color: 'ledger.onInk' } } }}
      >
        <Sidebar onNavigate={() => setMenuOpen(false)} />
      </Drawer>

      <Box
        component="aside"
        sx={{
          display: { xs: 'none', md: 'block' },
          width: SIDEBAR_WIDTH,
          flexShrink: 0,
          position: 'sticky',
          top: 0,
          height: '100vh',
          bgcolor: 'ledger.ink',
          color: 'ledger.onInk',
        }}
      >
        <Sidebar />
      </Box>

      <Box component="main" sx={{ flexGrow: 1, minWidth: 0 }}>
        <Toolbar sx={{ display: { md: 'none' } }} />
        <Box sx={{ maxWidth: 1200, mx: 'auto', px: { xs: 2, sm: 3, md: 6 }, py: { xs: 3, md: 6 } }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}

interface SidebarProps {
  readonly onNavigate?: () => void;
}

function Sidebar({ onNavigate }: SidebarProps) {
  const user = useSignedInUser();
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', px: 2, py: 3 }}>
      <Box
        sx={{
          pl: 1,
          mb: 4,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          color: 'ledger.onInkMuted',
        }}
      >
        <Box sx={{ color: 'ledger.onInk' }}>
          <BrandMark />
        </Box>
        {/* On narrow screens the bell sits in the top bar instead. */}
        {onNavigate === undefined && <NotificationsBell />}
      </Box>

      <Box component="nav" aria-label="Main navigation" sx={{ display: 'grid', gap: 2.5 }}>
        {navigationFor(user.role).map((section) => (
          <div key={section.title ?? 'main'}>
            {section.title !== undefined && (
              <Typography
                variant="overline"
                component="h2"
                sx={{ px: 1.5, color: 'ledger.onInkMuted', letterSpacing: '0.08em' }}
              >
                {section.title}
              </Typography>
            )}
            <List disablePadding sx={{ display: 'grid', gap: 0.5 }}>
              {section.items.map((item) => (
                <NavigationLink key={item.path} item={item} onNavigate={onNavigate} />
              ))}
            </List>
          </div>
        ))}
      </Box>

      <AccountFooter />
    </Box>
  );
}

/** Who is signed in, and the way out. */
function AccountFooter() {
  const user = useSignedInUser();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const signOut = useMutation({
    ...signOutMutation(),
    onSettled: () => {
      // Whatever the server said, this browser leaves: nothing of this user stays in the cache.
      void navigate(paths.signIn, { replace: true });
      queryClient.removeQueries();
    },
  });

  return (
    <Box
      sx={{
        mt: 'auto',
        px: 1,
        pt: 2,
        borderTop: 1,
        borderColor: 'rgba(255, 255, 255, 0.1)',
        display: 'flex',
        alignItems: 'center',
        gap: 1,
      }}
    >
      <Box sx={{ minWidth: 0, flexGrow: 1 }}>
        <Typography variant="body2" noWrap sx={{ fontWeight: 600 }}>
          {user.displayName}
        </Typography>
        <Typography variant="body2" noWrap sx={{ color: 'ledger.onInkMuted' }}>
          {user.email}
        </Typography>
      </Box>
      <Tooltip title="Sign out">
        <IconButton
          aria-label="Sign out"
          onClick={() => signOut.mutate({})}
          disabled={signOut.isPending}
          sx={{ color: 'ledger.onInkMuted', '&:hover': { color: 'ledger.onInk' } }}
        >
          <LogoutRounded fontSize="small" />
        </IconButton>
      </Tooltip>
    </Box>
  );
}

interface NavigationLinkProps {
  readonly item: NavigationItem;
  readonly onNavigate: (() => void) | undefined;
}

function NavigationLink({ item, onNavigate }: NavigationLinkProps) {
  const Icon = item.icon;
  return (
    <ListItemButton
      component={NavLink}
      to={item.path}
      end
      onClick={onNavigate}
      sx={{
        borderRadius: 1.5,
        color: 'ledger.onInkMuted',
        '&:hover': { bgcolor: 'rgba(255, 255, 255, 0.06)', color: 'ledger.onInk' },
        '&.active': { bgcolor: 'rgba(142, 162, 255, 0.14)', color: 'ledger.onInk' },
        '&.active .MuiListItemIcon-root': { color: '#8ea2ff' },
      }}
    >
      <ListItemIcon sx={{ minWidth: 34, color: 'inherit' }}>
        <Icon fontSize="small" />
      </ListItemIcon>
      <ListItemText primary={item.label} slotProps={{ primary: { sx: { fontWeight: 500 } } }} />
    </ListItemButton>
  );
}
