package com.ledgerline.identity;

import java.util.UUID;
import org.springframework.stereotype.Component;

/** Stand-in for sign-in: every request acts for the same demo user. */
@Component
class DemoCurrentUser implements CurrentUser {

    static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Override
    public UUID id() {
        return DEMO_USER_ID;
    }
}
