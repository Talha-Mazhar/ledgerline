import NotificationsNoneRounded from '@mui/icons-material/NotificationsNoneRounded';
import Badge from '@mui/material/Badge';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import Popover from '@mui/material/Popover';
import Typography from '@mui/material/Typography';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import type { Notification } from '@/shared/api/generated';
import {
  listNotificationsOptions,
  listNotificationsQueryKey,
  markAllNotificationsReadMutation,
  markNotificationReadMutation,
} from '@/shared/api/generated/@tanstack/react-query.gen';
import { formatDateTime } from '@/shared/money/format';

/** How often the bell asks for new notifications while the app is open. */
const POLL_MS = 30_000;

/**
 * The bell in the sidebar: the number of unread notifications, and a panel with the latest ones.
 * Opening one marks it read and goes to the page it is about. Notifications are written by the
 * backend's Kafka consumers, so they arrive a moment after the payment that caused them; the
 * bell checks again every 30 seconds.
 */
export function NotificationsBell() {
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const notifications = useQuery({
    ...listNotificationsOptions({ query: { limit: 20 } }),
    refetchInterval: POLL_MS,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: listNotificationsQueryKey() });
  const markRead = useMutation({ ...markNotificationReadMutation(), onSuccess: refresh });
  const markAllRead = useMutation({ ...markAllNotificationsReadMutation(), onSuccess: refresh });

  const unread = notifications.data?.unread ?? 0;
  const items = notifications.data?.items ?? [];

  function open(notification: Notification) {
    if (!notification.read) {
      markRead.mutate({ path: { notificationId: notification.id } });
    }
    setAnchor(null);
    if (typeof notification.link === 'string') {
      void navigate(notification.link);
    }
  }

  return (
    <>
      <IconButton
        aria-label={unread === 0 ? 'Notifications' : `Notifications, ${unread} unread`}
        onClick={(event) => setAnchor(event.currentTarget)}
        sx={{ color: 'inherit' }}
      >
        <Badge badgeContent={unread} color="error" max={9}>
          <NotificationsNoneRounded fontSize="small" />
        </Badge>
      </IconButton>
      <Popover
        open={anchor !== null}
        anchorEl={anchor}
        onClose={() => setAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        slotProps={{
          paper: {
            role: 'dialog',
            'aria-label': 'Notifications',
            sx: { width: 360, maxWidth: 'calc(100vw - 32px)' },
          },
        }}
      >
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            px: 2,
            py: 1.5,
            borderBottom: 1,
            borderColor: 'ledger.rule',
          }}
        >
          <Typography variant="h3" component="h2">
            Notifications
          </Typography>
          <Button
            size="small"
            disabled={unread === 0 || markAllRead.isPending}
            onClick={() => markAllRead.mutate({})}
          >
            Mark all read
          </Button>
        </Box>
        {items.length === 0 ? (
          <Typography sx={{ px: 2, py: 3, color: 'text.secondary' }}>
            {notifications.isError
              ? "Notifications couldn't be loaded."
              : 'Nothing yet. Payments, approvals and statements show up here.'}
          </Typography>
        ) : (
          <List disablePadding sx={{ maxHeight: 420, overflowY: 'auto' }}>
            {items.map((notification) => (
              <ListItemButton
                key={notification.id}
                onClick={() => open(notification)}
                sx={{
                  display: 'block',
                  px: 2,
                  py: 1.5,
                  borderBottom: 1,
                  borderColor: 'ledger.rule',
                  bgcolor: notification.read ? 'transparent' : 'rgba(36, 70, 216, 0.05)',
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  {!notification.read && (
                    <Box
                      component="span"
                      aria-label="Unread"
                      sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: 'primary.main' }}
                    />
                  )}
                  <Typography sx={{ fontWeight: notification.read ? 500 : 650 }}>
                    {notification.title}
                  </Typography>
                </Box>
                <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.25 }}>
                  {notification.body}
                </Typography>
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                  {formatDateTime(notification.createdAt)}
                </Typography>
              </ListItemButton>
            ))}
          </List>
        )}
      </Popover>
    </>
  );
}
