package com.ledgerline.notifications;

import com.ledgerline.identity.CurrentUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's notifications. */
@RestController
@RequestMapping("/api/v1/notifications")
@Validated
class NotificationController {

    record NotificationListResponse(int unread, List<Inbox.Item> items) {}

    private final Inbox inbox;
    private final CurrentUser currentUser;

    NotificationController(Inbox inbox, CurrentUser currentUser) {
        this.inbox = inbox;
        this.currentUser = currentUser;
    }

    @GetMapping
    NotificationListResponse listNotifications(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        UUID user = currentUser.id();
        return new NotificationListResponse(inbox.unread(user), inbox.latest(user, limit));
    }

    @PostMapping("/{notificationId}/read")
    ResponseEntity<Void> markNotificationRead(@PathVariable UUID notificationId) {
        inbox.markRead(currentUser.id(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    ResponseEntity<Void> markAllNotificationsRead() {
        inbox.markAllRead(currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
