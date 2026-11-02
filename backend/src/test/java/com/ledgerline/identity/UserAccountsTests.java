package com.ledgerline.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledgerline.TestUsers;
import com.ledgerline.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/** Roles and user search against the real database, in rolled-back transactions. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RecordApplicationEvents
class UserAccountsTests {

    private final UserAccounts accounts;

    @Autowired
    @SuppressWarnings("NullAway.Init")
    ApplicationEvents events;

    UserAccountsTests(@Autowired UserAccounts accounts) {
        this.accounts = accounts;
    }

    @Test
    void findsUsersByPartOfTheirNameOrEmail() {
        assertThat(accounts.search("NOVÁK", 50)).extracting(UserSummary::email).containsExactly("jana@ledgerline.dev");
        assertThat(accounts.search("ledgerline.dev", 50)).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void knowsTheStaff() {
        assertThat(accounts.withRoles(List.of(Role.SUPPORT, Role.ADMIN)))
                .extracting(UserSummary::email)
                .containsExactly("admin@ledgerline.dev", "support@ledgerline.dev");
    }

    @Test
    void anAdminPromotesAUser() {
        UserSummary jana = accounts.changeRole(TestUsers.ADMIN.id(), TestUsers.JANA.id(), Role.SUPPORT);

        assertThat(jana.role()).isEqualTo(Role.SUPPORT);
        assertThat(accounts.require(TestUsers.JANA.id()).role()).isEqualTo(Role.SUPPORT);
        assertThat(events.stream(RoleChanged.class)).singleElement().satisfies(changed -> {
            assertThat(changed.actorId()).isEqualTo(TestUsers.ADMIN.id());
            assertThat(changed.from()).isEqualTo(Role.CUSTOMER);
        });
    }

    @Test
    void theLastAdminCantBeDemoted() {
        assertThatThrownBy(() -> accounts.changeRole(TestUsers.ADMIN.id(), TestUsers.ADMIN.id(), Role.CUSTOMER))
                .isInstanceOf(RoleChangeRefusedException.class)
                .hasMessage("Ledgerline needs at least one admin.");
    }

    @Test
    void signingUpPublishesAnEvent() {
        accounts.register("new.user@example.com", "New User", "a-long-password");

        assertThat(events.stream(UserRegistered.class))
                .singleElement()
                .extracting(UserRegistered::email)
                .isEqualTo("new.user@example.com");
    }
}
