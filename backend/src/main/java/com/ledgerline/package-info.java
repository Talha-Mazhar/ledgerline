/**
 * Ledgerline API root package.
 *
 * <p>{@code @NullMarked} makes every type in this package non-null unless it is explicitly
 * annotated {@code @Nullable}. NullAway checks this at compile time. JSpecify annotations apply to
 * one package only, not its sub-packages, so every package needs its own {@code package-info.java};
 * {@code NullSafetyTests} enforces that.
 */
@NullMarked
package com.ledgerline;

import org.jspecify.annotations.NullMarked;
