package com.ledgerline;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.ledgerline.identity.Role;
import com.ledgerline.identity.SignedInUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** The users the migrations create, all with the password {@code ledgerline-demo}. */
public final class TestUsers {

    public static final String PASSWORD = "ledgerline-demo";

    public static final SignedInUser DEMO = new SignedInUser(
            UUID.fromString("00000000-0000-4000-8000-000000000001"), "demo@ledgerline.dev", "Demo user", Role.CUSTOMER);

    public static final SignedInUser JANA = new SignedInUser(
            UUID.fromString("00000000-0000-4000-8000-000000000002"),
            "jana@ledgerline.dev",
            "Jana Nováková",
            Role.CUSTOMER);

    public static final SignedInUser SUPPORT = new SignedInUser(
            UUID.fromString("00000000-0000-4000-8000-000000000003"),
            "support@ledgerline.dev",
            "Sam Support",
            Role.SUPPORT);

    public static final SignedInUser ADMIN = new SignedInUser(
            UUID.fromString("00000000-0000-4000-8000-000000000004"), "admin@ledgerline.dev", "Alex Admin", Role.ADMIN);

    /** The authentication a session holds for this user, with the authority of their role. */
    public static Authentication authenticationOf(SignedInUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(
                user, null, List.of(new SimpleGrantedAuthority(user.role().authority())));
    }

    /** Sends one request as this user, whoever the test is otherwise signed in as. */
    public static RequestPostProcessor signedInAs(SignedInUser user) {
        return authentication(authenticationOf(user));
    }

    private TestUsers() {}
}
