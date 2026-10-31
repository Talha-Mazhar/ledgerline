package com.ledgerline;

import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Request builders for API tests. A POST carries a CSRF token exactly as the frontend sends it: the
 * {@code XSRF-TOKEN} cookie and the same value in the {@code X-XSRF-TOKEN} header. Tests of the
 * CSRF protection itself use Spring's plain builder.
 *
 * <p>Why not Spring Security's {@code csrf()} test helper: it replaces the CSRF filter's token
 * store with a session-based one for the rest of the test run. Later tests would then no longer
 * exercise the cookie-based setup the app really uses.
 */
public final class ApiRequests {

    private static final String TOKEN = "test-csrf-token";

    public static MockHttpServletRequestBuilder post(String urlTemplate, Object... uriVariables) {
        return MockMvcRequestBuilders.post(urlTemplate, uriVariables)
                .cookie(new Cookie("XSRF-TOKEN", TOKEN))
                .header("X-XSRF-TOKEN", TOKEN);
    }

    private ApiRequests() {}
}
