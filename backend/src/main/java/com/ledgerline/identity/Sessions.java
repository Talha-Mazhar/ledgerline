package com.ledgerline.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.stereotype.Component;

/**
 * Starts a session for a user who just signed in or signed up. This is what Spring Security's
 * form login does internally; Ledgerline's sign-in is a JSON endpoint, so it does the same steps
 * itself:
 *
 * <ol>
 *   <li>Give the session a new id, so an id someone planted before sign-in is worthless (session
 *       fixation).
 *   <li>Replace the CSRF token, for the same reason.
 *   <li>Store the signed-in user in the session; later requests find it there.
 * </ol>
 */
@Component
class Sessions {

    private final SecurityContextRepository contexts;
    private final ChangeSessionIdAuthenticationStrategy newSessionId = new ChangeSessionIdAuthenticationStrategy();
    private final CsrfAuthenticationStrategy newCsrfToken;

    Sessions(
            SecurityContextRepository contexts,
            CsrfTokenRepository csrfTokens,
            CsrfTokenRequestAttributeHandler csrfHandler) {
        this.contexts = contexts;
        this.newCsrfToken = new CsrfAuthenticationStrategy(csrfTokens);
        // The same handler as the filter chain: it writes the new token's cookie in this response.
        this.newCsrfToken.setRequestHandler(csrfHandler);
    }

    void start(SignedInUser user, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationFor(user);
        request.getSession(true);
        newSessionId.onAuthentication(authentication, request, response);
        newCsrfToken.onAuthentication(authentication, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
    }

    /** The authentication a signed-in user carries: the user and the authority of their role. */
    static Authentication authenticationFor(SignedInUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(
                user, null, List.of(new SimpleGrantedAuthority(user.role().authority())));
    }
}
