package com.ledgerline.notifications;

import com.ledgerline.identity.Role;
import com.ledgerline.identity.UserDirectory;
import com.ledgerline.identity.UserSummary;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** Delivers a notice: stores it in the inbox and, the first time only, emails it. */
@Component
class Notifier {

    private final Inbox inbox;
    private final EmailSender email;
    private final UserDirectory users;

    Notifier(Inbox inbox, EmailSender email, UserDirectory users) {
        this.inbox = inbox;
        this.email = email;
        this.users = users;
    }

    void tell(UUID userId, UUID eventId, String kind, String title, String body, @Nullable String link, boolean byEmail) {
        boolean isNew = inbox.add(new Notice(userId, eventId, kind, title, body, link));
        if (isNew && byEmail) {
            users.find(userId).ifPresent(user -> email.send(user.email(), title, body));
        }
    }

    void tellStaff(UUID eventId, String kind, String title, String body, String link) {
        for (UserSummary staff : users.withRoles(List.of(Role.SUPPORT, Role.ADMIN))) {
            tell(staff.id(), eventId, kind, title, body, link, false);
        }
    }

    String nameOf(@Nullable UUID user) {
        if (user == null) {
            return "someone";
        }
        return users.find(user).map(UserSummary::displayName).orElse("a former user");
    }
}
