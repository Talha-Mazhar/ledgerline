package com.ledgerline.identity;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registering users and checking their passwords. Also the module's {@link UserDirectory}.
 *
 * <p>Passwords are hashed with bcrypt through a delegating encoder, which stores the algorithm with
 * the hash ({@code {bcrypt}$2a$10$...}). A stronger algorithm can become the default later; old
 * hashes keep working because each one says how it was made.
 */
@Service
class UserAccounts implements UserDirectory {

    /** Deliberately loose: the real check is that mail arrives, which this demo doesn't send. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    static final int MIN_PASSWORD = 10;
    static final int MAX_PASSWORD = 64;
    /** bcrypt reads only the first 72 bytes of a password; longer ones are refused, not cut. */
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserAccountRepository users;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    /** Checked when nobody has the email, so a wrong email takes as long as a wrong password. */
    private final String unknownUserHash;

    UserAccounts(
            UserAccountRepository users, PasswordEncoder passwords, Clock clock, ApplicationEventPublisher events) {
        this.users = users;
        this.events = events;
        this.passwords = passwords;
        this.clock = clock;
        this.unknownUserHash = passwords.encode("no-user-" + UUID.randomUUID());
    }

    @Transactional
    SignedInUser register(String email, String displayName, String password) {
        String normalised = UserAccount.normalise(email);
        String name = displayName.trim();
        if (normalised.length() > 254 || !EMAIL.matcher(normalised).matches()) {
            throw new InvalidSignUpException("Enter a valid email address.");
        }
        if (name.isEmpty() || name.length() > 80) {
            throw new InvalidSignUpException("Enter your name, up to 80 characters.");
        }
        if (password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD || tooLongForBcrypt(password)) {
            throw new InvalidSignUpException(
                    "Use a password of " + MIN_PASSWORD + " to " + MAX_PASSWORD + " characters.");
        }
        if (users.existsByEmail(normalised)) {
            throw new EmailTakenException(null);
        }
        try {
            UserAccount account = users.saveAndFlush(
                    UserAccount.register(normalised, name, passwords.encode(password), clock.instant()));
            events.publishEvent(
                    new UserRegistered(UUID.randomUUID(), account.id(), account.email(), account.displayName()));
            return signedIn(account);
        } catch (DataIntegrityViolationException raced) {
            // Two sign-ups with the same email at once: the unique constraint lets only one through.
            throw new EmailTakenException(raced);
        }
    }

    @Transactional(readOnly = true)
    SignedInUser authenticate(String email, String password) {
        if (tooLongForBcrypt(password)) {
            throw new InvalidCredentialsException();
        }
        @Nullable UserAccount account = users.findByEmail(UserAccount.normalise(email)).orElse(null);
        boolean matches = passwords.matches(password, account == null ? unknownUserHash : account.passwordHash());
        if (account == null || !matches) {
            throw new InvalidCredentialsException();
        }
        return signedIn(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummary> findByEmail(String email) {
        return users.findByEmail(UserAccount.normalise(email)).map(UserAccount::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummary> find(UUID id) {
        return users.findById(id).map(UserAccount::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> withRoles(Collection<Role> roles) {
        return users.findByRoleInOrderByEmail(roles).stream()
                .map(UserAccount::summary)
                .toList();
    }

    /** Users whose email or name contains the text; all users, alphabetically, for empty text. */
    @Transactional(readOnly = true)
    List<UserSummary> search(String text, int limit) {
        return users.search(text.trim(), PageRequest.of(0, limit)).stream()
                .map(UserAccount::summary)
                .toList();
    }

    /**
     * Changes a user's role. Only admins call this (the controller checks). The last admin can't
     * be demoted, so the back office can never lock itself out.
     */
    @Transactional
    UserSummary changeRole(UUID actorId, UUID userId, Role role) {
        UserAccount user = users.findById(userId).orElseThrow(UnknownUserException::new);
        Role before = user.role();
        if (before == role) {
            return user.summary();
        }
        if (before == Role.ADMIN && users.countByRole(Role.ADMIN) == 1) {
            throw new RoleChangeRefusedException("Ledgerline needs at least one admin.");
        }
        user.changeRole(role);
        events.publishEvent(new RoleChanged(actorId, userId, before, role));
        return user.summary();
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummary require(UUID id) {
        return users.findById(id)
                .map(UserAccount::summary)
                .orElseThrow(() -> new IllegalStateException("No user " + id));
    }

    private static boolean tooLongForBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }

    private static SignedInUser signedIn(UserAccount account) {
        return new SignedInUser(account.id(), account.email(), account.displayName(), account.role());
    }
}
