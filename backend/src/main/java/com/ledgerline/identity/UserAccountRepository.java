package com.ledgerline.identity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    /** {@code email} must already be normalised (lower case, trimmed). */
    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    List<UserAccount> findByRoleInOrderByEmail(Collection<Role> roles);

    long countByRole(Role role);

    /** Users whose email or name contains the text, ignoring case. */
    @Query("""
            SELECT u FROM UserAccount u
            WHERE lower(u.email) LIKE lower(concat('%', :text, '%'))
               OR lower(u.displayName) LIKE lower(concat('%', :text, '%'))
            ORDER BY u.email
            """)
    List<UserAccount> search(@Param("text") String text, Pageable page);
}
