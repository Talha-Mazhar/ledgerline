package com.ledgerline;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * Runs a test signed in as {@link TestUsers#DEMO}, the way a real session would be after sign-in,
 * without going through the password check every time.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@WithSecurityContext(factory = WithDemoUser.Factory.class)
public @interface WithDemoUser {

    final class Factory implements WithSecurityContextFactory<WithDemoUser> {

        @Override
        public SecurityContext createSecurityContext(WithDemoUser annotation) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(TestUsers.DEMO, null, List.of()));
            return context;
        }
    }
}
