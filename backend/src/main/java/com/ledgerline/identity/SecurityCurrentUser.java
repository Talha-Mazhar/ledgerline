package com.ledgerline.identity;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * {@link CurrentUser} from Spring Security: the {@link SignedInUser} stored at sign-in. Every
 * {@code /api} endpoint except sign-in and sign-up requires a session, so a controller always has
 * one; if not, that's a bug and fails loudly.
 */
@Component
class SecurityCurrentUser implements CurrentUser {

    @Override
    public UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SignedInUser user) {
            return user.id();
        }
        throw new IllegalStateException("No signed-in user for this request");
    }
}
