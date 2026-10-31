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
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { NavLink, Outlet } from 'react-router';
import { BrandMark } from '@/app/layout/BrandMark';
import { navigationItems, type NavigationItem } from '@/app/layout/navigation';

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
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', px: 2, py: 3 }}>
      <Box sx={{ px: 1, mb: 4 }}>
        <BrandMark />
      </Box>

      <List
        component="nav"
        aria-label="Main navigation"
        disablePadding
        sx={{ display: 'grid', gap: 0.5 }}
      >
        {navigationItems.map((item) => (
          <NavigationLink key={item.path} item={item} onNavigate={onNavigate} />
        ))}
      </List>

      <Box sx={{ mt: 'auto', px: 1, pt: 2, borderTop: 1, borderColor: 'rgba(255, 255, 255, 0.1)' }}>
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
          Demo account
        </Typography>
        <Typography variant="body2" sx={{ color: 'ledger.onInkMuted' }}>
          Sign-in isn't set up yet
        </Typography>
      </Box>
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
