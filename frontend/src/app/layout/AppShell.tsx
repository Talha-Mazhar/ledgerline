import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { NavLink, Outlet } from 'react-router';
import { navigationItems, type NavigationItem } from '@/app/layout/navigation';

const DRAWER_WIDTH = 240;

/** The frame around every signed-in page: top bar, side navigation and the page itself. */
export function AppShell() {
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
        <Toolbar>
          <Typography variant="h3" component="span" sx={{ color: 'primary.main' }}>
            Ledgerline
          </Typography>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        <Toolbar />
        <List component="nav" aria-label="Main navigation" sx={{ px: 1 }}>
          {navigationItems.map((item) => (
            <NavigationLink key={item.path} item={item} />
          ))}
        </List>
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, p: { xs: 2, md: 4 } }}>
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}

interface NavigationLinkProps {
  readonly item: NavigationItem;
}

function NavigationLink({ item }: NavigationLinkProps) {
  const Icon = item.icon;
  return (
    <ListItemButton
      component={NavLink}
      to={item.path}
      end
      sx={{
        borderRadius: 1,
        '&.active': { bgcolor: 'action.selected', color: 'primary.main' },
        '&.active .MuiListItemIcon-root': { color: 'primary.main' },
      }}
    >
      <ListItemIcon sx={{ minWidth: 36 }}>
        <Icon fontSize="small" />
      </ListItemIcon>
      <ListItemText primary={item.label} />
    </ListItemButton>
  );
}
