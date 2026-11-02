package com.ledgerline.identity;

/**
 * What a user may do. Customers use their own wallets; support staff review verifications and
 * held payments in the back office; admins can also change other users' roles.
 */
public enum Role {
    CUSTOMER,
    SUPPORT,
    ADMIN;

    /** The Spring Security authority for this role, e.g. {@code ROLE_SUPPORT}. */
    public String authority() {
        return "ROLE_" + name();
    }

    public boolean isStaff() {
        return this != CUSTOMER;
    }
}
