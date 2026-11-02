package com.ledgerline.identity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The identity module's public API for finding users, e.g. the recipient of a transfer. */
public interface UserDirectory {

    /** The user with this email, ignoring case. */
    Optional<UserSummary> findByEmail(String email);

    /** The user with this id. */
    UserSummary require(UUID id);

    /** The user with this id, if there is one. */
    Optional<UserSummary> find(UUID id);

    /** Everyone with one of these roles, e.g. all staff to notify about a held payment. */
    List<UserSummary> withRoles(Collection<Role> roles);
}
