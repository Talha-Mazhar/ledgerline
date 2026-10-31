package com.ledgerline;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Request builders for API tests. A POST carries a valid CSRF token in the {@code X-XSRF-TOKEN}
 * header, as the frontend sends it; tests of the CSRF protection itself use Spring's plain builder.
 */
public final class ApiRequests {

    public static MockHttpServletRequestBuilder post(String urlTemplate, Object... uriVariables) {
        return MockMvcRequestBuilders.post(urlTemplate, uriVariables).with(csrf().asHeader());
    }

    private ApiRequests() {}
}
