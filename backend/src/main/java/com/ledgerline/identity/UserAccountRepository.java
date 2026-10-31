package com.ledgerline.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    /** {@code email} must already be normalised (lower case, trimmed). */
    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}
