package com.ledgerline.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign-up, sign-in and "who am I". Sign-out is handled by Spring Security's logout filter at
 * {@code POST /api/v1/auth/sign-out} (see {@link SecurityConfiguration}).
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    record SignUpRequest(String email, String displayName, String password) {
        SignUpRequest {
            Objects.requireNonNull(email, "email is required");
            Objects.requireNonNull(displayName, "displayName is required");
            Objects.requireNonNull(password, "password is required");
        }
    }

    record SignInRequest(String email, String password) {
        SignInRequest {
            Objects.requireNonNull(email, "email is required");
            Objects.requireNonNull(password, "password is required");
        }
    }

    /** The contract's {@code User}. */
    record UserResponse(String id, String email, String displayName) {
        static UserResponse from(SignedInUser user) {
            return new UserResponse(user.id().toString(), user.email(), user.displayName());
        }
    }

    private final UserAccounts accounts;
    private final Sessions sessions;

    AuthController(UserAccounts accounts, Sessions sessions) {
        this.accounts = accounts;
        this.sessions = sessions;
    }

    @PostMapping("/sign-up")
    ResponseEntity<UserResponse> signUp(
            @RequestBody SignUpRequest body, HttpServletRequest request, HttpServletResponse response) {
        SignedInUser user = accounts.register(body.email(), body.displayName(), body.password());
        sessions.start(user, request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/sign-in")
    UserResponse signIn(@RequestBody SignInRequest body, HttpServletRequest request, HttpServletResponse response) {
        SignedInUser user = accounts.authenticate(body.email(), body.password());
        sessions.start(user, request, response);
        return UserResponse.from(user);
    }

    /** The signed-in user. Without a session, the security filters answer 401 before this runs. */
    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal SignedInUser user) {
        return UserResponse.from(user);
    }
}
