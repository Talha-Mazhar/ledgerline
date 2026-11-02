package com.ledgerline.identity;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * The session cookie, now that sessions live in Redis (Spring Session) rather than in the servlet
 * container: Spring Session writes its own cookie and doesn't read {@code server.servlet.session.*}.
 *
 * <ul>
 *   <li>{@code HttpOnly}: scripts on the page, including an injected one, can't read it.
 *   <li>{@code SameSite=Lax}: other sites can't send it with their POSTs; CSRF tokens are the second
 *       line of defence.
 *   <li>{@code Secure} when {@code ledgerline.session.secure-cookie} is true (behind HTTPS), and
 *       otherwise whenever the request came in over HTTPS.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
class SessionCookieConfiguration {

    static final String COOKIE_NAME = "LEDGERLINE_SESSION";

    @Bean
    CookieSerializer sessionCookieSerializer(
            @Value("${ledgerline.session.secure-cookie:#{null}}") @Nullable Boolean secure) {
        DefaultCookieSerializer cookies = new DefaultCookieSerializer();
        cookies.setCookieName(COOKIE_NAME);
        cookies.setCookiePath("/");
        cookies.setUseHttpOnlyCookie(true);
        cookies.setSameSite("Lax");
        if (secure != null) {
            cookies.setUseSecureCookie(secure);
        }
        return cookies;
    }
}
