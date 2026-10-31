package com.ledgerline;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.ledgerline.identity.SignedInUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** The two users the V5 migration creates, both with the password {@code ledgerline-demo}. */
public final class TestUsers {

    public static final String PASSWORD = "ledgerline-demo";

    public static final SignedInUser DEMO = new SignedInUser(
            UUID.fromString("00000000-0000-4000-8000-000000000001"), "demo@ledgerline.dev", "Demo user");

    public static final SignedInUser JANA = new SignedInUser(
            UUID.fromString("00000000-0000-4000-8000-000000000002"), "jana@ledgerline.dev", "Jana Nováková");

    /** Sends one request as this user, whoever the test is otherwise signed in as. */
    public static RequestPostProcessor signedInAs(SignedInUser user) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }

    private TestUsers() {}
}
