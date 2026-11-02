/**
 * Currency exchange module: the rates Ledgerline offers for changing one currency into another.
 *
 * <p>Rates are the European Central Bank's reference rates, fetched from the Frankfurter API every
 * hour (on one instance, ShedLock) through a retry and a circuit breaker (Resilience4j), and read
 * through a Redis cache. If the feed is down, the last rates stay in use.
 *
 * <p>Only knows rates. Moving the money is the payments module's job; it asks this module for a
 * rate. May depend on the {@code ledger} module (currencies and money).
 */
@NullMarked
@ApplicationModule(displayName = "FX", allowedDependencies = "ledger")
package com.ledgerline.fx;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
