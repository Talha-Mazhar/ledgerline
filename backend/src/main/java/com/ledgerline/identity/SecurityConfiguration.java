package com.ledgerline.identity;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * How requests are secured.
 *
 * <ul>
 *   <li><b>Sessions.</b> Signing in puts the user in a server-side session; the browser holds only
 *       the session cookie ({@code HttpOnly}, so page scripts can't read it). No tokens in local
 *       storage, where any injected script could steal them.
 *   <li><b>CSRF.</b> Because the browser sends the session cookie automatically, another site could
 *       make it send a request on the user's behalf. Every request that changes something must
 *       therefore also carry the CSRF token in an {@code X-XSRF-TOKEN} header. The token arrives in
 *       an {@code XSRF-TOKEN} cookie that Ledgerline's own pages can read and other sites can't.
 *   <li><b>Errors.</b> 401 (not signed in) and 403 (refused, e.g. a missing CSRF token) are
 *       answered as problem details, like every other error of the API.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    private static final String UNAUTHORIZED = """
            {"type":"about:blank","title":"Sign-in required","status":401,"detail":"Sign in to continue."}""";
    private static final String FORBIDDEN = """
            {"type":"about:blank","title":"Request refused","status":403,\
            "detail":"The request was refused. Reload the page and try again."}""";

    @Bean
    SecurityFilterChain api(
            HttpSecurity http,
            SecurityContextRepository contexts,
            CsrfTokenRepository csrfTokens,
            CsrfTokenRequestAttributeHandler csrfHandler)
            throws Exception {
        http.authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/sign-in", "/api/v1/auth/sign-up")
                        .permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .securityContext(context -> context.securityContextRepository(contexts))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens).csrfTokenRequestHandler(csrfHandler))
                .logout(logout -> logout.logoutUrl("/api/v1/auth/sign-out")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(
                                (request, response, exception) -> problem(response, 401, UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) -> problem(response, 403, FORBIDDEN)))
                .requestCache(cache -> cache.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());
        return http.build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        // HttpOnly is off on purpose: the frontend must read this cookie to copy it into the header.
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    /**
     * Loads the token on every request, so the XSRF-TOKEN cookie is always set, also right after
     * sign-in replaced it. The header must carry the token exactly as the cookie has it.
     */
    @Bean
    CsrfTokenRequestAttributeHandler csrfTokenRequestHandler() {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName(null);
        return handler;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Sign-in goes through {@code AuthController}, not an {@code AuthenticationManager}. Declaring
     * this bean stops Spring Boot from creating its default in-memory user with a generated
     * password.
     */
    @Bean
    AuthenticationManager noAuthenticationManager() {
        return authentication -> {
            throw new ProviderNotFoundException("Sign in through POST /api/v1/auth/sign-in");
        };
    }

    private static void problem(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body);
    }
}
