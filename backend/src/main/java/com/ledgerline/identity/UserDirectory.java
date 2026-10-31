package com.ledgerline.identity;

import java.util.Optional;
import java.util.UUID;

/** The identity module's public API for finding users, e.g. the recipient of a transfer. */
public interface UserDirectory {

    /** The user with this email, ignoring case. */
    Optional<UserSummary> findByEmail(String email);

    /** The user with this id. */
    UserSummary require(UUID id);
}
